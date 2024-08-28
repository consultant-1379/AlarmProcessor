/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.processing.analyser;

import static com.ericsson.oss.services.fm.common.util.Utility.DEFAULT_NUMBER_OF_CONCURRENT_CONSUMERS;
import static com.ericsson.oss.services.fm.common.util.Utility.fetchConfiguredNumberOfQueueConcurrentConsumers;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CIRCUIT_BREAKER;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.UnProcessedAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder.ProcessingState;
import com.ericsson.oss.services.fm.common.listeners.ListenersInitializer;

/**
 * Timer Bean responsible to check the processing rate of each of the active Alarm Processing threads to understand if there are any threads taking
 * more time than usual and send UnprocessedAlarms to NorthBound in case if any thread is processing an alarm for more than the configured amount of
 * time(default:20 seconds).
 */
@Singleton
@Startup
public class AlarmProcessingAnalyser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmProcessingAnalyser.class);

    private static final String QUEUE_NAME = "fmalarmqueue";
    private static final String QUEUE_JNDI_NAME = "jms:queue/fmalarmqueue";
    private static final long THREAD_BUFFER_TIME = 70000L;
    // Default consumer count for fmalarmqueue
    private int primaryConsumerCount;

    private Timer alarmProcessingRateCheckTimer;

    @Resource
    private TimerService timerService;

    @EJB
    private ListenersInitializer listenersInitializer;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private ActiveThreadsHolder threadsHolder;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private UnProcessedAlarmHandler unProcessedAlarmHandler;

    @PostConstruct
    public void createTimer() {
        startTimer(configParametersListener.getThreadStateCheckInterval());
        final Integer jvmPropertyValueForQueue = fetchConfiguredNumberOfQueueConcurrentConsumers(QUEUE_NAME, DEFAULT_NUMBER_OF_CONCURRENT_CONSUMERS);
        if (jvmPropertyValueForQueue != null) {
            primaryConsumerCount = jvmPropertyValueForQueue;
        }
    }

    public void startTimer(final int timerDuration) {
        LOGGER.info("Creating the alarmProcessingRateCheckTimer with::{}milliseconds", alarmProcessingRateCheckTimer);
        final long timeDuration = timerDuration * 1000L; //timeDuration in milliseconds.
        alarmProcessingRateCheckTimer = timerService.createIntervalTimer(timeDuration, timeDuration, createNonPersistentTimerConfig());
    }

    /**
     * Method that iterates-over all the active Alarm Processing threads to check if any thread is processing an alarm for more than the configured
     * amount of time.
     */
    @Timeout
    public void analyzeProcessingThreads() {
        final Map<Long, BufferedData> activeThreadsAndAlarms = new HashMap<>();
        final Map<Long, BufferedData> problematicThreadsAndAlarms = new HashMap<>();
        final long thresholdTimeInMillis = configParametersListener.getThresholdHoldTime() * 1000L;
        try {
            activeThreadsAndAlarms.putAll(threadsHolder.getActiveThreadsAndAlarms());
            LOGGER.debug("Took a local copy of all {} threads from master buffer of size {}", activeThreadsAndAlarms.size(), threadsHolder.size());
            for (final Entry<Long, BufferedData> entry : activeThreadsAndAlarms.entrySet()) {
                final long threadId = entry.getKey();
                final BufferedData data = entry.getValue();
                if (data != null) {
                    final long timeStamp = data.getTimeStamp();
                    final long timeDifference = System.currentTimeMillis() - timeStamp;
                    if (thresholdTimeInMillis <= timeDifference) {
                        LOGGER.warn("Thread id {} is in buffer for {}ms", threadId, timeDifference);
                        problematicThreadsAndAlarms.put(threadId, data);
                    }
                    if (timeDifference > THREAD_BUFFER_TIME && data.isSentToNorthBound()) {
                        threadsHolder.remove(threadId);
                        problematicThreadsAndAlarms.remove(threadId, data);
                        LOGGER.warn("Alarm Details After Removing from Thread Buffer:{} : {} : {} ", data.getEvents(), timeDifference, threadId);
                    }
                }
            }
            if (!problematicThreadsAndAlarms.isEmpty()) {
                checkThreshold(problematicThreadsAndAlarms.size());
                sendProblematicDataNorthBound(problematicThreadsAndAlarms);
            } else if (problematicThreadsAndAlarms.isEmpty() && !ProcessingState.RUNNING_NORMALLY.equals(threadsHolder.getProcessingState())) {
                checkThreshold(problematicThreadsAndAlarms.size());
            } else {
                LOGGER.debug("There are ZERO problematic threads out of {} active threads", activeThreadsAndAlarms.size());
            }
        } catch (final Exception exception) {
            LOGGER.error("Exception occured while analysing the active alarm processing threads: {}", exception.getMessage());
            LOGGER.debug("Exception occured while analysing the active alarm processing threads: ", exception);
            // Cancel Existing timer and recreate Timer again.
            recreateTimerWithNewInterval(configParametersListener.getThreadStateCheckInterval());
        }
    }

    @PreDestroy
    public void cancelTimer() {
        LOGGER.info("Cancelling the alarmProcessingRateCheckTimer");
        if (alarmProcessingRateCheckTimer != null) {
            alarmProcessingRateCheckTimer.cancel();
            alarmProcessingRateCheckTimer = null;
        }
    }

    /**
     * Cancels the existing timer and recreates timer with given new duration.
     * @param Integer duration
     */
    public void recreateTimerWithNewInterval(final Integer duration) {
        cancelTimer();
        startTimer(duration);
    }

    private void sendProblematicDataNorthBound(final Map<Long, BufferedData> problematicThreadData) {
        for (final Entry<Long, BufferedData> entry : problematicThreadData.entrySet()) {
            final long problematicThreadId = entry.getKey();
            final BufferedData data = entry.getValue();
            if (threadsHolder.get(problematicThreadId) != null && !data.isSentToNorthBound()) {
                final int sentToNorthBoundCount = unProcessedAlarmHandler.prepareAndSendUnprocessedAlarms(data.getEvents(), false,
                        problematicThreadId, CIRCUIT_BREAKER);
                LOGGER.warn("Sent {} unprocessed alarms as waiting in the buffer for long time with problematic threads.", sentToNorthBoundCount);
                data.setSentToNorthBound(true);
                if (threadsHolder.get(problematicThreadId) != null) {
                    threadsHolder.put(problematicThreadId, data);
                }
            }
        }
    }

    private TimerConfig createNonPersistentTimerConfig() {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        return timerConfig;
    }

    private void checkThreshold(final int blockedCount) {
        final int problematicAlarmRatio = computeRatio(blockedCount);
        final ProcessingState currentProcessingState = threadsHolder.getProcessingState();
        if (problematicAlarmRatio >= configParametersListener.getHardThresholdRatio()) {
            // breach of hard-threshold
            if (ProcessingState.HARD_THRESHOLD_CROSSED.equals(currentProcessingState)) {
                LOGGER.warn("Already HARD_THRESHOLD mode is running. Nothing to do...continuing to check in next cycle!!!");
            } else {
                LOGGER.warn("Detected HARD_THRESHOLD CROSS. Changing the mode to HARD_THRESHOLD and spawning new threads!");
                threadsHolder.setProcessingState(ProcessingState.HARD_THRESHOLD_CROSSED);
                apsInstrumentedBean.setAlarmsProcessingState(3);
                startSecondaryConsumers();
            }
        } else if (problematicAlarmRatio >= configParametersListener.getSoftThresholdRatio()) {
            // breach of soft-threshold
            if (ProcessingState.HARD_THRESHOLD_CROSSED.equals(currentProcessingState)) {
                LOGGER.warn("Detected SOFT_THRESHOLD CROSS. Falling back to SOFT_THRESHOLD mode and stopping the threads spawned!");
                stopNewThreads();
            } else {
                LOGGER.warn("Detected SOFT_THRESHOLD CROSS. Setting the mode to SOFT_THRESHOLD!");
            }
            threadsHolder.setProcessingState(ProcessingState.SOFT_THRESHOLD_CROSSED);
            apsInstrumentedBean.setAlarmsProcessingState(2);
        } else {
            // fall back to normal state
            if (ProcessingState.HARD_THRESHOLD_CROSSED.equals(currentProcessingState)) {
                LOGGER.warn("Alarm Processing is running normally. Changing the mode to RUNNING_NORMALLY and stopping the new threads spawned!");
                stopNewThreads();
            } else {
                LOGGER.debug("Alarm Processing is running normally. Nothing to do...continuing to check in next cycle!!!");
            }
            threadsHolder.setProcessingState(ProcessingState.RUNNING_NORMALLY);
            apsInstrumentedBean.setAlarmsProcessingState(1);
        }
    }

    private int computeRatio(final int blockedThreadCount) {
        int ratio = 0;
        if (primaryConsumerCount != 0) {
            ratio = blockedThreadCount * 100 / primaryConsumerCount;
        }
        return ratio;
    }

    private void stopNewThreads() {
        threadsHolder.stopConsumers();
    }

    private void startSecondaryConsumers() {
        threadsHolder.setSecondaryConnection(
                listenersInitializer.startListening(QUEUE_JNDI_NAME, false, configParametersListener.getSecondaryThreadsCount()));
        LOGGER.info("Successfully spawned secondary thread pool of size {}", configParametersListener.getSecondaryThreadsCount());
    }

}

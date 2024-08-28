/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.staging;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmHandlerBean;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class responsible for alarmStagingTimer operations for the Staging algorithm.
 */
@Singleton
public class AlarmStagingTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmStagingTimer.class);

    private final Map<Timer, String> timers = new HashMap<Timer, String>();

    @Resource
    private TimerService timerService;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private TransientAlarmStagingCacheManager transientAlarmStagingCacheManager;

    @Inject
    private ProcessedAlarmSender processedAlarmSender;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private AlarmHandlerBean alarmHandlerBean;

    public void startTimer(final int timerDuration, final String key) {
        final Timer alarmStagingTimer = timerService.createSingleActionTimer(timerDuration, createNonPersistentTimerConfig());
        timers.put(alarmStagingTimer, key);
        LOGGER.info("Started the alarmStagingTimer. Duration {}, key {}. Total active timers:{}", timerDuration, key, timers.size());
    }

    @Timeout
    public void timeOut(final Timer timer) {
        String cacheKey = "";
        ProcessedAlarmEvent stagedAlarm = new ProcessedAlarmEvent();
        try {
            cacheKey = timers.get(timer); // returns the Timer corresponding to this Timeout from the Map of Timers
            AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
            stagedAlarm = transientAlarmStagingCacheManager.unstageTransientAlarm(cacheKey);
            if (stagedAlarm != null) {
                LOGGER.debug("Timeout for the alarm {}. Sending it back for processing it again!", stagedAlarm);
                alarmProcessingResponse = alarmHandlerBean.processAlarm(stagedAlarm);
                processedAlarmSender.sendAlarms(alarmProcessingResponse, null);
                apsInstrumentedBean.incrementAlarmsUnstagedCount();
            } else {
                LOGGER.debug("There are No staged alarms left in the cache against the key:{}", cacheKey);
            }
        } catch (final Exception exception) {
            cancelTimer(timer);
            transientAlarmStagingCacheManager.stageTransientAlarm(cacheKey, stagedAlarm);
            startTimer(configParametersListener.getTransientAlarmStagingThresholdTime(), cacheKey);
            LOGGER.error("Exception {} in Timeout method of AlarmStagingTimer while processing the alarm with key:{} and value:{}", exception,
                    cacheKey, stagedAlarm);
        }
    }

    private void cancelTimer(Timer timer) {
        LOGGER.debug("Cancelling the alarmStagingTimer");
        if (timer != null) {
            try {
                timer.cancel();
                timer = null;
                timers.remove(timer);
            } catch (final NoSuchObjectLocalException exception) {
                LOGGER.warn("Exception {} occurred while trying to cancle the timer {}.", exception.getMessage(), timer);
            }
        }
    }

    private TimerConfig createNonPersistentTimerConfig() {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        return timerConfig;
    }
}

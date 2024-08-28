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

package com.ericsson.oss.services.fm.alarmprocessor.alarmsync;

import static com.ericsson.oss.services.fm.alarmprocessor.util.EventNotificationSerializer.serializeObject;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.ericsson.oss.services.fm.alarmprocessor.builders.SyncAbortNotificationBuilder;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.util.NodeRef;

/**
 * Timer Bean responsible to generate SyncAbort alarm if alarmSynchronization for a NetworkElement is ongoing for a duration greater than the
 * configured value.
 */
@Singleton
@Startup
public class AlarmSyncAbortTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmSyncAbortTimer.class);
    private Timer syncAbortTimer;

    @Resource
    private TimerService timerService;

    @Inject
    private MembershipChangeProcessor membershipChangeProcessor;

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private SyncAbortNotificationBuilder syncAbortNotificationBuilder;

    @Inject
    @Modeled
    private EventSender<EventNotificationBatch> notificationSender;

    @PostConstruct
    public void createTimer() {
        startTimer(configParametersListener.getTimerIntervalToDiscardOngoingAlarmSync());
    }

    public void startTimer(final int duration) {
        final int timerDuration = computeTimerDuration(duration);
        LOGGER.debug("Creating the ongoingSyncWatchTimer");
        syncAbortTimer = timerService.createIntervalTimer(timerDuration, timerDuration, createNonPersistentTimerConfig());
    }

    /**
     * Method that sends Sync Abort alarm for the nodes whose alarm synchronization is ongoing for a longer time than configured threshold.
     */
    @Timeout
    public void checkDurationAndGenerateSyncAbort() {
        if (membershipChangeProcessor.getMasterState()) {
            LOGGER.debug("Current APS Instance is master");
            try {
                final List<String> fmFunctionFdns = fmFunctionMoService
                        .readNodeListForLongOngoingSync(configParametersListener.getTimerIntervalToDiscardOngoingAlarmSync());
                if (fmFunctionFdns != null && !fmFunctionFdns.isEmpty()) {
                    for (final String fmFunctionFdn : fmFunctionFdns) {
                        buildAndSendSyncAbortNotifications(fmFunctionFdn);
                    }
                }
            } catch (final Exception exception) {
                LOGGER.error("Exception occured while ordering sync abort alarms on nodes with exception : {}", exception.getMessage());
                LOGGER.debug("Exception occured while ordering sync abort alarms on nodes with exception : ", exception);
                // Cancel Existing timer and recreate Timer again.
                cancelTimer();
                startTimer(configParametersListener.getTimerIntervalToDiscardOngoingAlarmSync());
            }
        } else {
            LOGGER.debug("Ignoring the Sync Time Monitor request as the current APS Instance is slave.");
        }
    }

    @PreDestroy
    public void cancelTimer() {
        LOGGER.debug("Cancelling the ongoingSyncWatchTimer");
        if (syncAbortTimer != null) {
            syncAbortTimer.cancel();
            syncAbortTimer = null;
        }
    }

    /**
     * Cancels the existing timer and recreates timer with given new duration.
     */
    public void recreateTimerWithNewInterval(final Integer duration) {
        cancelTimer();
        startTimer(duration);
    }

    private void buildAndSendSyncAbortNotifications(final String nodeFdn) {
        final List<EventNotification> notification = syncAbortNotificationBuilder.build(NodeRef.fromNodeFdn(nodeFdn));
        notificationSender.send(serializeObject(notification));
        LOGGER.info("Sync Abort Alarm has been generated to discard ongoing sync for node:  {}", nodeFdn);
    }

    private static int computeTimerDuration(final int duration) {
        int timerDuration = 0;
        if (duration > 0) {
            timerDuration = duration * 60 * 1000;
        } else {
            timerDuration = 30 * 60 * 1000;
        }
        return timerDuration;
    }

    private static TimerConfig createNonPersistentTimerConfig() {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        return timerConfig;
    }
}
/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.INITIATE_SYNC;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.SOURCE_TYPE;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.annotation.PreDestroy;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionAttribute;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.fm.capability.util.ModelCapabilities;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.SyncInitiator;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 *
 * @author tcskuya
 *
 * Timer class executes every minute to check the HB alarms count of a AXE blade cluster type nodes
 * and if no HB alarms present changes the currentServiceState to IN_SERVICE
 *
 */
@Singleton
public class AxeBladeClusterHbClearTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeBladeClusterHbClearTimer.class);

    private static final long DEFAULT_TIMER = 60000; // 1 min in milliseconds

    private Timer abortTimer;

    private final Map<String, ProcessedAlarmEvent> hbClearAlarmsMap = new ConcurrentHashMap<>();

    private boolean isTimerStarted = false;

    @Resource
    private TimerService timerService;

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private CurrentServiceStateUpdator currentServiceStateUpdator;

    @Inject
    private SyncInitiator syncInitiator;

    @Inject
    private ModelCapabilities modelCapabilities;

    @Inject
    private SystemRecorder systemRecorder;

    @PreDestroy
    public void cancelTimer() {
        LOGGER.info("Cancelling the AxeBladeClusterHbClearTimer as all HB clear alarms are processed!");
        if (abortTimer != null) {
            isTimerStarted = false;
            abortTimer.cancel();
            abortTimer = null;
        }
    }

    public void startTimer() {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        abortTimer = timerService.createIntervalTimer(DEFAULT_TIMER, DEFAULT_TIMER, timerConfig);
        LOGGER.info("AxeBladeClusterHbClearTimer is started and nextTimeOut {} ", abortTimer.getNextTimeout());
        // TimerStarted
        isTimerStarted = true;
    }

    public void changeCurrentServiceStateByTimer(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("AxeBladeClusterHbClearTimer: HB clear alarm added to list {}", alarmRecord.getObjectOfReference());
        hbClearAlarmsMap.put(alarmRecord.getFdn(), alarmRecord);
        if(!isTimerStarted){
            startTimer();
        }
    }

    @Timeout
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void monitorHeartbeatClearAlarms() {
        LOGGER.debug("In the monitorHeartbeatClearAlarms method of AxeBladeClusterHbClearTimer");
        final List<String> hbClearNodesList = new ArrayList<>();
        if (hbClearAlarmsMap.size() == 0) {
            LOGGER.debug("AxeBladeClusterHbClearTimer: hbClearAlarmsMap is empty");
            cancelTimer();
        } else {
            for (final Map.Entry<String, ProcessedAlarmEvent> entry : hbClearAlarmsMap.entrySet()) {
                final ProcessedAlarmEvent alarmRecord = entry.getValue();
                final Long matchedHbAlarmCount = alarmReader.getMatchedHbAlarms(alarmRecord);
                LOGGER.info("AxeBladeClusterHbClearTimer: Matched active HB alarms on the node {} is {}",
                        entry.getKey(), matchedHbAlarmCount);
                systemRecorder.recordEvent("APS", EventLevel.DETAILED, "FM",
                        "AxeBladeClusterHbClearTimer: Matched active HB alarms: ", String.valueOf(matchedHbAlarmCount));
                if (matchedHbAlarmCount == 0) {
                    updateHeartBeatStateToInService(alarmRecord);
                    // Add node to removed list: hbClearNodesList
                    hbClearNodesList.add(entry.getKey());
                } else {
                    LOGGER.debug("AxeBladeClusterHbClearTimer: HB clear alarm retained in list {}",
                            alarmRecord.getObjectOfReference());
                }
            }
            // Remove node from the hbClearAlarmsMap
            for (final String nodeFdn:hbClearNodesList) {
                LOGGER.debug("AxeBladeClusterHbClearTimer: Node is removed from list as all HB cleared for {}", nodeFdn);
                hbClearAlarmsMap.remove(nodeFdn);
            }
            // clear the List
            hbClearNodesList.clear();
        }
    }

    /*
     * updateHeartBeatStateToInService
     */
    private void updateHeartBeatStateToInService(final ProcessedAlarmEvent alarmRecord){
        LOGGER.debug("AxeBladeClusterHbClearTimer: changing currentServiceState for {}", alarmRecord);
        systemRecorder.recordEvent("APS", EventLevel.DETAILED, "FM",
                "AxeBladeClusterHbClearTimer: changing currentServiceState for Network Element with fdn ", alarmRecord.getFdn());
        final Map<String, Boolean> response = currentServiceStateUpdator.updateForHeartBeatAlarm(alarmRecord);
        // InitiateAlarmSync
        if (response.get(INITIATE_SYNC) != null && response.get(INITIATE_SYNC)) {
           initiateAlarmSync(alarmRecord);
        }
    }

    /*
     * initiateAlarmSync
     */
    private void initiateAlarmSync(final ProcessedAlarmEvent alarmRecord){
        boolean alarmSyncSupported = true;
        try {
            final String targetType = alarmRecord.getAdditionalInformation().get(SOURCE_TYPE);
            alarmSyncSupported = modelCapabilities.isAlarmSyncSupportedByNode(targetType);
        } catch (final Exception exception) {
            LOGGER.warn("Exception thrown while retrieving alarmSyncsupported for fdn:{} is:{}. Defaults to true",
                    alarmRecord.getFdn(), exception.getMessage());
            LOGGER.debug("Exception thrown while retrieving alarmSyncsupported for fdn:{} is:{}. Defaults to true",
                    alarmRecord.getFdn(), exception);
            alarmSyncSupported = true;
        }
        if (alarmSyncSupported) {
            LOGGER.debug("AxeBladeClusterHbClearTimer: Initiating Alarm Sync for {}", alarmRecord.getFdn());
            syncInitiator.initiateAlarmSynchronization(alarmRecord.getFdn());
        } else {
            LOGGER.debug("AxeBladeClusterHbClearTimer: AlarmSync not supported for {}", alarmRecord.getFdn());
        }
    }
}

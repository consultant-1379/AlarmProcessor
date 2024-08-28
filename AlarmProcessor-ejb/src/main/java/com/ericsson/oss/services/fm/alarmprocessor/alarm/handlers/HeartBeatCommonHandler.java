/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static com.ericsson.oss.mediation.translator.model.Constants.SKIP_CURRENT_SERVICE_STATE_UPDATE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_TO_BE_PROCESSED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.APS;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.HEARTBEAT;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.INITIATE_SYNC;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class handles alarms with record types HEARTBEAT_ALARM and HB_FAILURE_NO_SYNCH.
 */
public class HeartBeatCommonHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatCommonHandler.class);

    @Inject
    private CurrentServiceStateUpdator currentServiceStateUpdator;

    @Inject
    private AlarmProcessor newAlarmProcessor;

    @Inject
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Inject
    private AlarmValidator alarmValidator;

    @Inject
    private AlarmCorrelator alarmCorrelator;

    @Inject
    private MultiLevelHeartbeatHandler multilevelHeartbeatHandler;

    @Inject
    private SystemRecorder systemRecorder;
    
    /**
     * Method which handles alarm with record type HEARTBEAT_ALARM or HB_FAILURE_NO_SYNCH when there is a correlated alarm and when there is no
     * correlated alarm in db.
     *
     * @param {@link
     *            ProcessedAlarmEvent} --alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Alarm received to HeartBeatCommonHandler : {}", alarmRecord);
        StringBuilder srMsg = new StringBuilder();
        srMsg.append(" received for NE: ").append(alarmRecord.getFdn())
            .append(", Specific Problem: ").append(alarmRecord.getSpecificProblem()).append(", Probable Cause: ")
            .append(alarmRecord.getProbableCause()).append(", Event Type: ").append(alarmRecord.getEventType())
            .append(", Record Type: ").append(alarmRecord.getRecordType());
        systemRecorder.recordEvent(APS, EventLevel.DETAILED, HEARTBEAT, alarmRecord.getPresentSeverity().toString(), srMsg.toString());

        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        Map<String, Boolean> response = new HashMap<>();
        final boolean skipCurrentServiceStateUpdate = Boolean
                .parseBoolean(alarmRecord.getAdditionalInformation().remove(SKIP_CURRENT_SERVICE_STATE_UPDATE));
        final boolean multiLevelHandling = currentServiceStateUpdator.getMultilevelCapabilityValue(alarmRecord);

        if (multiLevelHandling) {
            alarmProcessingResponse = multilevelHeartbeatHandler.handleAlarm(alarmRecord);
        } else {
            if (skipCurrentServiceStateUpdate) {
                // For specific HB alarms, the dps update for currentServiceState is not required. This will be identified by the additional
                // attribute that populated while creating the EventNotification. The alarms will be displayed in the UI.
                LOGGER.debug("currentServiceState update in DPS is not required for the OOR {} ", alarmRecord.getObjectOfReference());
                response.put(ALARM_TO_BE_PROCESSED, true);
            } else {
                // update current service state of FDN and check if further processing is required and also whether sync initiation is required or
                // not.
                response = currentServiceStateUpdator.updateForHeartBeatAlarm(alarmRecord);
            }
            if (response.get(ALARM_TO_BE_PROCESSED)) {
                final ProcessedAlarmEvent correlatedAlarm = alarmCorrelator.getCorrelatedAlarm(alarmRecord);
                if (correlatedAlarm != null && correlatedAlarm.getEventPOId() > 0) {
                    if (alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)) {
                        alarmProcessingResponse = correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
                    }
                } else {
                    alarmProcessingResponse = newAlarmProcessor.processAlarm(alarmRecord);
                }
            }
            // setting InitiateAlarmSync attribute in alarmProcessingResponse
            if (response.get(INITIATE_SYNC) != null && response.get(INITIATE_SYNC)) {
                alarmProcessingResponse.setInitiateAlarmSync(true);
            }
        }
        return alarmProcessingResponse;
    }
}

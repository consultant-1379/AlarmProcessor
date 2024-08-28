/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class handles alarms with record types HEARTBEAT_ALARM for Nodes with Multilevel architecture.
 */
public class MultiLevelHeartbeatHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiLevelHeartbeatHandler.class);

    @Inject
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Inject
    private AlarmValidator alarmValidator;

    @Inject
    private AlarmCorrelator alarmCorrelator;

    @Inject
    private CurrentServiceStateUpdator currentServiceStateUpdator;

    @Inject
    private AlarmProcessor newAlarmProcessor;

    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        Map<String, Boolean> response = new HashMap<>();
        LOGGER.debug("Alarm received to HeartBeatCommonHandler : {}", alarmRecord);
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        final ProcessedAlarmEvent correlatedAlarm = alarmCorrelator.getCorrelatedAlarm(alarmRecord);
        if (correlatedAlarm != null && correlatedAlarm.getEventPOId() > 0) {
            if (alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)) {
                alarmProcessingResponse = correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
            }
        } else {
            alarmProcessingResponse = newAlarmProcessor.processAlarm(alarmRecord);
        }
        if (!verifyBladeCpLevelCurrentServiceState(alarmRecord)) {
            response = currentServiceStateUpdator.updateForHeartBeatAlarm(alarmRecord);
        }

        // setting InitiateAlarmSync attribute in alarmProcessingResponse
        if (response.get(INITIATE_SYNC) != null && response.get(INITIATE_SYNC)) {
            alarmProcessingResponse.setInitiateAlarmSync(true);
        }
        return alarmProcessingResponse;
    }

    private boolean verifyBladeCpLevelCurrentServiceState(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Alarm received to verifyBladeCpLevelCurrentServiceState : {}", alarmRecord.toString());
        return currentServiceStateUpdator.checkCsstateUpdateRequired(alarmRecord);
    }

}

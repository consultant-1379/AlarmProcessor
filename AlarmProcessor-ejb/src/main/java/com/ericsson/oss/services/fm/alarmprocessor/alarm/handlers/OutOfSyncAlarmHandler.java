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

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_TO_BE_PROCESSED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.INITIATE_SYNC;

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
 * Class handles alarms with record type OUT_OF_SYNC.
 */
public class OutOfSyncAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutOfSyncAlarmHandler.class);

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

    /**
     * Method handles alarms with record type OUT_OF_SYNC when there is a correlated alarm and when there is no correlated alarm in db.
     * @param {@link ProcessedAlarmEvent} --alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Alarm received to OutOfSyncAlarmHandler : {}", alarmRecord);
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        // update current service state of FDN and check if further processing is required and also whether sync initiation is required or not.
        final Map<String, Boolean> response = currentServiceStateUpdator.updateForOutOfSyncAlarm(alarmRecord);
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
        return alarmProcessingResponse;
    }
}
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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

/**
 * Class handles alarms with record type REPEATED_NON_SYNCHABLE.
 */
public class RepeatedNonSyncAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepeatedNonSyncAlarmHandler.class);

    @Inject
    private AlarmProcessor newAlarmProcessor;

    @Inject
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Inject
    private AlarmValidator alarmValidator;

    @Inject
    private AlarmCorrelator alarmCorrelator;

    @Inject
    private NonSyncAlarmHandler nonSyncAlarmHandler;

    /**
     * Method handles alarms with record type {@link FMProcessedEventType#REPEATED_NON_SYNCHABLE} when there is a correlated alarm and when there is
     * no correlated alarm in database.
     *
     * @param {@link ProcessedAlarmEvent} --alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Alarm received to RepeatedNonSyncAlarmHandler : {}", alarmRecord);
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        // change record type to non_synchable_alarm when repeated alarm with severity cleared is received
        if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())) {
            alarmRecord.setRecordType(FMProcessedEventType.NON_SYNCHABLE_ALARM);
            alarmProcessingResponse = nonSyncAlarmHandler.handleAlarm(alarmRecord);
            LOGGER.debug("Received Clear on RepeatedNonSync Alarm for OOR {} ,Recordtype is changed to Alarm", alarmRecord.getObjectOfReference());
            return alarmProcessingResponse;
        }
        final ProcessedAlarmEvent correlatedAlarm = alarmCorrelator.getCorrelatedAlarm(alarmRecord);
        if (correlatedAlarm != null && correlatedAlarm.getEventPOId() > 0) {
            if (alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)) {
                alarmProcessingResponse = correlatedAlarmProcessor.processRepeated(alarmRecord, correlatedAlarm);
            }
        } else {
            alarmProcessingResponse = newAlarmProcessor.processAlarm(alarmRecord);
        }
        return alarmProcessingResponse;
    }
}
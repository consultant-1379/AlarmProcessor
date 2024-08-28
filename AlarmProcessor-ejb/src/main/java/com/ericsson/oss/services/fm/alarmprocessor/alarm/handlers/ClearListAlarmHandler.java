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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.APS_SERVICE_ID;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.DELETE_NETWORK_ELEMENT_PROBLEM_TEXT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATED_EVENT_PO_ID;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_ALARM_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_UPDATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_TEXT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PSEUDO_PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PSEUDO_PREVIOUS_SEVERITY;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.common.builder.ProcessedAlarmEventBuilder;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;

/**
 * Class to handle Alarm with RecordType CLEAR_LIST All Open Alarms corresponding to the NetworkElement for which the alarm is received are
 * ceased.Alarm is sent to FMX.
 */
public class ClearListAlarmHandler {

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    /**
     * Cease all open Alarms present in database for the corresponding NetworkElement.Alarm is communicated to FMX.
     *
     * @param {@link ProcessedAlarmEvent}--alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        final String fdn = alarmRecord.getFdn();
        systemRecorder.recordEvent("APS", EventLevel.DETAILED, "NODE  ", fdn,
                "NetworkElement is deleted and all corresponding Open Alarms are to be cleared");
        final Iterator<PersistenceObject> poIterator = alarmReader.readAlarms(fdn, false, false);

        while (poIterator.hasNext()) {
            final PersistenceObject alarmFromDataBase = poIterator.next();
            final Map<String, Object> alarmAttributes = alarmFromDataBase.getAllAttributes();
            final Long eventPoId = alarmFromDataBase.getPoId();
            updateAlarmToBeCleared(eventPoId, alarmAttributes);
            final ProcessedAlarmEvent alarmToBeCleared = ProcessedAlarmEventBuilder.buildProcessedAlarm(alarmAttributes);
            alarmToBeCleared.setEventPOId(eventPoId);
            alarmAttributesPopulator.updateLastDeliveredTime(alarmToBeCleared, alarmToBeCleared, alarmAttributes);
            openAlarmService.removeAlarm(alarmToBeCleared.getCorrelatedPOId(), alarmAttributes);
            alarmProcessingResponse.getProcessedAlarms().add(alarmToBeCleared);
        }
        alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
        apsInstrumentedBean.incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
        return alarmProcessingResponse;
    }

    /**
     * Method prepares clear alarm for alarm with clear_list record type.
     *
     * @param {@link Long} eventPoId
     * @param (@link Map) alarmAttributes
     */
    private void updateAlarmToBeCleared(final Long eventPoId, final Map<String, Object> alarmAttributes) {
        final Date ceaseTime = new Date();
        // Active Alarm
        if (!ProcessedEventSeverity.CLEARED.name().equals(alarmAttributes.get(PRESENT_SEVERITY))) {
            alarmAttributes.put(PREVIOUS_SEVERITY, alarmAttributes.get(PRESENT_SEVERITY));
            alarmAttributes.put(PRESENT_SEVERITY, ProcessedEventSeverity.CLEARED.name());
            alarmAttributes.put(CEASE_TIME, ceaseTime);
            alarmAttributes.put(CEASE_OPERATOR, APS_SERVICE_ID);
        }

        alarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_ACKNOWLEDGED.toString());
        alarmAttributes.put(LAST_UPDATED, ceaseTime);
        alarmAttributes.put(LAST_ALARM_OPERATION, ProcessedLastAlarmOperation.CLEAR.name());
        alarmAttributes.put(CORRELATED_EVENT_PO_ID, eventPoId);
        alarmAttributes.put(PROBLEM_TEXT, DELETE_NETWORK_ELEMENT_PROBLEM_TEXT);
        alarmAttributes.put(PSEUDO_PRESENT_SEVERITY, alarmAttributes.get(PSEUDO_PRESENT_SEVERITY));
        alarmAttributes.put(PSEUDO_PREVIOUS_SEVERITY, alarmAttributes.get(PSEUDO_PREVIOUS_SEVERITY));
    }
}
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

package com.ericsson.oss.services.fm.alarmprocessor.processors;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_ALARM_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_UPDATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_DETAIL;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AdditionalAttributesRetainer;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;

/**
 * Class for processing alarm when correlated alarm is cleared in db.
 */
public class CorrelatedClearAlarmProcessor {

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private OscillationCorrelationProcessor oscillationCorrelationProcessor;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    /**
     * Method processes alarm when correlated alarm severity is cleared. 1. Deleted alarm from db when hide is received. 2. Adds alarm to cache when
     * clear is received. 3. when other than these are received, updates alarm is oscillation is ON else inserts alarm as new record. 4. Checks if
     * clear event is present in cache and processes it.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param correlatedAlarm
     *            {@link ProcessedAlarmEvent}
     * @return {@link AlarmProcessingResponse}
     */
    public AlarmProcessingResponse processAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        // Scenario 1: original -> hide -> clear (but the clear was processed before hide request)
        if (FMX_HIDE.equals(alarmRecord.getAdditionalInformation().get(HIDE_OPERATION))) {
            alarmProcessingResponse = processHideOnClear(alarmRecord, correlatedAlarm);
            apsInstrumentedBean.incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
            return alarmProcessingResponse;
        }
        // Scenario 2: Clear on Clear should be stored in clear Cache
        if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())) {
            clearAlarmsCacheManager.addClearAlarm(alarmRecord);
            apsInstrumentedBean.incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
            return alarmProcessingResponse;
        }
        // Scenario 3: Oscillation ON/OFF case
        oscillationCorrelationProcessor.processAlarm(alarmRecord, correlatedAlarm);
        // At the end checking for clear event in clear alarm cache and processing it if present
        alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
        clearAlarmsCacheManager.checkAndProcessForClearAlarm(alarmRecord, alarmProcessingResponse);
        return alarmProcessingResponse;
    }

    public Map<String, Object> populateAlarmAttributesForRemoving(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        final long poId = correlatedAlarm.getEventPOId();
        final Map<String, Object> alarmAttributes = new HashMap<String, Object>();
        final Date lastUpdated = new Date();
        alarmAttributes.put(LAST_UPDATED, lastUpdated);
        // TORF-241695 - Retain attributes from additionalAttribute of an alarm present in database.
        final Map<String, String> additionalInformation = alarmRecord.getAdditionalInformation();
        AdditionalAttributesRetainer.retainAdditionalAttributesPresentInDatabase(additionalInformation, correlatedAlarm.getAdditionalInformation());
        alarmRecord.setAdditionalInformation(additionalInformation);
        alarmAttributes.put(ADDITIONAL_INFORMATION, alarmRecord.getAdditionalInformationString());
        alarmAttributes.put(VISIBILITY, alarmRecord.getVisibility());
        alarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_ACKNOWLEDGED.name());
        alarmAttributes.put(ACK_OPERATOR, correlatedAlarm.getAckOperator());
        alarmAttributes.put(ACK_TIME, correlatedAlarm.getAckTime());
        alarmRecord.setAlarmState(ProcessedEventState.CLEARED_ACKNOWLEDGED);
        alarmRecord.setAckOperator(correlatedAlarm.getAckOperator());
        alarmRecord.setAckTime(correlatedAlarm.getAckTime());
        alarmRecord.setLastUpdatedTime(lastUpdated);
        alarmRecord.setProblemDetail(AlarmProcessorConstants.DELETE_ALARM_PROBLEM_DETAIL_FMX);
        alarmRecord.setEventPOId(poId);
        alarmRecord.setCorrelatedPOId(poId);
        alarmAttributes.put(CEASE_OPERATOR, correlatedAlarm.getCeaseOperator());
        alarmAttributes.put(CEASE_TIME, correlatedAlarm.getCeaseTime());
        alarmAttributes.put(LAST_ALARM_OPERATION, correlatedAlarm.getLastAlarmOperation().name());
        alarmAttributes.put(PRESENT_SEVERITY, correlatedAlarm.getPresentSeverity().name());
        alarmAttributes.put(PREVIOUS_SEVERITY, correlatedAlarm.getPreviousSeverity().name());
        alarmAttributes.put(PROBLEM_DETAIL, AlarmProcessorConstants.DELETE_ALARM_PROBLEM_DETAIL_FMX);
        alarmRecord.setCeaseOperator(correlatedAlarm.getCeaseOperator());
        alarmRecord.setCeaseTime(correlatedAlarm.getCeaseTime());
        alarmRecord.setLastAlarmOperation(correlatedAlarm.getLastAlarmOperation());
        alarmRecord.setPresentSeverity(correlatedAlarm.getPresentSeverity());
        alarmRecord.setPreviousSeverity(correlatedAlarm.getPreviousSeverity());
        return alarmAttributes;
    }

    /**
     * Method which processes hide on clear(race condition). Correlates alarm and deletes alarm from db.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param correlatedAlarm
     *            {@link ProcessedAlarmEvent}
     * @return {@link AlarmProcessingResponse}
     */
    private AlarmProcessingResponse processHideOnClear(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        // update alarm in the DB and remove it from UI
        if (!alarmRecord.isVisibility()) {
            alarmRecord.setAlarmState(ProcessedEventState.CLEARED_UNACKNOWLEDGED);
            final Map<String, Object> alarmAttributes = populateAlarmAttributesForRemoving(alarmRecord, correlatedAlarm);
            alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, correlatedAlarm, alarmAttributes);
            openAlarmService.removeAlarm(correlatedAlarm.getEventPOId(), alarmAttributes);
            alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
        }
        return alarmProcessingResponse;
    }
}

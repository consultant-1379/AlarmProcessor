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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ORIGINAL_RECORD_TYPE;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.populateUpdateAlarm;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.EVENT_PO_ID;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATEDVISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATED_EVENT_PO_ID;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.INSERT_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_ALARM_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OBJECT_OF_REFERENCE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OSCILLATION_COUNT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_DETAIL;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_TEXT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.REPEAT_COUNT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.ServiceProxyProviderBean;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.FakeClearToNorthBoundUtility;
import com.ericsson.oss.services.fm.common.builder.ProcessedAlarmEventBuilder;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;

/**
 * Class handles alarm with RecordType {@link FMProcessedEventType#UPDATE}. This class mainly deals with alarms processed by FMX.
 */
public class UpdateAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAlarmHandler.class);

    @Inject
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    /**
     * Method handles alarm with RecordType {@link FMProcessedEventType#UPDATE} received from FMX It does the following while updating
     * <p>
     * 1.Checks whether the alarm with the given poId exists in database or not. 2 Updates the alarm in database with the received update alarm
     * attributes if it exists. 3.Discards the alarm if the alarm does not exist in database
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @return {@link AlarmProcessingResponse}
     */
    public AlarmProcessingResponse handleAlarm(ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Alarm received to UpdateAlarmProcesor: {}", alarmRecord);
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        final long eventPoId = Long.parseLong(alarmRecord.getAdditionalInformation().get(EVENT_PO_ID));
        final Map<String, Object> existingAlarmAttributes = alarmReader.readAllAttributes(eventPoId);
        if (existingAlarmAttributes != null && !existingAlarmAttributes.isEmpty()) {
            alarmRecord = processUpdateAlarm(alarmRecord, eventPoId, existingAlarmAttributes);
            //Check if fakeClearToNorthBound is required or not.
            alarmProcessingResponse.setSendFakeClearToNbi(FakeClearToNorthBoundUtility.checkIfFakeClearIsToBeSentToNorthBound(alarmRecord,
                    existingAlarmAttributes));
        } else {
            LOGGER.warn("Alarm with the poid : {} does not exist in the database. Alarm is not updated", eventPoId);
            LOGGER.debug("Alarm with the poid : {} does not exist in the database. Alarm : {} is not updated", eventPoId,alarmRecord);
            apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
            alarmRecord.setProblemText("FMX UPDATE failed as the alarm does not exist in FM DB");
            serviceProxyProviderBean.getAlarmSender().sendAlarm(alarmRecord);
            apsInstrumentedBean.incrementFailedFmxUpdateAlarmsCount();
            LOGGER.warn("Reported the UPDATE failure to FMX, so this Alarm with poid : {} can be handled in ErrorPort rule configuration ",eventPoId);
            LOGGER.debug("Reported the UPDATE failure to FMX, so this Alarm : {} can be handled in ErrorPort rule configuration ",alarmRecord );
            return alarmProcessingResponse;
        }
        alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
        return alarmProcessingResponse;
    }

    /**
     * Method sets severity for update alarm.
     * @param processedAlarmEvent
     *            {@link ProcessedAlarmEvent}
     * @param existingAlarmAttributes
     *            {@link Map}
     */
    public void setSeverity(final ProcessedAlarmEvent processedAlarmEvent, final Map<String, Object> existingAlarmAttributes) {
        final String perceivedSeverity = existingAlarmAttributes.get(PRESENT_SEVERITY).toString();
        LOGGER.debug("Updating the processedAlarmEvent {} with previousSeverity with the preceivedSeverity is {}", processedAlarmEvent,
                perceivedSeverity);
        if (ProcessedEventSeverity.CLEARED.name().equals(perceivedSeverity)) {
            final Map<String, Object> updatedAlarmAttributes = populateUpdateAlarm(processedAlarmEvent);
            final String fmxPresentSeverity = (String) updatedAlarmAttributes.get(PRESENT_SEVERITY);
            // Update on clear(occurs during race conditions only) - so setting alarm's previous severity to PresentSeverity received from FMX
            if (!fmxPresentSeverity.equals(perceivedSeverity)) {
                processedAlarmEvent.setPreviousSeverity(ProcessedEventSeverity.valueOf(fmxPresentSeverity));
                processedAlarmEvent.setPresentSeverity(ProcessedEventSeverity.valueOf(perceivedSeverity));
            }
            processedAlarmEvent.setAlarmState(ProcessedEventState.CLEARED_UNACKNOWLEDGED);
        } else {
            // this block is executed when update is received on non-cleared alarm.
            processedAlarmEvent.setLastAlarmOperation(ProcessedLastAlarmOperation.CHANGE);
            // Checking if the PresentSeverity of the Correlated alarm and the FMX Update/ShowHide alarm is same.
            if (perceivedSeverity.equals(processedAlarmEvent.getPresentSeverity().name())) {
                processedAlarmEvent.setPreviousSeverity(ProcessedEventSeverity.valueOf(existingAlarmAttributes.get(PREVIOUS_SEVERITY).toString()));
            } else {
                processedAlarmEvent.setPreviousSeverity(ProcessedEventSeverity.valueOf(perceivedSeverity));
            }
        }
    }

    /**
     * Method that Updates alarm in database with updatedAlarmAttributes received in FMX update It does the following while updating
     * <p>
     * 1.It discards the Alarm if the OOR value is changed in the request.
     * <p>
     * 2.Updates the alarm in database with the received update alarm attributes
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param eventPoId
     *            {@link long}
     * @param existingAlarmAttributes
     *            {@linkMap }
     */
    private ProcessedAlarmEvent processUpdateAlarm(final ProcessedAlarmEvent inputAlarmRecord, final long eventPoId,
            final Map<String, Object> existingAlarmAttributes) {
        inputAlarmRecord.setEventPOId(eventPoId);
        ProcessedAlarmEvent updatedAlarm = null;
        final String perceivedSeverity = existingAlarmAttributes.get(PRESENT_SEVERITY).toString();
        final String oor = existingAlarmAttributes.get(OBJECT_OF_REFERENCE).toString();
        final String ceaseOperator = existingAlarmAttributes.get(CEASE_OPERATOR).toString();
        final String lastAlarmOperation = existingAlarmAttributes.get(LAST_ALARM_OPERATION).toString();

        if (isValidObjectOfRefernce(inputAlarmRecord, perceivedSeverity, oor)) {
            inputAlarmRecord.setEventPOId(Long.parseLong(inputAlarmRecord.getAdditionalInformation().get(EVENT_PO_ID)));
            final Map<String, Object> updatedAlarmAttributes = buildForUpdateAlarm(inputAlarmRecord, existingAlarmAttributes);
            final String addInfo = (String) existingAlarmAttributes.get(ADDITIONAL_INFORMATION);
            final Boolean correlatedVisibility = (Boolean) existingAlarmAttributes.get(VISIBILITY);
            updatedAlarmAttributes.put(CORRELATEDVISIBILITY, correlatedVisibility);
            updatedAlarmAttributes.put(PREVIOUS_SEVERITY, perceivedSeverity);
            updatedAlarmAttributes.put(CEASE_OPERATOR, ceaseOperator);
            if(lastAlarmOperation.equals("CLEAR")) {
                updatedAlarmAttributes.put(LAST_ALARM_OPERATION, ProcessedLastAlarmOperation.CHANGE.name());
            }
            // The last alarm operation should be NEW only when the alarm is first shown. So, Verifying if the correlated alarm contains hideOperation
            // in additional attributes which should be present for any alarms which are already shown/hidden by FMX. CorrelatedVisibility is verified
            // to avoid setting last alarm operation NEW for the alarms which are being hidden by FMX.
            if (!addInfo.contains(HIDE_OPERATION) && correlatedVisibility != null && !correlatedVisibility) {
                updatedAlarmAttributes.put(LAST_ALARM_OPERATION, ProcessedLastAlarmOperation.NEW.name());
            }
            updatedAlarm = ProcessedAlarmEventBuilder.buildProcessedAlarm(updatedAlarmAttributes);
            alarmAttributesPopulator.updateLastDeliveredTime(inputAlarmRecord, updatedAlarm, updatedAlarmAttributes);

            LOGGER.debug("ProcessedAlarmEvent after updating the AdditionallyRequiredAttibutes values : {}", updatedAlarm);
            openAlarmService.updateAlarm(eventPoId, updatedAlarmAttributes);
            if (ProcessedEventSeverity.CLEARED.name().equals(perceivedSeverity)
                    && FMX_HIDE.equals(inputAlarmRecord.getAdditionalInformation().get(HIDE_OPERATION)) && !inputAlarmRecord.getVisibility()) {
                final Map<String, Object> attributes =
                        getAttributes(inputAlarmRecord, eventPoId, AlarmProcessorConstants.DELETE_ALARM_PROBLEM_DETAIL_FMX);
                // removing the open alarm PO when a hide request comes on clear alarm.
                openAlarmService.removeAlarm(eventPoId, attributes);
            }
            final FMProcessedEventType recordType = inputAlarmRecord.getRecordType();
            if (recordType != null
                    && (FMProcessedEventType.ERROR_MESSAGE.equals(recordType) || FMProcessedEventType.REPEATED_ERROR_MESSAGE.equals(recordType))
                    && !inputAlarmRecord.getVisibility()) {
                final Map<String, Object> attributes =
                        getAttributes(inputAlarmRecord, eventPoId, AlarmProcessorConstants.DELETE_ALARM_PROBLEM_DETAIL_FMX);
                openAlarmService.removeAlarm(eventPoId, attributes); // removing the state-less alarm PO when a hide request comes from FMX.
            }
            apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(ProcessedEventSeverity.valueOf(perceivedSeverity));
        }
        if (updatedAlarm != null) {
            updatedAlarm.setEventPOId(eventPoId);
        }
        return updatedAlarm;
    }

    private boolean isValidObjectOfRefernce(final ProcessedAlarmEvent inputAlarmRecord, final String perceivedSeverity, final String oor) {
        if (oor.equals(inputAlarmRecord.getObjectOfReference())) {
            return true;
        } else {
            LOGGER.error("Alarm with poid : {} has been discarded because object of refernce is changed .", inputAlarmRecord.getEventPOId());
            LOGGER.debug("Alarm : {} has been discarded because object of refernce is changed .", inputAlarmRecord);
            apsInstrumentedBean.incrementDiscardedAlarmCount(ProcessedEventSeverity.valueOf(perceivedSeverity));
            return false;
        }
    }

    private Map<String, Object> getAttributes(final ProcessedAlarmEvent alarmRecord, final long eventPoId, final String problemDetail) {
        final Map<String, Object> alarmAttributes = new HashMap<>();
        alarmAttributes.put(PROBLEM_DETAIL, problemDetail);
        alarmRecord.setProblemDetail(problemDetail);
        final List<String> outputAttributes = new ArrayList<>();
        outputAttributes.add(PROBLEM_TEXT);
        outputAttributes.add(INSERT_TIME);
        outputAttributes.add(ACK_OPERATOR);
        outputAttributes.add(ACK_TIME);
        final Map<String, Object> attributes = alarmReader.readAttributes(alarmRecord.getCorrelatedPOId(), outputAttributes);
        final String presentAlarmState = (String) attributes.get(ALARM_STATE);
        if (ProcessedEventState.ACTIVE_ACKNOWLEDGED.name().equals(presentAlarmState)) {
            setAckInformation(alarmRecord, attributes);
        } else {
            alarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_UNACKNOWLEDGED.name());
            alarmRecord.setAlarmState(ProcessedEventState.CLEARED_UNACKNOWLEDGED);
        }
        alarmAttributes.put(CORRELATED_EVENT_PO_ID, eventPoId);
        alarmRecord.setCorrelatedPOId(eventPoId);
        return alarmAttributes;
    }

    /**
     * Method builds update alarm attributes from {@link ProcessedAlarmEvent}.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param existingAlarmAttributes
     *            {@link Map}
     * @return {@link Map} updatedAlarmAttributes
     */
    private Map<String, Object> buildForUpdateAlarm(final ProcessedAlarmEvent alarmRecord, final Map<String, Object> existingAlarmAttributes) {
        setSeverity(alarmRecord, existingAlarmAttributes);
        alarmRecord.setInsertTime((Date) existingAlarmAttributes.get(INSERT_TIME));
        final Date currentTime = new Date();
        alarmRecord.setLastUpdatedTime(currentTime);
        alarmRecord.setCeaseTime((Date) existingAlarmAttributes.get(CEASE_TIME));
        alarmRecord.setRepeatCount((Integer) existingAlarmAttributes.get(REPEAT_COUNT));
        alarmRecord.setOscillationCount((Integer) existingAlarmAttributes.get(OSCILLATION_COUNT));
        if (existingAlarmAttributes.get(ADDITIONAL_INFORMATION).toString().contains(FMProcessedEventType.NON_SYNCHABLE_ALARM.name())) {
            alarmRecord.getAdditionalInformation().put(ORIGINAL_RECORD_TYPE, FMProcessedEventType.NON_SYNCHABLE_ALARM.name());
        }
        final Map<String, Object> updatedAlarmAttributes = populateUpdateAlarm(alarmRecord);
        final Object originalRecordType = existingAlarmAttributes.get(RECORD_TYPE);
        // When FMX UPDATE is performed on E-MSG/R-E-MSG, the recordType should be restored to the original.
        // As the ACK on alarm with recordType UPDATE would not delete the event from DB though the original alarm is state-less.
        if (originalRecordType != null
                && (FMProcessedEventType.ERROR_MESSAGE.name().equals(originalRecordType.toString()) || FMProcessedEventType.REPEATED_ERROR_MESSAGE
                        .name().equals(originalRecordType.toString()))) {
            updatedAlarmAttributes.put(RECORD_TYPE, originalRecordType);
            alarmRecord.setRecordType(FMProcessedEventType.valueOf(originalRecordType.toString()));
        }
        final Set<String> alarmAttributeNames = updatedAlarmAttributes.keySet();
        for (final String attributeName : alarmAttributeNames) {
            updatedAlarmAttributes.computeIfAbsent(attributeName, k -> existingAlarmAttributes.get(attributeName));
        }
        return updatedAlarmAttributes;
    }

    /**
     * AlarmRecord is built with ack information.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param attributes
     *            {@link Map}
     */
    private void setAckInformation(final ProcessedAlarmEvent alarmRecord, final Map<String, Object> attributes) {
        final String ackOperator = (String) attributes.get(ACK_OPERATOR);
        final Date ackTime = (Date) attributes.get(ACK_TIME);
        alarmRecord.setAckOperator(ackOperator);
        alarmRecord.setAckTime(ackTime);
        alarmRecord.setAlarmState(ProcessedEventState.CLEARED_ACKNOWLEDGED);
    }
}

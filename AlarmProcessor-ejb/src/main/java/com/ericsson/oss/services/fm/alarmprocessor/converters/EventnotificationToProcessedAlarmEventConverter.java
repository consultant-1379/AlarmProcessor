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

package com.ericsson.oss.services.fm.alarmprocessor.converters;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ORIGINAL_EVENTTIME_FROM_NODE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ORIGINAL_RECORD_TYPE;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.getAlarmingObject;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.getManagedObject;
import static com.ericsson.oss.services.fm.alarmprocessor.util.DateAndTimeFormatter.parseTime;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.ACKNOWLEDGER;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.ACKNOWLEDGE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.ADDITIONAL_TEXT;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.BACKEDUP_STATUS;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.BACKUP_OBJECT;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.EVENT_AGENT_ID;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.EXTERNAL_EVENT_ID;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.EXT_ACKNOWLEDGER;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.EXT_ACKNOWLEDGE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.EXT_PRA;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.GENERATED_ALARM_ID;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.MANAGED_OBJECT;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.NOTIFY_CHANGED_ALARM;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.PROPOSED_REPAIR_ACTIONS;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.TRANSLATE_RESULT;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.TREND_INDICATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_ID;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.EventTypeConstants.COMMUNICATIONS_ALARM;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.EMPTY_STRING;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.TRUE;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.UNDER_SCORE_DELIMITER;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventTrendIndication;

/**
 * Class responsible for converting {@link EventNotification} to {@link ProcessedAlarmEvent}.
 */
public final class EventnotificationToProcessedAlarmEventConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventnotificationToProcessedAlarmEventConverter.class);

    private EventnotificationToProcessedAlarmEventConverter() {
    }

    /**
     * Method extracts information from received EventNotification and sets in ProcessedAlarmEvent.
     * @param eventNotification
     *            {@link EventNotification}
     * @return {@link ProcessedAlarmEvent}
     */
    public static ProcessedAlarmEvent convert(final EventNotification eventNotification) {
        final ProcessedAlarmEvent alarmRecord = new ProcessedAlarmEvent();
        final String managedObjectReference = eventNotification.getManagedObjectInstance();
        alarmRecord.setObjectOfReference(managedObjectReference);
        alarmRecord.setAlarmingObject(getAlarmingObject(managedObjectReference));
        alarmRecord.setManagedObject(getManagedObject(managedObjectReference));
        alarmRecord.setTimeZone(eventNotification.getTimeZone());
        setRecordType(alarmRecord, eventNotification);
        if (eventNotification.getRecordType().equals(FMProcessedEventType.ALARM.name())) {
            setRecordTypeBasedOnSpecificProblem(alarmRecord, eventNotification);
        }
        alarmRecord.setPresentSeverity(getSeverity(eventNotification.getPerceivedSeverity()));
        alarmRecord.setEventTime(parseTime(eventNotification.getEventTime(), eventNotification.getTimeZone()));
        if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())) {
            // Setting default state and ceasetime for clear alarms. Ref. TORF-380583
            alarmRecord.setAlarmState(ProcessedEventState.CLEARED_UNACKNOWLEDGED);
            alarmRecord.setCeaseTime(alarmRecord.getEventTime());
        } else {
            alarmRecord.setAlarmState(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        }
        if (eventNotification.isAcknowledged()) {
            alarmRecord.setAlarmState(ProcessedEventState.ACTIVE_ACKNOWLEDGED);
            alarmRecord.setAckOperator(eventNotification.getOperator());
            alarmRecord.setAckTime(parseTime(eventNotification.getAckTime(), eventNotification.getTimeZone()));
        }
        if (eventNotification.getAdditionalAttributes() != null) {
            alarmRecord.setAlarmNumber(extractAlarmNumber(eventNotification.getAdditionalAttributes()));
            alarmRecord.setAlarmId(extractAlarmNumber(eventNotification.getAdditionalAttributes()));
            setAdditionalAttributes(alarmRecord, eventNotification.getAdditionalAttributes());
        }
        alarmRecord.getAdditionalInformation().put(EVENT_AGENT_ID, eventNotification.getEventAgentId());
        alarmRecord.getAdditionalInformation().put(EXTERNAL_EVENT_ID, eventNotification.getExternalEventId());
        alarmRecord.getAdditionalInformation().put(TRANSLATE_RESULT, eventNotification.getTranslateResult());
        alarmRecord.getAdditionalInformation().put(OPERATOR, eventNotification.getOperator());
        // Original EventTime received from Node is added to AdditionalAttributes.This is used to validate alarms.
        if (alarmRecord.getAdditionalInformation().get(ORIGINAL_EVENTTIME_FROM_NODE) == null) {
            alarmRecord.getAdditionalInformation().put(ORIGINAL_EVENTTIME_FROM_NODE, String.valueOf(alarmRecord.getEventTime().getTime()));
        }
        if (eventNotification.getAdditionalAttribute(FDN) != null) {
            alarmRecord.setFdn(eventNotification.getAdditionalAttribute(FDN));
        }
        alarmRecord.setSpecificProblem(eventNotification.getSpecificProblem() == null ? EMPTY_STRING : eventNotification.getSpecificProblem());
        alarmRecord.setProbableCause(eventNotification.getProbableCause() == null ? EMPTY_STRING : eventNotification.getProbableCause());
        alarmRecord.setEventType(eventNotification.getEventType() == null ? EMPTY_STRING : eventNotification.getEventType());
        if (eventNotification.getRecordType().equals(FMProcessedEventType.SYNCHRONIZATION_ENDED.name())
                || eventNotification.getRecordType().equals(FMProcessedEventType.SYNCHRONIZATION_STARTED.name())) {
            alarmRecord.setPresentSeverity(ProcessedEventSeverity.INDETERMINATE);
        } else if (eventNotification.getRecordType().equals(FMProcessedEventType.SYNCHRONIZATION_ABORTED.name())) {
            setSyncAbortAttributes(alarmRecord);
        } else if (FMProcessedEventType.NON_SYNCHABLE_ALARM.name().equals(eventNotification.getRecordType())
                || FMProcessedEventType.REPEATED_NON_SYNCHABLE.name().equals(eventNotification.getRecordType())) {
            alarmRecord.getAdditionalInformation().put(ORIGINAL_RECORD_TYPE, FMProcessedEventType.NON_SYNCHABLE_ALARM.name());
        }
        return alarmRecord;
    }

    private static void setRecordType(final ProcessedAlarmEvent alarmRecord, final EventNotification eventNotification) {
        try {
            if (eventNotification.getRecordType() != null) {
                alarmRecord.setRecordType(FMProcessedEventType.valueOf(eventNotification.getRecordType()));
            } else {
                alarmRecord.setRecordType(FMProcessedEventType.UNDEFINED);
            }
        } catch (final IllegalArgumentException illegalArgumentException) {
            LOGGER.trace("Exception occured while setting recordType in ProcessedAlarmEvent,Exception details are:", illegalArgumentException);
            alarmRecord.setRecordType(FMProcessedEventType.UNDEFINED);
        }
    }

    private static void setRecordTypeBasedOnSpecificProblem(final ProcessedAlarmEvent alarmRecord, final EventNotification event) {
        if (AlarmProcessorConstants.ALARMSUPPRESSED_SP.equals(event.getSpecificProblem())) {
            alarmRecord.setRecordType(FMProcessedEventType.ALARM_SUPPRESSED_ALARM);
        } else if (AlarmProcessorConstants.TECHNICIANPRESENT_SP.equals(event.getSpecificProblem())) {
            alarmRecord.setRecordType(FMProcessedEventType.TECHNICIAN_PRESENT);
        }
    }

    private static ProcessedEventSeverity getSeverity(final String severity) {
        try {
            if (severity != null) {
                return ProcessedEventSeverity.valueOf(severity);
            } else {
                return ProcessedEventSeverity.INDETERMINATE;
            }
        } catch (final IllegalArgumentException illegalArgumentException) {
            LOGGER.trace("Exception occured while fetching severity,Exception details are: ", illegalArgumentException);
            return ProcessedEventSeverity.INDETERMINATE;
        }
    }

    private static void setSyncAbortAttributes(final ProcessedAlarmEvent alarmRecord) {
        alarmRecord.setEventType(COMMUNICATIONS_ALARM);
        alarmRecord.setSpecificProblem(AlarmProcessorConstants.SP_SYNCABORTED);
        alarmRecord.setProbableCause(AlarmProcessorConstants.PC_SYNCABORTED);
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.INDETERMINATE);
    }

    /**
     * Extract Alarm Number from additionalAttributes of the alarm. generatedAlarmId is extracted from additionalAttributes and returned as
     * AlarmNumber . If generatedAlarmId is not present then alarmId is extracted and returned as AlarmNumber.
     * @param additionalAttributes
     *            {link Map}
     * @return long alarmNumber
     */
    private static long extractAlarmNumber(final Map<String, String> additionalAttributes) {
        Long alarmNumber = -2L;
        if (additionalAttributes.get(GENERATED_ALARM_ID) != null) {
            alarmNumber = convertToLong(additionalAttributes.get(GENERATED_ALARM_ID));
        }
        // if alarmId =0 need to check for external alarm id
        if (alarmNumber == -2) {
            final String externalAlarmId = additionalAttributes.get(ALARM_ID);
            if (externalAlarmId != null) {
                final String alarmId = externalAlarmId.substring(externalAlarmId.lastIndexOf(UNDER_SCORE_DELIMITER) + 1, externalAlarmId.length());
                alarmNumber = convertToLong(alarmId);
            }
        }
        return alarmNumber;
    }

    /**
     * Method converts alarmId from string to long.
     * @param String
     *            alarmId
     * @return Long alarmNumber
     */
    private static Long convertToLong(final String alarmId) {
        Long alarmNumber = -2L;
        try {
            if (alarmId != null && alarmId.length() > 0 && Long.parseLong(alarmId) > 0) {
                alarmNumber = Long.parseLong(alarmId);
            }
        } catch (final NumberFormatException numberFormatException) {
            LOGGER.debug("Setting default value for alarmNumber as NumberFormatException {} in parsing value: {}", numberFormatException, alarmId);
        }
        return alarmNumber;
    }

    /**
     * Set additional attributes on {@link ProcessedAlarmEvent} received from incoming Alarm.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param additionalAttributes
     *            {@link Map}
     */
    private static void setAdditionalAttributes(final ProcessedAlarmEvent alarmRecord, final Map<String, String> additionalAttributes) {
        final Map<String,String> additionalInformation=new HashMap<>();
        try {
            for (final Map.Entry<String, String> additionalAttribute : additionalAttributes.entrySet()) {
                if (null != additionalAttribute.getKey() && null != additionalAttribute.getValue() && !additionalAttribute.getValue().isEmpty()) {
                    updateAlarmRecordAdditionalAttributes(alarmRecord, additionalAttribute,additionalInformation);
                }
            }
        } catch (final Exception exception) {
            LOGGER.warn("Exception with setAdditionalAttributes for {} is {}", alarmRecord.getAdditionalInformationString(), exception.getMessage());
            LOGGER.debug("Exception with setAdditionalAttributes for {} is {}", alarmRecord, exception);
        }
        additionalInformation.put(MANAGED_OBJECT, alarmRecord.getManagedObject());
        alarmRecord.setAdditionalInformation(additionalInformation);
    }

    private static void updateAlarmRecordAdditionalAttributes(final ProcessedAlarmEvent alarmRecord, final Map.Entry<String, String> additionalAttribute,final Map<String, String> additionalInformation) {
        // backedUp status values
        final String zero = "0";
        final String one = "1";
        switch (additionalAttribute.getKey()) {
            case ADDITIONAL_TEXT:
                alarmRecord.setProblemText(additionalAttribute.getValue());
                break;
            case BACKEDUP_STATUS:
                if (zero.equalsIgnoreCase(additionalAttribute.getValue())) {
                    alarmRecord.setBackupStatus(false);
                } else if (one.equalsIgnoreCase(additionalAttribute.getValue())) {
                    alarmRecord.setBackupStatus(true);
                }
                break;
            case BACKUP_OBJECT:
                alarmRecord.setBackupObjectInstance(additionalAttribute.getValue());
                break;
            case TREND_INDICATION:
                setTrendIndication(alarmRecord, additionalAttribute);
                break;
            case ACKNOWLEDGER:
                additionalInformation.put(EXT_ACKNOWLEDGER, additionalAttribute.getValue());
                break;
            case ACKNOWLEDGE_TIME:
                additionalInformation.put(EXT_ACKNOWLEDGE_TIME, additionalAttribute.getValue());
                break;
            case PROPOSED_REPAIR_ACTIONS:
                additionalInformation.put(EXT_PRA, additionalAttribute.getValue());
                break;
            case GENERATED_ALARM_ID:
                alarmRecord.setAlarmId(new Long(additionalAttribute.getValue()));
                additionalInformation.put(additionalAttribute.getKey(), additionalAttribute.getValue());
                break;
            case NOTIFY_CHANGED_ALARM:
                if (TRUE.equalsIgnoreCase(additionalAttribute.getValue())
                        && !alarmRecord.getRecordType().equals(FMProcessedEventType.SYNCHRONIZATION_ALARM)
                        && !alarmRecord.getRecordType().equals(FMProcessedEventType.ALARM_SUPPRESSED_ALARM)
                        && !alarmRecord.getRecordType().equals(FMProcessedEventType.TECHNICIAN_PRESENT)) {
                    alarmRecord.setRecordType(FMProcessedEventType.REPEATED_ALARM);
                }
                break;
            default:
                additionalInformation.put(additionalAttribute.getKey(), additionalAttribute.getValue());
        }
    }

    private static void setTrendIndication(final ProcessedAlarmEvent alarmRecord, final Map.Entry<String, String> additionalAttribute) {
        try {
            alarmRecord.setTrendIndication(ProcessedEventTrendIndication.valueOf(additionalAttribute.getValue()));
        } catch (final IllegalArgumentException illegalArgumentException) {
            LOGGER.trace("Exception occured while setting trendIndication in ProcessedAlarmEvent,Exception details are:{}", illegalArgumentException);
            alarmRecord.setTrendIndication(ProcessedEventTrendIndication.UNDEFINED);
        }
    }
}

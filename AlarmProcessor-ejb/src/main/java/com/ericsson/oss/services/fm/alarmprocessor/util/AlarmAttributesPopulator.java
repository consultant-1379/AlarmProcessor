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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.APS;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.LAST_DELIVERED;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.CI_GROUP_1;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.CI_GROUP_2;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.ROOT;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.TARGET_ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.TREND_INDICATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARMING_OBJECT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_ID;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_NUMBER;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.BACKUP_OBJECT_INSTANCE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.BACKUP_STATUS;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.COMMENT_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.COMMENT_TEXT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.COMMENT_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATEDVISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATED_EVENT_PO_ID;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.EVENT_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.EVENT_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FMX_GENERATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.INSERT_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_ALARM_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_UPDATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.MANUALCEASE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OBJECT_OF_REFERENCE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OSCILLATION_COUNT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBABLE_CAUSE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_DETAIL;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_TEXT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROCESSING_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROPOSED_REPAIR_ACTION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PSEUDO_PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PSEUDO_PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.REPEAT_COUNT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.SPECIFIC_PROBLEM;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_PROCESSED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.EMPTY_STRING;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.UNDER_SCORE_DELIMITER;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.SYNC_STATE;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.util.StringUtils;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.common.addinfo.TargetAdditionalInformationHandler;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;
import com.ericsson.oss.services.fm.util.PseudoSeverities;

/**
 * Class for populating alarm attributes from {@link ProcessedAlarmEvent}.
 */
public class AlarmAttributesPopulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmAttributesPopulator.class);

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private AlarmReader alarmReader;

    /**
     * Method populates alarmAttributeMap from {@link ProcessedAlarmEvent} which needs to be updated in DB and removed.
     * @param alarmRecord
     *            The alarm received from the source.
     * @param correlatedAlarm
     *            The correlated alarm found in database.
     * @return the latest attributes of alarm.
     */
    public Map<String, Object> populateDeleteAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        final long poId = alarmRecord.getCorrelatedPOId();
        final Date lastUpdated = new Date();
        alarmRecord.setLastUpdatedTime(lastUpdated);
        alarmRecord.setInsertTime(correlatedAlarm.getInsertTime());
        alarmRecord.setProblemText(correlatedAlarm.getProblemText());
        final Map<String, Object> alarmAttributes = alarmReader.readAllAttributes(poId);
        alarmAttributes.put(LAST_UPDATED, lastUpdated);
        alarmAttributes.put(PREVIOUS_SEVERITY, alarmRecord.getPreviousSeverity().name());
        alarmAttributes.put(PRESENT_SEVERITY, alarmRecord.getPresentSeverity().name());
        alarmAttributes.put(PSEUDO_PRESENT_SEVERITY, alarmRecord.getPseudoPresentSeverity());
        alarmAttributes.put(PSEUDO_PREVIOUS_SEVERITY, alarmRecord.getPseudoPreviousSeverity());
        alarmAttributes.put(ALARM_STATE, alarmRecord.getAlarmState().name());
        alarmAttributes.put(CEASE_TIME, alarmRecord.getCeaseTime());
        alarmAttributes.put(LAST_ALARM_OPERATION, alarmRecord.getLastAlarmOperation().name());
        alarmAttributes.put(CORRELATED_EVENT_PO_ID, poId);
        alarmAttributes.put(CEASE_OPERATOR, alarmRecord.getCeaseOperator());
        alarmAttributes.put(PROBLEM_TEXT, alarmRecord.getProblemText());
        return alarmAttributes;
    }

    /**
     * Method converts ProcessedAlarmEvent to a Map This method is applicable only for existing OpenAlarm Object.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @return openAlarmAttributes {@link Map}
     */
    public static Map<String, Object> populateUpdateAlarm(final ProcessedAlarmEvent alarmRecord) {
        final Map<String, Object> alarmAttributes = getAlarmAttributes(alarmRecord);
        alarmAttributes.put(LAST_ALARM_OPERATION, alarmRecord.getLastAlarmOperation().name());
        alarmAttributes.put(MANUALCEASE, alarmRecord.getManualCease());
        if (alarmRecord.getAdditionalInformation().get(REPEAT_COUNT) != null) {
            alarmAttributes.put(REPEAT_COUNT, Integer.valueOf(alarmRecord.getAdditionalInformation().get(REPEAT_COUNT)));
        }
        if (alarmRecord.getAdditionalInformation().get(OSCILLATION_COUNT) != null) {
            alarmAttributes.put(OSCILLATION_COUNT, Integer.valueOf(alarmRecord.getAdditionalInformation().get(OSCILLATION_COUNT)));
        }
        return alarmAttributes;
    }

    /**
     * Method populates alarm attributes for replacing an existing alarm in the database.<br>
     * RepeatCount is incremented by one except in the case of FMX hide operation.
     * <p>
     * CommentTime,InsertTime,CommentOperator, Visibility and CorrelatedVisibility values are restored to previous values present in
     * database.(Shouldn't be updated to the new value).
     * <p>
     * Attributes present in additionalInformation of an alarm are to be retained if and only if they are not present in incoming alarm.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param poAttributes
     *            (@link Map}
     * @return map of attributes to replace in database.
     */
    public Map<String, Object> populateUpdateAlarm(final ProcessedAlarmEvent alarmRecord, final Map<String, Object> poAttributes) {
        // Initializing to the default values
        boolean previousVisibility = true;
        boolean previousCorrelatedVisibility = false;
        String processingType = NOT_SET;
        Date previousCommentTime = new Date();
        String previousCommentOperator = EMPTY_STRING;
        String previousCommentText = EMPTY_STRING;
        int repeatCount = 0;
        if (poAttributes != null && !poAttributes.isEmpty()) {
            repeatCount = (int) poAttributes.get(REPEAT_COUNT);
            previousCommentTime = (Date) poAttributes.get(COMMENT_TIME);
            previousCommentOperator = (String) poAttributes.get(COMMENT_OPERATOR);
            previousCommentText = (String) poAttributes.get(COMMENT_TEXT);
            ++repeatCount;
            final Date oldInsertTime = (Date) poAttributes.get(INSERT_TIME);
            setInsertTime(alarmRecord, oldInsertTime);
            previousVisibility = (boolean) poAttributes.get(VISIBILITY);
            previousCorrelatedVisibility = (boolean) poAttributes.get(CORRELATEDVISIBILITY);
            processingType = (String) poAttributes.get(PROCESSING_TYPE);
        }
        final Map<String, String> additionalInformation = alarmRecord.getAdditionalInformation();
        // for show/hide request on hidden repeated alarm from FMX.we are simply restricting not to increment repeat count.
        if (FMX_PROCESSED.equalsIgnoreCase(alarmRecord.getFmxGenerated())
                || (additionalInformation != null && FMX_HIDE.equalsIgnoreCase(additionalInformation.get(HIDE_OPERATION)))) {
           LOGGER.info("skipping increment of repeatcount for show/hide request on repeated alarm");
        } else {
            if (!NOT_SET.equals(processingType) && !NOT_SET.equals(alarmRecord.getProcessingType()) && !previousVisibility) {
                // Updating the Visibility & CorrelatedVisibility values to the repeatedAlarm when FMX rule is enabled.
                // As visibility should be retained when there is a hidden alarm present in DB with FMX rules active and FMX
                // operator should decide further actions on the alarm.
                alarmRecord.setVisibility(previousVisibility);
                alarmRecord.setCorrelatedVisibility(previousCorrelatedVisibility);
            } else if (!NOT_SET.equals(processingType) && !NOT_SET.equals(alarmRecord.getProcessingType()) && previousVisibility
                    && !alarmRecord.getVisibility()) {
                alarmRecord.setVisibility(false);
                alarmRecord.setCorrelatedVisibility(true);
            }
            if (additionalInformation != null) {
                additionalInformation.put(REPEAT_COUNT, Integer.toString(repeatCount));
                alarmRecord.setAdditionalInformation(additionalInformation);
            }
            alarmRecord.setCommentText(previousCommentText);
            alarmRecord.setRepeatCount(Integer.valueOf(repeatCount));
        }
        // TORF-241695 - Retain attributes from additionalAttribute of an alarm present in database.
        // Reason to set here is to ensure that the above if block is not impacted by retaining the attributes from additionalInfo.
        if (poAttributes != null) {
            AdditionalAttributesRetainer.retainAdditionalAttributesPresentInDatabase(additionalInformation,
                    (String) poAttributes.get(ADDITIONAL_INFORMATION));
        }
        alarmRecord.setAdditionalInformation(additionalInformation);
        alarmRecord.setCommentTime(previousCommentTime);
        final Map<String, Object> alarmAttributes = populateUpdateAlarm(alarmRecord);
        alarmAttributes.put(COMMENT_TIME, previousCommentTime);
        alarmAttributes.put(COMMENT_OPERATOR, previousCommentOperator);
        LOGGER.trace("Alarm attributes:{}", alarmAttributes);
        return alarmAttributes;
    }

    /**
     * Update the InsertTime in ProcessedAlarmEvent.
     * @param alarm
     *            {@link ProcessedAlarmEvent}
     * @param oldInsertTime
     *            {@link Date}
     */
    private void setInsertTime(final ProcessedAlarmEvent alarm, final Date oldInsertTime) {
        if (!configParametersListener.getUpdateInsertTime()) {
            alarm.setInsertTime(oldInsertTime);
        } else {
            final Date insertTime = new Date();
            alarm.setInsertTime(new Date(insertTime.getTime()));
        }
    }

    /**
     * Method converts ProcessedAlarmEvent to a Map. This method is applicable only for new OpenAlarm Object.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @return {@link Map} openAlarmAttributesMap
     */
    public static Map<String, Object> populateNewAlarm(final ProcessedAlarmEvent alarmRecord) {
        final Date insertTime = new Date();
        alarmRecord.setInsertTime(insertTime);
        alarmRecord.setLastUpdatedTime(insertTime);
        final Map<String, Object> alarmAttributes = getAlarmAttributes(alarmRecord);
        // To indicate New Alarm
        alarmAttributes.put(LAST_ALARM_OPERATION, ProcessedLastAlarmOperation.NEW.name());
        alarmAttributes.put(REPEAT_COUNT, 0);
        alarmAttributes.put(OSCILLATION_COUNT, 0);
        alarmRecord.getAdditionalInformation().put(LAST_DELIVERED, String.valueOf(insertTime.getTime()));
        alarmAttributes.put(LAST_DELIVERED, insertTime.getTime());
        return alarmAttributes;
    }

    /**
     * Method that builds alarm attributes when a clear is received on a hidden alarm.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param alarmAttributes
     *            Map containing alarm attributes
     * @return {@link Map } of attributes
     */
    public static Map<String, Object> populateHiddenAlarm(final ProcessedAlarmEvent alarmRecord, final Map<String, Object> alarmAttributes) {
        alarmRecord.setProblemDetail(AlarmProcessorConstants.DELETE_ALARM_PROBLEM_DETAIL);
        // Setting the AlarmState to CLEARED_ACKNOWLEDGED as the alarm is being deleted from DB, so should be at NMS.
        alarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_ACKNOWLEDGED.name());
        alarmRecord.setAlarmState(ProcessedEventState.CLEARED_ACKNOWLEDGED);
        setAckInformation(alarmRecord, alarmAttributes);
        alarmAttributes.put(PROBLEM_DETAIL, AlarmProcessorConstants.DELETE_ALARM_PROBLEM_DETAIL);
        alarmAttributes.put(PREVIOUS_SEVERITY, alarmAttributes.get(PRESENT_SEVERITY));
        alarmAttributes.put(PRESENT_SEVERITY, ProcessedEventSeverity.CLEARED.name());
        alarmAttributes.put(CEASE_TIME, alarmRecord.getEventTime());
        final Date lastUpdated = new Date();
        alarmAttributes.put(LAST_UPDATED, lastUpdated);
        alarmAttributes.put(LAST_ALARM_OPERATION, ProcessedLastAlarmOperation.CLEAR.name());
        alarmAttributes.put(CORRELATED_EVENT_PO_ID, alarmRecord.getCorrelatedPOId());
        alarmAttributes.put(CEASE_OPERATOR, alarmRecord.getCeaseOperator());
        alarmRecord.setInsertTime((Date) alarmAttributes.get(INSERT_TIME));
        alarmRecord.setLastUpdatedTime(lastUpdated);
        final String pseudoPresentSeverity = alarmAttributes.get(PRESENT_SEVERITY) + UNDER_SCORE_DELIMITER
                + ProcessedEventSeverity.CLEARED.name();
        alarmAttributes.put(PSEUDO_PRESENT_SEVERITY, PseudoSeverities.PSEUDO_SEVERITIES.get(pseudoPresentSeverity));
        alarmAttributes.put(PSEUDO_PREVIOUS_SEVERITY, PseudoSeverities.PSEUDO_SEVERITIES.get(alarmAttributes.get(PRESENT_SEVERITY)));
        return alarmAttributes;
    }

    /**
     * Method that builds clear correlated original alarm with attributes received in cease alarm.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param (@link Map} poAttributes
     */
    public Map<String, Object> populateClearAlarm(final ProcessedAlarmEvent alarmRecord) {
        final Map<String, Object> alarmAttributes = alarmReader.readAllAttributes(alarmRecord.getCorrelatedPOId());
        alarmAttributes.put(PREVIOUS_SEVERITY, alarmRecord.getPreviousSeverity().name());
        alarmAttributes.put(PRESENT_SEVERITY, alarmRecord.getPresentSeverity().name());
        alarmAttributes.put(PSEUDO_PRESENT_SEVERITY, alarmRecord.getPseudoPresentSeverity());
        alarmAttributes.put(PSEUDO_PREVIOUS_SEVERITY, alarmRecord.getPseudoPreviousSeverity());
        alarmAttributes.put(CEASE_TIME, alarmRecord.getEventTime());
        alarmAttributes.put(CEASE_OPERATOR, alarmRecord.getCeaseOperator());
        alarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_UNACKNOWLEDGED.name());
        alarmAttributes.put(LAST_ALARM_OPERATION, ProcessedLastAlarmOperation.CLEAR.name());
        alarmAttributes.put(CORRELATED_EVENT_PO_ID, alarmRecord.getCorrelatedPOId());
        final Date lastUpdated = new Date();
        alarmAttributes.put(LAST_UPDATED, lastUpdated);
        return alarmAttributes;
    }

    public void updateLastDeliveredTime(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarmRecord,
            final Map<String, Object> alarmAttributes) {
        Long lastDeliveredTime = null;
        final long lastUpdatedTime = alarmRecord.getLastUpdatedTime().getTime();
        boolean clearAlarm = false;
        if (correlatedAlarmRecord.getAdditionalInformation().get(LAST_DELIVERED) != null
                && !correlatedAlarmRecord.getAdditionalInformation().get(LAST_DELIVERED).isEmpty()) {
            try {
                lastDeliveredTime = Long.parseLong(correlatedAlarmRecord.getAdditionalInformation().get(LAST_DELIVERED));
                final Long diffTimeInMilliSeconds = Math.abs(lastUpdatedTime - lastDeliveredTime);
                if (ProcessedEventState.CLEARED_ACKNOWLEDGED.name().equals(alarmRecord.getAlarmState().name())
                        || ProcessedEventState.CLEARED_UNACKNOWLEDGED.name().equals(alarmRecord.getAlarmState().name())) {
                    clearAlarm = true;
                }
                if (diffTimeInMilliSeconds < configParametersListener.getAlarmThresholdInterval()) {
                    if (clearAlarm) {
                        lastDeliveredTime = lastUpdatedTime + configParametersListener.getClearAlarmDelayToQueue();
                    } else {
                        lastDeliveredTime = lastUpdatedTime + configParametersListener.getAlarmDelayToQueue();
                    }
                } else {
                    lastDeliveredTime = lastUpdatedTime;
                }
                LOGGER.debug("lastDeliveredTime is {} lastUpdatedTime is {} diffTimeInMillSeconds is {},isClearAlarm is {} ", lastDeliveredTime,
                        lastUpdatedTime, diffTimeInMilliSeconds, clearAlarm);
                alarmRecord.getAdditionalInformation().put(LAST_DELIVERED, String.valueOf(lastDeliveredTime));
                alarmAttributes.put(LAST_DELIVERED, lastDeliveredTime);
            } catch (final NumberFormatException numberFormatException) {
                LOGGER.error("Exception occurred for LAST DELIVERED TIME returning timeToDeliver as NULL : {}", numberFormatException.getMessage());
                LOGGER.debug("Exception occurred for LAST DELIVERED TIME returning timeToDeliver as NULL : {}", numberFormatException);
                alarmRecord.getAdditionalInformation().put(LAST_DELIVERED, String.valueOf(lastUpdatedTime));
                alarmAttributes.put(LAST_DELIVERED, lastUpdatedTime);
            }
        } else {
            alarmRecord.getAdditionalInformation().put(LAST_DELIVERED, String.valueOf(lastUpdatedTime));
            alarmAttributes.put(LAST_DELIVERED, lastUpdatedTime);
        }
    }

    /**
     * AlarmRecord is built with ack information.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param attributes
     *            map containing alarm attributes.
     * @param {@link Map } attributes
     */
    private static void setAckInformation(final ProcessedAlarmEvent alarmRecord, final Map<String, Object> attributes) {
        final String ackOperator = (String) attributes.get(ACK_OPERATOR);
        final Date ackTime = (Date) attributes.get(ACK_TIME);
        if (ackOperator != null && ackTime != null) {
            alarmRecord.setAckOperator(ackOperator);
            alarmRecord.setAckTime(ackTime);
        } else {
            final Date eventTime = alarmRecord.getEventTime();
            alarmRecord.setAckOperator(APS);
            alarmRecord.setAckTime(eventTime);
            attributes.put(ACK_OPERATOR, APS);
            attributes.put(ACK_TIME, eventTime);
        }
        alarmRecord.setAlarmState(ProcessedEventState.CLEARED_ACKNOWLEDGED);
    }

    private static Map<String, Object> getAlarmAttributes(final ProcessedAlarmEvent alarmRecord) {
        final Map<String, Object> alarmAttributes = new HashMap<>();
        alarmAttributes.put(PROBLEM_TEXT, alarmRecord.getProblemText());
        alarmAttributes.put(PROBLEM_DETAIL, alarmRecord.getProblemDetail());
        alarmAttributes.put(FDN, alarmRecord.getFdn());
        alarmAttributes.put(SYNC_STATE, alarmRecord.getSyncState());
        alarmAttributes.put(ADDITIONAL_INFORMATION, alarmRecord.getAdditionalInformationString());

        if (alarmRecord.getAdditionalInformation() != null
                && !StringUtils.isEmpty(alarmRecord.getAdditionalInformation().get(TARGET_ADDITIONAL_INFORMATION))) {
            reduceTargetAdditionalInformation(alarmRecord, alarmAttributes);
        }
        alarmAttributes.put(COMMENT_TEXT, alarmRecord.getCommentText());
        alarmAttributes.put(OBJECT_OF_REFERENCE, alarmRecord.getObjectOfReference());
        alarmAttributes.put(EVENT_TYPE, alarmRecord.getEventType());
        alarmAttributes.put(EVENT_TIME, alarmRecord.getEventTime());
        alarmAttributes.put(PROBABLE_CAUSE, alarmRecord.getProbableCause());
        alarmAttributes.put(SPECIFIC_PROBLEM, alarmRecord.getSpecificProblem());
        alarmAttributes.put(BACKUP_STATUS, alarmRecord.getBackupStatus());
        alarmAttributes.put(BACKUP_OBJECT_INSTANCE, alarmRecord.getBackupObjectInstance());
        alarmAttributes.put(PROPOSED_REPAIR_ACTION, alarmRecord.getProposedRepairAction());
        alarmAttributes.put(ALARM_NUMBER, alarmRecord.getAlarmNumber());
        alarmAttributes.put(ALARM_ID, alarmRecord.getAlarmId());
        alarmAttributes.put(CEASE_TIME, alarmRecord.getCeaseTime());
        alarmAttributes.put(CEASE_OPERATOR, alarmRecord.getCeaseOperator());
        alarmAttributes.put(ACK_TIME, alarmRecord.getAckTime());
        alarmAttributes.put(ACK_OPERATOR, alarmRecord.getAckOperator());
        alarmAttributes.put(PRESENT_SEVERITY, alarmRecord.getPresentSeverity().name());
        alarmAttributes.put(PREVIOUS_SEVERITY, alarmRecord.getPreviousSeverity().name());
        alarmAttributes.put(PSEUDO_PRESENT_SEVERITY, alarmRecord.getPseudoPresentSeverity());
        alarmAttributes.put(PSEUDO_PREVIOUS_SEVERITY, alarmRecord.getPseudoPreviousSeverity());
        alarmAttributes.put(RECORD_TYPE, alarmRecord.getRecordType().name());
        alarmAttributes.put(ALARM_STATE, alarmRecord.getAlarmState().name());
        alarmAttributes.put(TREND_INDICATION, alarmRecord.getTrendIndication().name());
        alarmAttributes.put(CORRELATED_EVENT_PO_ID, alarmRecord.getCorrelatedPOId());
        alarmAttributes.put(VISIBILITY, alarmRecord.isVisibility());
        alarmAttributes.put(CORRELATEDVISIBILITY, alarmRecord.getCorrelatedVisibility());
        alarmAttributes.put(FMX_GENERATED, alarmRecord.getFmxGenerated());
        alarmAttributes.put(PROCESSING_TYPE, alarmRecord.getProcessingType());
        alarmAttributes.put(ALARMING_OBJECT, alarmRecord.getAlarmingObject());
        alarmAttributes.put(INSERT_TIME, alarmRecord.getInsertTime());
        alarmAttributes.put(LAST_UPDATED, alarmRecord.getLastUpdatedTime());
        return alarmAttributes;
    }

    public static Map<String, Object> getAlarmAttributesRoot(final ProcessedAlarmEvent alarmRecord) {
        final Map<String, Object> alarmAttributes = new HashMap<>();
        alarmAttributes.put(ADDITIONAL_INFORMATION, alarmRecord.getAdditionalInformationString());

        if (alarmRecord.getAdditionalInformation() != null
                && !StringUtils.isEmpty(alarmRecord.getAdditionalInformation().get(TARGET_ADDITIONAL_INFORMATION))) {
            reduceTargetAdditionalInformation(alarmRecord, alarmAttributes);
        }
        return alarmAttributes;
    }

    private static void reduceTargetAdditionalInformation(final ProcessedAlarmEvent alarmRecord, final Map<String, Object> alarmAttributes) {
        final TargetAdditionalInformationHandler targetAdditionalInformationHandler = new TargetAdditionalInformationHandler();
        final Map<String, String> reducedCi = targetAdditionalInformationHandler
                .reduceTargetAdditionalInformationCorrelationInformation(alarmRecord.getAdditionalInformation().get(
                        TARGET_ADDITIONAL_INFORMATION));
        if (reducedCi != null) {
            // to keep unchanged alarmRecord, replacing attributes to persist in OpenAlarm with reduced correlation Information
            final ProcessedAlarmEvent processedAlarmEvent = new ProcessedAlarmEvent();
            processedAlarmEvent.setAdditionalInformationToMap(alarmRecord.getAdditionalInformationString());
            processedAlarmEvent.getAdditionalInformation().put(TARGET_ADDITIONAL_INFORMATION, reducedCi.get(TARGET_ADDITIONAL_INFORMATION));
            alarmAttributes.put(ADDITIONAL_INFORMATION, processedAlarmEvent.getAdditionalInformationString());
            String ciFirstGroup = reducedCi.get(CI_GROUP_1);
            String ciSecondGroup = reducedCi.get(CI_GROUP_2);
            String root = reducedCi.get(ROOT);
            LOGGER.debug("reduceTargetAdditionalInformation:ciFirstGroup is {} ciSecondGroup is {} root is {} ",
                    ciFirstGroup, ciSecondGroup, root);
            
            if ((ciFirstGroup == null || ciFirstGroup.trim().isEmpty())
                    && (ciSecondGroup == null || ciSecondGroup.trim().isEmpty())) {
                root = "NOT_APPLICABLE";
                LOGGER.debug("reduceTargetAdditionalInformation: root value in condition {}", root);
            }
            alarmAttributes.put(CI_GROUP_1, ciFirstGroup);
            alarmAttributes.put(CI_GROUP_2, ciSecondGroup);
            alarmAttributes.put(ROOT, root);
        }
    }
}
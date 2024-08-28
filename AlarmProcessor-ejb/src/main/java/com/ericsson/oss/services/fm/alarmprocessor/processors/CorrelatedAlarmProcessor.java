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

import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.getAlarmAttributesRoot;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.populateUpdateAlarm;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_UPDATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmSyncStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;

/**
 * Class for processing alarms when there is a correlated alarm in db.
 */
public class CorrelatedAlarmProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelatedAlarmProcessor.class);

    @Inject
    private CorrelatedClearAlarmProcessor correlatedClearAlarmProcessor;

    @Inject
    private CorrelatedUpdateAlarmProcessor correlatedUpdateAlarmProcessor;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Inject
    private OscillationCorrelationProcessor oscillationCorrelationProcessor;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private ServiceStateModifier serviceStateModifier;

    @Inject
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Inject
    private ConfigParametersListener configParametersListener;

    /**
     * Method that processes alarms with record type {@link FMProcessedEventType#ALARM} for which there is a correlated alarm in db.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     * @return {@link AlarmProcessingResponse}
     */
    public AlarmProcessingResponse processNormalAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        AlarmProcessingResponse alarmProcessingResponse;
        // database has cleared Alarm
        if (ProcessedEventSeverity.CLEARED.equals(correlatedAlarm.getPresentSeverity())) {
            alarmProcessingResponse = correlatedClearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        } else {
            alarmProcessingResponse = correlatedUpdateAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm);
        }
        return alarmProcessingResponse;
    }

    /**
     * Method that processes alarms with record types {@link FMProcessedEventType#REPEATED_ALARM}and
     * {@link FMProcessedEventType#REPEATED_NON_SYNCHABLE} for which there is a correlated alarm in db.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     * @return {@link AlarmProcessingResponse}
     */
    public AlarmProcessingResponse processRepeated(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        AlarmProcessingResponse alarmProcessingResponse;
        if (ProcessedEventSeverity.CLEARED.equals(correlatedAlarm.getPresentSeverity())) {
            alarmProcessingResponse = correlatedClearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        } else {
            alarmProcessingResponse = correlatedUpdateAlarmProcessor.processRepeatedAlarm(alarmRecord, correlatedAlarm);
        }
        return alarmProcessingResponse;
    }

    /**
     * Method that processes alarms with record types {@link FMProcessedEventType#TECHNICIAN_PRESENT},
     * {@link FMProcessedEventType#ALARM_SUPPRESSED_ALARM}, {@link FMProcessedEventType#HEARTBEAT_ALARM},
     * {@link FMProcessedEventType#NON_SYNCHABLE_ALARM}, {@link FMProcessedEventType#HB_FAILURE_NO_SYNCH}, {@link FMProcessedEventType#OUT_OF_SYNC}
     * and {@link FMProcessedEventType#NODE_SUSPENDED} for which there is a correlated alarm in db.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     * @return {@link AlarmProcessingResponse}
     */
    public AlarmProcessingResponse processAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        if (ProcessedEventSeverity.CLEARED.equals(correlatedAlarm.getPresentSeverity())) {
            alarmProcessingResponse = correlatedClearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        } else {
            alarmProcessingResponse = correlatedUpdateAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        }
        return alarmProcessingResponse;
    }

    /**
     * Method that processes alarms with record type {@link FMProcessedEventType#CLEARALL} for which there is a correlated alarm in db.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     * @return {@link AlarmProcessingResponse}
     */
    public void processClearAllAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        if (ProcessedEventSeverity.CLEARED.equals(correlatedAlarm.getPresentSeverity())) {
            oscillationCorrelationProcessor.processAlarm(alarmRecord, correlatedAlarm);
        } else {
            updateCorrelatedAlarmAttrValuesForClearAll(alarmRecord, correlatedAlarm);
            final Map<String, Object> pOAttributes = alarmReader.readAllAttributes(alarmRecord.getCorrelatedPOId());
            final Map<String, Object> alarmAttributes = alarmAttributesPopulator.populateUpdateAlarm(alarmRecord, pOAttributes);
            alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, correlatedAlarm, alarmAttributes);
            LOGGER.debug("Updated the AlarmAttributes map : {} with the comment and visibility data. Ready to replace in the DB", alarmAttributes);
            openAlarmService.updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
            apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
        }
    }

    /**
     * Method that processes alarms with record type {@link FMProcessedEventType#SYNCHRONIZATION_ALARM} for which there is a correlated alarm in db.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     * @return {@link AlarmProcessingResponse}
     */
    public AlarmProcessingResponse processSyncAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        final ProcessedEventSeverity severity = alarmRecord.getPresentSeverity();
        alarmRecord.setCorrelatedPOId(correlatedAlarm.getEventPOId());
        alarmRecord.setEventPOId(correlatedAlarm.getEventPOId());
        if (severity.equals(correlatedAlarm.getPresentSeverity())) {
            if (NOT_SET.equals(alarmRecord.getFmxGenerated())) {
                // As mentioned in TORF-175435 (Requirement to sync the ack information from node to existing alarm in ENM.)
                final boolean isUpdated = updateAckInfoForSyncAlarms(correlatedAlarm, alarmRecord);
                // TODO :Display Status of correlated alarm
                if (!correlatedAlarm.isVisibility() && alarmRecord.isVisibility()) {
                    // if show but previously hidden
                    alarmRecord.setVisibility(true);
                    changeDisplaystatus(alarmRecord);
                    alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
                } else if (isUpdated && alarmRecord.isVisibility()) {
                    alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
                }
            }
            // Increment Instrumentation
            apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(severity);
            final Map<String, Object> alarmAttributes = getAlarmAttributesRoot(alarmRecord);
            apsInstrumentedBean.incrementAlarmRootCounters(alarmAttributes);
        } else {
            if (correlatedAlarm.getSyncState() && ProcessedEventSeverity.CLEARED.equals(correlatedAlarm.getPresentSeverity())) {
                oscillationCorrelationProcessor.processAlarm(alarmRecord, correlatedAlarm);
                alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
                clearAlarmsCacheManager.checkAndProcessForClearAlarm(alarmRecord, alarmProcessingResponse);
            } else {
                alarmRecord.setRecordType(FMProcessedEventType.REPEATED_ALARM);
                alarmRecord.setPreviousSeverity(correlatedAlarm.getPresentSeverity());
                updateAlarmInDatabase(alarmRecord, correlatedAlarm);
                alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
                apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(severity);
                final Map<String, Object> alarmAttributes = getAlarmAttributesRoot(alarmRecord);
                apsInstrumentedBean.incrementAlarmRootCounters(alarmAttributes);
            }
            // apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(severity);
        }
        openAlarmSyncStateUpdator.updateSyncStateForPoId(correlatedAlarm.getEventPOId(), true);
        return alarmProcessingResponse;
    }

    /**
     * Method updates ProcessedAlarmEvent with correlatedAlarm's alarmId, alarmnumber, severity, EventPOId, CommentText. Called in case of clearAll
     * alarm.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     */
    private void updateCorrelatedAlarmAttrValuesForClearAll(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        final Date lastUpdated = new Date();
        alarmRecord.setAlarmId(correlatedAlarm.getAlarmId());
        alarmRecord.setAlarmNumber(correlatedAlarm.getAlarmNumber());
        alarmRecord.setPreviousSeverity(correlatedAlarm.getPresentSeverity());
        alarmRecord.setCorrelatedPOId(correlatedAlarm.getEventPOId());
        alarmRecord.setCommentText(correlatedAlarm.getCommentText());
        alarmRecord.setLastUpdatedTime(lastUpdated);
        alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.CHANGE);
        alarmRecord.setEventPOId(alarmRecord.getCorrelatedPOId());
    }

    /**
     * Method gets correlated alarm's visibility from database and sets the same in received alarm.
     * @param {@link ProcessedAlarmEvent} alarmEvent The alarm for change in display status.
     */
    private void changeDisplaystatus(final ProcessedAlarmEvent alarmEvent) {
        if (alarmEvent.getCorrelatedPOId() > 0) {
            // Find PO by CorrelatedId and update the status
            final Object visibility = alarmReader.readAttribute(alarmEvent.getCorrelatedPOId(), VISIBILITY);
            if (visibility != null) {
                alarmEvent.setVisibility((boolean) visibility);
            }
        }
    }

    /**
     * Method for replacing alarm in database with received alarm whose record type is SYNCHRONIZATION_ALARM, ERROR_MESSAGE, REPEATED_ERROR_MESSAGE.
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    private void updateAlarmInDatabase(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        setDataForReplacingAlarm(alarmRecord);
        final Map<String, Object> pOAttributes = alarmReader.readAllAttributes(alarmRecord.getCorrelatedPOId());
        final Map<String, Object> alarmAttributes = alarmAttributesPopulator.populateUpdateAlarm(alarmRecord, pOAttributes);
        alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, correlatedAlarm, alarmAttributes);
        openAlarmService.updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
    }

    /**
     * Method for setting ProcessedAlarmEvent's lastUpdatedTime, LastAlarmOperation and updates service state and record type based on SP.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    private void setDataForReplacingAlarm(final ProcessedAlarmEvent alarmRecord) {
        final Date lastUpdated = new Date();
        alarmRecord.setLastUpdatedTime(lastUpdated);
        alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.CHANGE);
        if (!(FMProcessedEventType.TECHNICIAN_PRESENT.equals(alarmRecord.getRecordType()) || FMProcessedEventType.ALARM_SUPPRESSED_ALARM
                        .equals(alarmRecord.getRecordType()))) {
            serviceStateModifier.updateFmFunctionBasedOnSpecificProblem(alarmRecord);
        }
    }

    /**
     * processSyncReplaceAlarm in case of a non-repeated alarm.
     * @param ProcessedAlarmEvent alarmRecord
     * @param ProcessedAlarmEvent correlatedAlarm
     * @return AlarmProcessingResponse
     */
    public AlarmProcessingResponse processSyncReplaceAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        final ProcessedEventSeverity severity = alarmRecord.getPresentSeverity();
        alarmRecord.setCorrelatedPOId(correlatedAlarm.getEventPOId());
        alarmRecord.setPreviousSeverity(correlatedAlarm.getPresentSeverity());
        alarmRecord.setRepeatCount(correlatedAlarm.getRepeatCount());
        final Date date = new Date();
        alarmRecord.setLastUpdatedTime(date);
        alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.CHANGE);
        serviceStateModifier.updateFmFunctionBasedOnSpecificProblem(alarmRecord);
        final Date oldInsertTime = correlatedAlarm.getInsertTime();
        setInsertTime(alarmRecord, oldInsertTime);
        final Map<String, Object> alarmAttributes = populateUpdateAlarm(alarmRecord);
        alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, correlatedAlarm, alarmAttributes);
        openAlarmService.updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
        alarmRecord.setEventPOId(alarmRecord.getCorrelatedPOId());
        alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
        apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(severity);
        apsInstrumentedBean.incrementAlarmRootCounters(alarmAttributes);
        return alarmProcessingResponse;
    }

    /**
     * Update the Alarm InsertTime in ProcessedAlarmEvent based on the Update Configuration Parameter.
     * @param processedAlarmEvent alarm
     * @param Date oldInsertime
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
     * Method updates ack information to correlated alarm, if alarm state of alarm coming from node is ACTIVE_ACKNOWLEDGED. (TORF-175435)
     * @param correlatedAlarm correlated alarm.
     * @param syncAlarm alarm coming from node.
     */
    private boolean updateAckInfoForSyncAlarms(final ProcessedAlarmEvent correlatedAlarm, final ProcessedAlarmEvent syncAlarm) {
        if (ProcessedEventState.ACTIVE_ACKNOWLEDGED.equals(syncAlarm.getAlarmState())
                        && !ProcessedEventState.ACTIVE_ACKNOWLEDGED.equals(correlatedAlarm.getAlarmState())) {
            final Map<String, Object> attributesToBeUpdated = alarmReader.readAllAttributes(correlatedAlarm.getEventPOId());
            if (attributesToBeUpdated != null) {
                LOGGER.debug("Updating Ack information to align ENM with NODE... CorrelatedAlarm ::: {} and incoming Alarm Record ::: {}",
                                correlatedAlarm, syncAlarm);
                syncAlarm.setEventPOId(correlatedAlarm.getEventPOId());
                syncAlarm.setInsertTime(correlatedAlarm.getInsertTime());
                syncAlarm.setLastUpdatedTime(new Date());
                attributesToBeUpdated.put(ALARM_STATE, syncAlarm.getAlarmState().toString());
                attributesToBeUpdated.put(ACK_OPERATOR, syncAlarm.getAckOperator());
                attributesToBeUpdated.put(ACK_TIME, syncAlarm.getAckTime());
                attributesToBeUpdated.put(RECORD_TYPE, syncAlarm.getRecordType().toString());
                attributesToBeUpdated.put(LAST_UPDATED, syncAlarm.getLastUpdatedTime());
                openAlarmService.updateAlarm(correlatedAlarm.getEventPOId(), attributesToBeUpdated);
                return true;
            }
        }
        return false;
    }
}

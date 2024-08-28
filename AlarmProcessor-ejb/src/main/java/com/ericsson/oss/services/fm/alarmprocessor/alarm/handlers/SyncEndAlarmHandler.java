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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARMSUPPRESSED_SP;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ORIGINAL_RECORD_TYPE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.TECHNICIANPRESENT_SP;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.populateHiddenAlarm;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.getManagedObject;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATED_EVENT_PO_ID;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_ALARM_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_UPDATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.MANUALCEASE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_DETAIL;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PSEUDO_PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PSEUDO_PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.SYNC_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.SPECIFIC_PROBLEM;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;

import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmSyncStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.common.builder.ProcessedAlarmEventBuilder;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;
import com.ericsson.oss.services.models.alarm.AlarmRecordType;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

/**
 * Class handles alarm with RecordType SYNCHRONIZATION_ENDED. Alarms with syncState attribute set to false corresponding to the Network Element for
 * which this alarm is received are ceased as these alarms do not exist on node anymore. CurrentServiceState,an attribute of FmFunction ManagedObject
 * is set to IN_SERVICE from SYNC_ONGOING.
 */
public class SyncEndAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncEndAlarmHandler.class);

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    @Inject
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    /**
     * Method handles alarm with RecordType {@link FMProcessedEventType#SYNCHRONIZATION_ENDED}. CurrentServiceState for a NetworkElement should be in
     * SYNC_ONGOING to process Synchronization_End alarm .
     * <p>
     * All open alarms for the corresponding NetworkElement whose syncState,an attribute of OpenAlarm PO is set to false are ceased and
     * CurrentServiceState is updated to IN_SERVICE.
     * <p>
     * CurrentServiceState, an attribute of FmFunction ManagedObject is updated with null to mark successful completion of processing synchronization
     * alarms.
     * @param inputEvent
     *            {@link ProcessedAlarmEvent}
     * @return {@link AlarmProcessingResponse} alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        LOGGER.debug("Alarm received to SyncEndAlarmHandler: {}", alarmRecord);
        final String fdn = alarmRecord.getFdn();
        final String currentState = (String) fmFunctionMoService.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE);
        if (FmSyncStatus100.SYNC_ONGOING.name().equalsIgnoreCase(currentState)) {
            alarmProcessingResponse = checkForUncorrelatedAlarms(fdn);
            fmFunctionMoService.updateCurrentServiceState(alarmRecord.getFdn(), FmSyncStatus100.IN_SERVICE.name());
            systemRecorder.recordEvent("APS", EventLevel.DETAILED, "currentServiceState", "is changed to IN_SERVICE from SYNC_ON_GOING for fdn", fdn);
        } else {
            openAlarmSyncStateUpdator.updateSyncState(fdn, true);
            systemRecorder.recordEvent("APS Discarding the Sync as current service is ", EventLevel.DETAILED, currentState, " for the node ", fdn);
        }
        apsInstrumentedBean.incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
        return alarmProcessingResponse;
    }

    /**
     * Generates Cease alarms for alarms left over as unmatched in alarm synchronization .
     * <p>
     * Clear alarm is generated with attributes similar to alarm already present in database .
     * @param String
     *            fdn
     * @return {@link AlarmProcessingResponse} alarmProcessingResponse
     */
    private AlarmProcessingResponse checkForUncorrelatedAlarms(final String fdn) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        final Iterator<PersistenceObject> poIterator = alarmReader.readAlarms(fdn, true, false);
        while (poIterator != null && poIterator.hasNext()) {
            ProcessedAlarmEvent alarmRecord = null;
            final PersistenceObject alarmFromDataBase = poIterator.next();
            final Map<String, Object> alarmAttributes = alarmFromDataBase.getAllAttributes();
            // Here as we are not updating syncState for NON_SYNCHABLE_ALARM to true again.
            // So syncState of this NonSynchable/RepeatedNonSynchable alarm
            // would always be false after this.
            if (alarmAttributes.get(ADDITIONAL_INFORMATION) != null
                    && !((String) alarmAttributes.get(ADDITIONAL_INFORMATION))
                            .contains(ORIGINAL_RECORD_TYPE + ":" + FMProcessedEventType.NON_SYNCHABLE_ALARM.name())) {
                updateAlarmAttrForUncorrelatedAlarms(alarmFromDataBase.getPoId(), alarmAttributes);
                alarmRecord = ProcessedAlarmEventBuilder.buildProcessedAlarm(alarmAttributes);
                alarmRecord.setEventPOId(alarmFromDataBase.getPoId());
                alarmRecord.setManagedObject(getManagedObject(alarmRecord.getObjectOfReference()));
                LOGGER.debug("Clear Alarm generated for uncorrelated Alarm during sync: {} ", alarmRecord);
                if (!alarmRecord.getVisibility()) {
                    final Map<String, Object> hiddenAlarmAttributes = populateHiddenAlarm(alarmRecord, alarmAttributes);
                    alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, alarmRecord, hiddenAlarmAttributes);
                    // Clear generated on a FMX Hidden Alarm.This alarm should be deleted from database.
                    openAlarmService.removeAlarm(alarmRecord.getCorrelatedPOId(), hiddenAlarmAttributes);
                    apsInstrumentedBean.incrementDeletedShortLivedAlarmsCount();
                    LOGGER.debug("Hidden alarm : {} removed from the list as part of alarm synchronization", alarmRecord);
                } else {
                    alarmAttributes.put(PSEUDO_PRESENT_SEVERITY, alarmRecord.getPseudoPresentSeverity());
                    alarmAttributes.put(PSEUDO_PREVIOUS_SEVERITY, alarmRecord.getPseudoPreviousSeverity());
                    alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, alarmRecord, alarmAttributes);
                    if (alarmRecord.getAlarmState().equals(ProcessedEventState.ACTIVE_ACKNOWLEDGED)) {
                        alarmRecord.setAlarmState(ProcessedEventState.CLEARED_ACKNOWLEDGED);
                        alarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_ACKNOWLEDGED.name());
                        openAlarmService.removeAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
                    } else if (alarmRecord.getAlarmState().equals(ProcessedEventState.ACTIVE_UNACKNOWLEDGED)) {
                        alarmRecord.setAlarmState(ProcessedEventState.CLEARED_UNACKNOWLEDGED);
                        alarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_UNACKNOWLEDGED.name());
                        LOGGER.debug("Attributes to be set for PO Id {} are {}", alarmRecord.getCorrelatedPOId(), alarmAttributes);
                        openAlarmService.updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
                    }
                }
                alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
            }
        }
        LOGGER.info("Number of Alarms Cleared for NetworkElement {} is: {} ", fdn, alarmProcessingResponse.getProcessedAlarms().size());
        return alarmProcessingResponse;
    }

    /**
     * Method builds clear alarm for uncorrelated alarm received in sync.
     * @param eventPoId
     *            {@link Long}
     * @param alarmAttributes
     *            {@link Map}
     */
    private void updateAlarmAttrForUncorrelatedAlarms(final Long eventPoId, final Map<String, Object> alarmAttributes) {
        alarmAttributes.put(PREVIOUS_SEVERITY, alarmAttributes.get(PRESENT_SEVERITY));
        alarmAttributes.put(PRESENT_SEVERITY, ProcessedEventSeverity.CLEARED.name());

        if (!FMProcessedEventType.HEARTBEAT_ALARM.name().equals(alarmAttributes.get(RECORD_TYPE)) && !FMProcessedEventType.ALARM_SUPPRESSED_ALARM.name().equals(alarmAttributes.get(RECORD_TYPE)) && !FMProcessedEventType.TECHNICIAN_PRESENT.name().equals(alarmAttributes.get(RECORD_TYPE))) {
                alarmAttributes.put(RECORD_TYPE, FMProcessedEventType.SYNCHRONIZATION_ALARM.name());
        }

        if (AlarmRecordType.ALARM_SUPPRESSED_ALARM.name().equalsIgnoreCase((String) alarmAttributes.get(RECORD_TYPE))
                || ALARMSUPPRESSED_SP.equals(alarmAttributes.get(SPECIFIC_PROBLEM))) {
            fmFunctionMoService.update((String)alarmAttributes.get(FDN), AlarmProcessorConstants.ALARM_SUPPRESSED_STATE, false);
        } else if (AlarmRecordType.TECHNICIAN_PRESENT.name().equalsIgnoreCase((String)alarmAttributes.get(RECORD_TYPE))
                || TECHNICIANPRESENT_SP.equals(alarmAttributes.get(SPECIFIC_PROBLEM))) {
            fmFunctionMoService.update((String)alarmAttributes.get(FDN), AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE, false);
        }

        alarmAttributes.put(PROBLEM_DETAIL, AlarmProcessorConstants.CLEAR_UNCORRELATED_PROBLEM_TEXT);
        alarmAttributes.put(SYNC_STATE, true);
        alarmAttributes.put(LAST_ALARM_OPERATION, ProcessedLastAlarmOperation.CLEAR.name());
        alarmAttributes.put(CEASE_OPERATOR, AlarmProcessorConstants.APS_SERVICE_ID);
        final Date lastUpdated = new Date();
        alarmAttributes.put(LAST_UPDATED, lastUpdated);
        alarmAttributes.put(CEASE_TIME, lastUpdated);
        alarmAttributes.put(MANUALCEASE, true);
        alarmAttributes.put(CORRELATED_EVENT_PO_ID, eventPoId);
    }
}

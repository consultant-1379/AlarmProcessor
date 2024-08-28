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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CATEGORY;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.SOURCETYPE;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.getAlarmAttributesRoot;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.populateNewAlarm;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.fm.capability.util.ModelCapabilities;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmSyncStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.ServiceProxyProviderBean;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

/**
 * Class handles alarm with RecordType SYNCHRONIZATION_ALARM. Alarm Correlation is performed for the received Synchronization_Alarm. If correlated
 * alarm is found it means the alarm is already present in database .So just syncState attribute of alarm is updated to true. Correlated alarm is not
 * found then the synchronization alarm is inserted in Database as a new Alarm received in Synchronization with RecordType SYNCHRONIZATION_ALARM.
 */
public class SyncAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncAlarmHandler.class);

    @Inject
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Inject
    private ServiceStateModifier serviceStateModifier;

    @Inject
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Inject
    private AlarmCorrelator correlator;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    @Inject
    private AlarmValidator alarmValidator;

    @Inject
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Inject
    private ModelCapabilities modelCapabilities;

    /**
     * Handle handles alarm with RecordType SYNCHRONIZATION_ALARM Synchronization alarms are processed only if the current service state,an
     * attribute of FmFunction ManagedObject is Sync_Ongoing. If the CurrentServiceState is not SYNC_ONGOING then synchronization alarm is not
     * processed. As part of Synchronization alarm processing , alarm correlation is performed .If the correlated alarm is found and severity of the
     * alarm is same it means the alarm is already present in the database ."syncState" attribute for this openAlarm is updated back to true. If
     * correlated alarm is not found alarm is inserted in database.
     * @param {@link ProcessedAlarmEvent} --alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        LOGGER.debug("Alarm received to SyncAlarmHandler : {}", alarmRecord);
        final String fdn = alarmRecord.getFdn();
        final ProcessedEventSeverity severity = alarmRecord.getPresentSeverity();
        final String currentState = (String) fmFunctionMoService.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE);
        final boolean multiLevelHandling =
                modelCapabilities.isMultiLevelCssHandlingRequired(CATEGORY, alarmRecord.getAdditionalInformation().get(SOURCETYPE));
        if (FmSyncStatus100.SYNC_ONGOING.name().equals(currentState) || multiLevelHandling) {
            if (!severity.equals(ProcessedEventSeverity.CLEARED)) {
                final ProcessedAlarmEvent correlatedAlarm = correlator.getCorrelatedAlarm(alarmRecord);
                // Correlation Alarm not found in database.
                if (correlatedAlarm.getEventPOId().equals(AlarmProcessorConstants.DEFAULT_EVENTPOID_VALUE)
                        || correlatedAlarm.getEventPOId().equals(AlarmProcessorConstants.ALREC_NO_VAL)) {
                    insertNewAlarmReceivedInSync(alarmRecord);
                    apsInstrumentedBean.incrementNewlyProcessedAlarmCount(severity);
                    alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
                    // Check if there exists any Clear alarm for this newly Inserted alarm
                    clearAlarmsCacheManager.checkAndProcessForClearAlarm(alarmRecord, alarmProcessingResponse);
                } else if (correlatedAlarm.getEventPOId() > 0) {
                    alarmProcessingResponse = validateAndProcess(alarmRecord, correlatedAlarm);
                }
            } else {
                LOGGER.debug("Discarded received alarm in synchronization with severity CLEARED.{}", alarmRecord);
                apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
            }
        } else {
            LOGGER.debug("Received Sync Alarm is out of proper order.Discarding it: {}", alarmRecord);
            apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
        }
        return alarmProcessingResponse;
    }

    private AlarmProcessingResponse validateAndProcess(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        // Correlation alarm found in database.
        if (alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)) {
            // Correlation alarm found in database is an FMX Updated alarm.
            // As per TORF-151875, no Sync alarm should replace the existing FMX Update alarm unless it is a CLEARED alarm.
            if (!NOT_SET.equals(alarmRecord.getProcessingType()) && !NOT_SET.equals(correlatedAlarm.getProcessingType())) {
                if (ProcessedEventSeverity.CLEARED.equals(correlatedAlarm.getPresentSeverity())) {
                    alarmProcessingResponse = correlatedAlarmProcessor.processSyncAlarm(alarmRecord, correlatedAlarm);
                } else {
                    correlateAlarmBySeverity(alarmRecord, correlatedAlarm);
                    openAlarmSyncStateUpdator.updateSyncStateForPoId(correlatedAlarm.getEventPOId(), true);
                }
            } else {
                if (!NOT_SET.equals(correlatedAlarm.getProcessingType())) {
                    LOGGER.debug("Sync Alarm {} is not matched with FMX subscription but correlated with an FMX Processed alarm", alarmRecord);
                    // Non-Repeated alarm
                    // TORF-247231:It is said that the FMX processed alarm is to be replaced with incoming alarm.
                    alarmProcessingResponse = correlatedAlarmProcessor.processSyncReplaceAlarm(alarmRecord, correlatedAlarm);
                } else {
                    alarmProcessingResponse = correlatedAlarmProcessor.processSyncAlarm(alarmRecord, correlatedAlarm);
                }
            }
        } else {
            LOGGER.debug("Received Synchronization alarm may not be latest.Discarding it.{}", alarmRecord);
            openAlarmSyncStateUpdator.updateSyncStateForPoId(correlatedAlarm.getEventPOId(), true);
        }
        return alarmProcessingResponse;
    }
    
    private void correlateAlarmBySeverity(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        if (!alarmRecord.getPresentSeverity().equals(correlatedAlarm.getPresentSeverity())) {
            LOGGER.debug("Forwarding received SyncAlarm {} to FMX only, as it is correlated to an FMX Updated alarm {} "
                    + "as there is an update in severity", alarmRecord, correlatedAlarm);
            serviceProxyProviderBean.getAlarmSender().sendAlarm(alarmRecord);
            apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
            final Map<String, Object> alarmAttributes = getAlarmAttributesRoot(alarmRecord);
            apsInstrumentedBean.incrementAlarmRootCounters(alarmAttributes);
        } else {
            LOGGER.debug("Discarded received SyncAlarm {} that is correlated to an FMX Updated alarm {}", alarmRecord, correlatedAlarm);
            apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
            final Map<String, Object> alarmAttributes = getAlarmAttributesRoot(alarmRecord);
            apsInstrumentedBean.incrementAlarmRootCounters(alarmAttributes);
        }
    }
    

    /**
     * Insert Alarm received as part of alarm synchronization. AlarmSuppressedState,TechnicianPresent attribute of FmFunction ManagedObject is
     * updated for alarms with these RecordTypes.
     * @param {@link ProcessedAlarmEvent} --alarmRecord
     */
    private void insertNewAlarmReceivedInSync(final ProcessedAlarmEvent alarmRecord) {
        serviceStateModifier.updateFmFunctionBasedOnSpecificProblem(alarmRecord);
        final Map<String, Object> alarmAtributes = populateNewAlarm(alarmRecord);
        alarmRecord.setEventPOId(openAlarmService.insertAlarmRecord(alarmAtributes));
        alarmRecord.setActionState(ProcessedLastAlarmOperation.NEW);
        apsInstrumentedBean.incrementAlarmRootCounters(alarmAtributes);
        LOGGER.debug("Alarm inserted in Database as it is a new alarm received in sync.{} ", alarmRecord);
    }
}

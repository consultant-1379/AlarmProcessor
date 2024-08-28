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

import static com.ericsson.oss.itpf.sdk.recording.ErrorSeverity.CRITICAL;
import static com.ericsson.oss.itpf.sdk.recording.EventLevel.DETAILED;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;
import static com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100.IDLE;
import static com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100.SYNCHRONIZATION;
import static com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100.SYNC_ONGOING;
import static com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100.HEART_BEAT_FAILURE;
import com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CLEARED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmSyncStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class handles alarms with RecordType SYNCHRONIZATION_STARTED. SyncState,an attribute of alarm is updated to false for all alarms in database whose
 * fdn matches the value of fdn in the received alarm. CurrentServiceState, an attribute of FmFunction ManagedObject is updated with current date to
 * SYNC_ONGOING if the value is IN_SERVICE/OUT_OF_SYNC.
 */
public class SyncStartAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncStartAlarmHandler.class);

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    @Inject
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private CurrentServiceStateUpdator currentServiceStateUpdator;

    /**
     * Method handles alarms with RecordType SYNCHRONIZATION_STARTED CurrentServiceState for a NetworkElement should be in IN_SERVICE('IDLE' only
     * in the case of Database migration) to process Synchronization_Start alarm .
     * <p>
     * All open alarms for the corresponding NetworkElement are marked i.e.,syncState,an attribute of OpenAlarm PO is set to false and
     * CurrentServiceState is updated to Sync_Ongoing.
     * <p>
     * lastUpdatedTimeStamp attribute of FmFunction ManagedObject is updated to mark the time stamp of Synchronization.
     *
     * @param {@link ProcessedAlarmEvent} --alarmRecord
     */
    //Note:If required we may need to acquire lock during update of syncState attribute for each alarm.
    //Can be added whenever an issue is seen.
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        LOGGER.debug("Alarm received to SyncStartAlarmHandler: {}", alarmRecord);
        final String fdn = alarmRecord.getFdn();
        final String currentServiceState = (String) fmFunctionMoService.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE);
        if (currentServiceState != null) {
                 if (IDLE.name().equals(currentServiceState) && configParametersListener.isMigrationOnGoing()) {
                     openAlarmSyncStateUpdator.updateSyncState(fdn, false);
                     fmFunctionMoService.updateCurrentServiceState(fdn, SYNC_ONGOING.name());
                     systemRecorder.recordEvent("APS", DETAILED, "DB migration ongoing, so currentServiceState",
                             "is changed to SYNC_ONGOING from IDLE for fdn", fdn);
                 } else if (!SYNC_ONGOING.name().equals(currentServiceState)
                         && !SYNCHRONIZATION.name().equals(currentServiceState)
                         && !IDLE.name().equals(currentServiceState)) {
                     if(HEART_BEAT_FAILURE.name().equals(currentServiceState)){
                         currentServiceStateUpdator.sendHearbeatStateRequest(CLEARED, fdn);
                         LOGGER.debug("MTR sent to clear HEARBEAT alarm for node {}",fdn);
                     }
                     openAlarmSyncStateUpdator.updateSyncState(fdn, false);
                     fmFunctionMoService.updateCurrentServiceState(fdn, SYNC_ONGOING.name());
                     systemRecorder.recordEvent("APS", DETAILED, "currentServiceState", "is changed to SYNC_ONGOING from "
                             + currentServiceState + " for fdn ", fdn);
                 } else {
                     LOGGER.info("currentServiceState should not be in SYNC_ONGOING or SYNCHRONIZATION or IDLE state to process sync alarms for fdn : {}."
                               + "Value returned is : {}", fdn, currentServiceState);
                 }
        } else {
            systemRecorder
                    .recordError("APS", CRITICAL, "currentServiceState", "is null for fdn.Sync alarms will not be processed", fdn);
        }
        apsInstrumentedBean.incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
        return alarmProcessingResponse;
    }
}
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

import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

/**
 * Class handles alarms with record type SYNCHRONIZATION_ABORTED.
 */
public class SyncAbortAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncAbortAlarmHandler.class);

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private ErrorAlarmHandler errorAlarmHandler;

    /**
     * Method handles alarms with record type SYNCHRONIZATION_ABORTED. Alarm record type is changed to ERROR_MESSAGE. CurrentServiceState is changed
     * accordingly. Further processed like error message.
     *
     * @param {@link ProcessedAlarmEvent} --alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Error Event received to SyncAbortAlarmHandler : {}", alarmRecord);
        final String fdn = alarmRecord.getFdn();
        alarmRecord.setRecordType(FMProcessedEventType.ERROR_MESSAGE);
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.INDETERMINATE);
        final String currentState = (String) fmFunctionMoService.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE);
        if (!(FmSyncStatus100.HEART_BEAT_FAILURE.name().equalsIgnoreCase(currentState))
                && !(FmSyncStatus100.OUT_OF_SYNC.name().equalsIgnoreCase(currentState))) {
            fmFunctionMoService.updateCurrentServiceState(fdn, FmSyncStatus100.IN_SERVICE.name());
            systemRecorder.recordEvent("APS", EventLevel.DETAILED, "currentServiceState",
                    "is changed to IN_SERVICE from SYNCHRONIZATION_ABORTED for fdn", fdn);
        }
        return errorAlarmHandler.handleAlarm(alarmRecord);
    }
}
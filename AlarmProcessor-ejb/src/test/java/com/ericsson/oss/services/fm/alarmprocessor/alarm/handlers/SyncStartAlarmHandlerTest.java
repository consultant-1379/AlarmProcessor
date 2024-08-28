/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmSyncStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

@RunWith(MockitoJUnitRunner.class)
public class SyncStartAlarmHandlerTest {

    @InjectMocks
    private SyncStartAlarmHandler syncStartAlarmHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private FmFunctionMoService fmFunctionMOFacade;

    @Mock
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Test
    public void testHandleAlarm_syncInService() {
        alarmRecord = new ProcessedAlarmEvent();

        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ALARM);
        alarmRecord.setFdn("NetowrkElement");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        when(fmFunctionMOFacade.read(alarmRecord.getFdn(), FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.IN_SERVICE.name());
        syncStartAlarmHandler.handleAlarm(alarmRecord);
        verify(fmFunctionMOFacade).updateCurrentServiceState(alarmRecord.getFdn(), FmSyncStatus100.SYNC_ONGOING.name());
        verify(openAlarmSyncStateUpdator).updateSyncState(alarmRecord.getFdn(), false);
        verify(systemRecorder).recordEvent("APS", EventLevel.DETAILED, "currentServiceState",
                "is changed to SYNC_ONGOING from IN_SERVICE for fdn ", alarmRecord.getFdn());

    }

    @Test
    public void testHandleAlarm_ServiceStateNull() {

        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ALARM);
        alarmRecord.setFdn("NetowrkElement");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        when(fmFunctionMOFacade.read(alarmRecord.getFdn(), FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(null);
        syncStartAlarmHandler.handleAlarm(alarmRecord);
        verify(systemRecorder).recordError("APS", ErrorSeverity.CRITICAL, "currentServiceState", "is null for fdn.Sync alarms will not be processed",
                alarmRecord.getFdn());

    }

}

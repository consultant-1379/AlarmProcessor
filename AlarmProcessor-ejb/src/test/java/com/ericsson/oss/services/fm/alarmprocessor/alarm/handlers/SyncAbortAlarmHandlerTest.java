// /*------------------------------------------------------------------------------
// *******************************************************************************
// * COPYRIGHT Ericsson 2016
// *
// * The copyright to the computer program(s) herein is the property of
// * Ericsson Inc. The programs may be used and/or copied only with written
// * permission from Ericsson Inc. or in accordance with the terms and
// * conditions stipulated in the agreement/contract under which the
// * program(s) have been supplied.
// *******************************************************************************
// *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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
public class SyncAbortAlarmHandlerTest {

    @InjectMocks
    private SyncAbortAlarmHandler syncAbortAlarmHandler;

    @Mock
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Mock
    private ErrorAlarmHandler errorAlarmHandler;

    @Mock
    private FmFunctionMoService fmFunctionMOFacade;

    @Mock
    private ErrorAlarmCommonHandler errorAlarmCommonHandler;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private SystemRecorder systemRecorder;

    @Test
    public void test_Handle_SyncAbort() {
        final String fdn = "NetworkElement=1";
        final ProcessedAlarmEvent syncAbort = new ProcessedAlarmEvent();
        syncAbort.setFdn(fdn);
        syncAbort.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ABORTED);
        syncAbort.setPresentSeverity(ProcessedEventSeverity.INDETERMINATE);
        when(fmFunctionMOFacade.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.SYNC_ONGOING.name());
        syncAbortAlarmHandler.handleAlarm(syncAbort);
        verify(fmFunctionMOFacade, times(1)).updateCurrentServiceState(fdn, FmSyncStatus100.IN_SERVICE.name());
        verify(systemRecorder).recordEvent("APS", EventLevel.DETAILED, "currentServiceState",
                "is changed to IN_SERVICE from SYNCHRONIZATION_ABORTED for fdn", fdn);
    }
}

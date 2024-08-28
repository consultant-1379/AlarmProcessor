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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;

import java.util.HashMap;
import java.util.Iterator;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmSyncStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

@RunWith(MockitoJUnitRunner.class)
public class SyncEndAlarmHandlerTest {

    @InjectMocks
    private SyncEndAlarmHandler syncEndAlarmHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private FmFunctionMoService fmFunctionMOFacade;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Test
    public void testHandleAlarm_SyncOngoing() {

        alarmRecord = new ProcessedAlarmEvent();
        final Iterator<PersistenceObject> poIterator = null;
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ALARM);
        alarmRecord.setFdn("NetowrkElement");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        when(fmFunctionMOFacade.read(alarmRecord.getFdn(), FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.SYNC_ONGOING.name());
        when(alarmReader.readAlarms(alarmRecord.getFdn(), true, false)).thenReturn(poIterator);
        syncEndAlarmHandler.handleAlarm(alarmRecord);
        verify(fmFunctionMOFacade).updateCurrentServiceState(alarmRecord.getFdn(), FmSyncStatus100.IN_SERVICE.name());
        verify(apsInstrumentedBean).incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
        verify(systemRecorder).recordEvent("APS", EventLevel.DETAILED, "currentServiceState", "is changed to IN_SERVICE from SYNC_ON_GOING for fdn",
                "NetowrkElement");
    }

    @Test
    public void testHandleAlarm() {

        alarmRecord = new ProcessedAlarmEvent();
        new HashMap<String, Object>();
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ALARM);
        alarmRecord.setFdn("NetowrkElement");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        when(fmFunctionMOFacade.read(alarmRecord.getFdn(), FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.IDLE.name());
        syncEndAlarmHandler.handleAlarm(alarmRecord);
        verify(apsInstrumentedBean).incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());

    }

}

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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_PROCESSED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.fm.capability.util.ModelCapabilities;
import com.ericsson.oss.services.fm.alarmprocessor.api.alarmsender.AlarmSender;
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
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

@RunWith(MockitoJUnitRunner.class)
public class SyncAlarmHandlerTest {

    @InjectMocks
    private SyncAlarmHandler syncAlarmHandler;

    @Mock
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private FmFunctionMoService fmFunctionMOFacade;

    @Mock
    private AlarmCorrelator correlator;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Mock
    private ServiceStateModifier serviceStateModifier;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private AlarmValidator alarmValidator;

    @Mock
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Mock
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Mock
    private ModelCapabilities modelCapabilities;

    @Mock
    private AlarmSender alarmSender;

    @Test
    public void testNewAlarmInSync_whenCorrelatedAlarm_Null() {
        alarmRecord = new ProcessedAlarmEvent();
        new HashMap<String, Object>();
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ALARM);
        alarmRecord.setFdn("NetowrkElement");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.INDETERMINATE);
        alarmRecord.setSpecificProblem("test");
        when(fmFunctionMOFacade.read(alarmRecord.getFdn(), FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.SYNC_ONGOING.name());
        when(correlator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(-2L);
        syncAlarmHandler.handleAlarm(alarmRecord);
        verify(serviceStateModifier).updateFmFunctionBasedOnSpecificProblem(alarmRecord);
        verify(openAlarmService).insertAlarmRecord((Map<String, Object>) anyObject());
        verify(apsInstrumentedBean).incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
        verify(apsInstrumentedBean).incrementAlarmRootCounters((Map<String, Object>) anyObject());

    }

    @Test
    public void testNewAlarmInSync_whenCorrelatedAlarm_NotNull() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ALARM);
        alarmRecord.setFdn("NetowrkElement");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.INDETERMINATE);
        alarmRecord.setSpecificProblem("test");
        alarmRecord.setProcessingType(FMX_PROCESSED);
        when(fmFunctionMOFacade.read(alarmRecord.getFdn(), FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.SYNC_ONGOING.name());
        when(correlator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(1234L);
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.MAJOR);
        when(alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)).thenReturn(true);
        when(serviceProxyProviderBean.getAlarmSender()).thenReturn(alarmSender);
        syncAlarmHandler.handleAlarm(alarmRecord);
        verify(openAlarmSyncStateUpdator).updateSyncStateForPoId(correlatedAlarm.getEventPOId(), true);
        verify(apsInstrumentedBean).incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
        verify(apsInstrumentedBean).incrementAlarmRootCounters((Map<String, Object>) anyObject());

        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        syncAlarmHandler.handleAlarm(alarmRecord);
        verify(correlatedAlarmProcessor).processSyncAlarm(alarmRecord, correlatedAlarm);
        verify(openAlarmSyncStateUpdator).updateSyncStateForPoId(correlatedAlarm.getEventPOId(), true);
        verify(apsInstrumentedBean).incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
        verify(apsInstrumentedBean).incrementAlarmRootCounters((Map<String, Object>) anyObject());
    }

    @Test
    public void testhandleAlarm_ProcessingType_Notset() {
        alarmRecord = new ProcessedAlarmEvent();
        new HashMap<String, Object>();
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ALARM);
        alarmRecord.setFdn("NetowrkElement");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.INDETERMINATE);
        alarmRecord.setSpecificProblem("test");
        alarmRecord.setProcessingType(NOT_SET);
        alarmRecord.getFdn();
        when(fmFunctionMOFacade.read(alarmRecord.getFdn(), FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.SYNC_ONGOING.name());
        when(correlator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(1234L);
        when(correlatedAlarm.getProcessingType()).thenReturn(NOT_SET);
        when(alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)).thenReturn(true);
        when(correlatedAlarmProcessor.processSyncAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        syncAlarmHandler.handleAlarm(alarmRecord);
        verify(correlatedAlarmProcessor).processSyncAlarm(alarmRecord, correlatedAlarm);

    }

}

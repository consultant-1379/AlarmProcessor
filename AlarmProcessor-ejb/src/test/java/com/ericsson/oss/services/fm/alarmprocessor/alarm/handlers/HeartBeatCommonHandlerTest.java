// ///*------------------------------------------------------------------------------
// // *******************************************************************************
// // * COPYRIGHT Ericsson 2016
// // *
// // * The copyright to the computer program(s) herein is the property of
// // * Ericsson Inc. The programs may be used and/or copied only with written
// // * permission from Ericsson Inc. or in accordance with the terms and
// // * conditions stipulated in the agreement/contract under which the
// // * program(s) have been supplied.
// // *******************************************************************************
// // *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static com.ericsson.oss.mediation.translator.model.Constants.SKIP_CURRENT_SERVICE_STATE_UPDATE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_TO_BE_PROCESSED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.INITIATE_SYNC;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.ALARM_SUPERVISION_STATE;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.AUTOMATIC_SYNCHRONIZATION;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.SyncInitiator;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmSupervisionMoReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;

@RunWith(MockitoJUnitRunner.class)
public class HeartBeatCommonHandlerTest {

    @InjectMocks
    private HeartBeatCommonHandler heartBeatCommonHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private FmFunctionMoService fmFunctionMOFacade;

    @Mock
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private ServiceStateModifier serviceStateModifier;

    @Mock
    private FmSupervisionMoReader fmSupervisionMOReader;

    @Mock
    private Map<String, Boolean> managedObjectAttributes;

    @Mock
    private Map<String, Object> alarmAttributes;

    @Mock
    private SyncInitiator syncInitiator;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private AlarmValidator alarmValidator;

    @Mock
    private CurrentServiceStateUpdator currentServiceStateUpdator;

    @Mock
    private AlarmProcessor newAlarmProcessor;
    
    @Mock
    private SystemRecorder systemRecorder;

    Map<String, Boolean> response = new HashMap<String, Boolean>();

    @Test
    public void testOnEventForHeartBeatAlarm() {
        when(alarmRecord.getRecordType()).thenReturn(FMProcessedEventType.HEARTBEAT_ALARM);
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.INDETERMINATE);
        when(alarmRecord.getObjectOfReference()).thenReturn("NetworkElement=1");
        when(alarmRecord.getFdn()).thenReturn("NetworkElement=1");
        when(alarmRecord.getCorrelatedPOId()).thenReturn(-2L);
        final String enodeBFdn = alarmRecord.getFdn();
        when(fmFunctionMOFacade.read(enodeBFdn, FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn("IN_SERVICE");
        when(alarmRecord.getFmxGenerated()).thenReturn("Test");
        final long milliSeconds = new Date().getTime();
        when(alarmRecord.getEventTime()).thenReturn(new Date(milliSeconds + 1000L));
        when(correlatedAlarm.getEventTime()).thenReturn(new Date(milliSeconds));
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        response.put(ALARM_TO_BE_PROCESSED, true);
        response.put(INITIATE_SYNC, false);
        when(currentServiceStateUpdator.updateForHeartBeatAlarm(alarmRecord)).thenReturn(response);
        heartBeatCommonHandler.handleAlarm(alarmRecord);
        verify(currentServiceStateUpdator, times(1)).updateForHeartBeatAlarm(alarmRecord);
        verify(newAlarmProcessor, times(1)).processAlarm(alarmRecord);
    }

    @Test
    public void testOnEventForHeartBeatClearForActiveUnAcknowledge() {
        when(alarmRecord.getRecordType()).thenReturn(FMProcessedEventType.HEARTBEAT_ALARM);
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        when(alarmRecord.getFdn()).thenReturn("NetworkElement=1");
        final String enodeBFdn = alarmRecord.getFdn();
        when(fmFunctionMOFacade.read(enodeBFdn, FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn("HEART_BEAT_FAILURE");
        response.put(ALARM_TO_BE_PROCESSED, true);
        response.put(INITIATE_SYNC, false);
        when(currentServiceStateUpdator.updateForHeartBeatAlarm(alarmRecord)).thenReturn(response);
        when(alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)).thenReturn(true);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getRecordType()).thenReturn(FMProcessedEventType.ALARM);
        when(fmSupervisionMOReader.read(enodeBFdn, AUTOMATIC_SYNCHRONIZATION)).thenReturn(true);
        when(correlatedAlarm.getEventPOId()).thenReturn(1L);
        when(alarmRecord.getFmxGenerated()).thenReturn("Test");
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        when(correlatedAlarm.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        when(fmSupervisionMOReader.readSupervisionAndAutoSyncAttributes(enodeBFdn)).thenReturn(managedObjectAttributes);
        when(managedObjectAttributes.get(ALARM_SUPERVISION_STATE)).thenReturn(true);
        when(managedObjectAttributes.get(AUTOMATIC_SYNCHRONIZATION)).thenReturn(true);
        final long milliSeconds = new Date().getTime();
        when(alarmRecord.getEventTime()).thenReturn(new Date(milliSeconds + 1000L));
        when(correlatedAlarm.getEventTime()).thenReturn(new Date(milliSeconds));
        heartBeatCommonHandler.handleAlarm(alarmRecord);
        verify(currentServiceStateUpdator, times(1)).updateForHeartBeatAlarm(alarmRecord);
        verify(correlatedAlarmProcessor, times(1)).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testOnEventForHeartBeatClearForActiveAcknowledge() {
        when(alarmRecord.getRecordType()).thenReturn(FMProcessedEventType.HEARTBEAT_ALARM);
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        when(alarmRecord.getFdn()).thenReturn("NetworkElement=1");

        final String enodeBFdn = alarmRecord.getFdn();
        when(fmFunctionMOFacade.read(enodeBFdn, FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn("HEART_BEAT_FAILURE");

        response.put(ALARM_TO_BE_PROCESSED, true);
        response.put(INITIATE_SYNC, false);

        when(currentServiceStateUpdator.updateForHeartBeatAlarm(alarmRecord)).thenReturn(response);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);

        when(correlatedAlarm.getRecordType()).thenReturn(FMProcessedEventType.ALARM);
        when(fmSupervisionMOReader.read(enodeBFdn, AUTOMATIC_SYNCHRONIZATION)).thenReturn(true);
        when(correlatedAlarm.getEventPOId()).thenReturn(1L);
        when(alarmRecord.getCorrelatedPOId()).thenReturn(1L);
        when(alarmRecord.getFmxGenerated()).thenReturn("Test");
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        when(correlatedAlarm.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_ACKNOWLEDGED);
        when(correlatedAlarm.getRecordType()).thenReturn(FMProcessedEventType.ALARM);
        when(fmSupervisionMOReader.read(enodeBFdn, ALARM_SUPERVISION_STATE)).thenReturn(true);
        when(fmSupervisionMOReader.readSupervisionAndAutoSyncAttributes(enodeBFdn)).thenReturn(managedObjectAttributes);
        when(managedObjectAttributes.get(ALARM_SUPERVISION_STATE)).thenReturn(true);
        when(managedObjectAttributes.get(AUTOMATIC_SYNCHRONIZATION)).thenReturn(true);
        final long milliSeconds = new Date().getTime();
        when(alarmRecord.getEventTime()).thenReturn(new Date(milliSeconds + 1000L));
        when(correlatedAlarm.getEventTime()).thenReturn(new Date(milliSeconds));
        when(correlatedAlarm.getVisibility()).thenReturn(true);
        when(alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)).thenReturn(true);
        heartBeatCommonHandler.handleAlarm(alarmRecord);
        verify(currentServiceStateUpdator, times(1)).updateForHeartBeatAlarm(alarmRecord);
        verify(correlatedAlarmProcessor, times(1)).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testOnEventForHeartBeatAlarmWithSkipCurrentServiceStateUpdate() {
        final Map<String, String> additionalInformation = new HashMap<>();
        additionalInformation.put(SKIP_CURRENT_SERVICE_STATE_UPDATE,"true");
        when(alarmRecord.getRecordType()).thenReturn(FMProcessedEventType.HEARTBEAT_ALARM);
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.INDETERMINATE);
        when(alarmRecord.getObjectOfReference()).thenReturn("NetworkElement=1");
        when(alarmRecord.getFdn()).thenReturn("NetworkElement=1");
        when(alarmRecord.getCorrelatedPOId()).thenReturn(-2L);
        when(alarmRecord.getAdditionalInformation()).thenReturn(additionalInformation);
        final String enodeBFdn = alarmRecord.getFdn();
        when(fmFunctionMOFacade.read(enodeBFdn, FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn("IN_SERVICE");
        when(alarmRecord.getFmxGenerated()).thenReturn("Test");
        final long milliSeconds = new Date().getTime();
        when(alarmRecord.getEventTime()).thenReturn(new Date(milliSeconds + 1000L));
        when(correlatedAlarm.getEventTime()).thenReturn(new Date(milliSeconds));
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        heartBeatCommonHandler.handleAlarm(alarmRecord);
        verify(currentServiceStateUpdator, times(0)).updateForHeartBeatAlarm(alarmRecord);
        verify(newAlarmProcessor, times(1)).processAlarm(alarmRecord);
    }
}

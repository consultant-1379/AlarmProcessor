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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FAILURE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CLEARED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FMALARM_SUPERVISION_MO_SUFFIX;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.HEARTBEATUPDATEREQUEST;

import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.SOURCE_TYPE;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.ALARM_SUPERVISION_STATE;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.AUTOMATIC_SYNCHRONIZATION;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.fm.capability.util.ModelCapabilities;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.core.events.MediationClientType;
import com.ericsson.oss.mediation.core.events.OperationType;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmSupervisionMoReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmSyncStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.service.model.FmMediationHeartBeatRequest;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

@RunWith(MockitoJUnitRunner.class)
public class CurrentServiceStateUpdatorTest {

    @InjectMocks
    private CurrentServiceStateUpdator currentServiceStateUpdator;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private FmFunctionMoService fmFunctionMOFacade;

    @Mock
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Mock
    private FmSupervisionMoReader fmSupervisionMOReader;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private ModelCapabilities modelCapabilities;
    @Mock
    private FmMediationHeartBeatRequest fmMediationHeartBeatRequest;
    
    @Mock
    private EventSender<MediationTaskRequest> mediationTaskSender;

    @Test
    public void testUpdateForHeartBeatAlarm_Sync_Ongoing() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("fdn");
        when(fmFunctionMOFacade.read("fdn", FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.SYNC_ONGOING.name());
        currentServiceStateUpdator.updateForHeartBeatAlarm(alarmRecord);
        verify(fmFunctionMOFacade).updateCurrentServiceState("fdn", FmSyncStatus100.HEART_BEAT_FAILURE.name());
        verify(systemRecorder).recordEvent("APS", EventLevel.DETAILED, "Ongoing Sync is Discarded and currentServiceState",
                "is changed to HEART_BEAT_FAILURE for NetworkElement", "fdn");
    }

    @Test
    public void testUpdateForHeartBeatAlarm_HB_Failure() {
        final Map<String, Boolean> supervisionAndAutoSyncAttributes = new HashMap<String, Boolean>();
        supervisionAndAutoSyncAttributes.put(ALARM_SUPERVISION_STATE, true);
        supervisionAndAutoSyncAttributes.put(AUTOMATIC_SYNCHRONIZATION, true);
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("fdn");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        when(fmSupervisionMOReader.readSupervisionAndAutoSyncAttributes(alarmRecord.getFdn())).thenReturn(supervisionAndAutoSyncAttributes);
        when(fmFunctionMOFacade.read("fdn", FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.HEART_BEAT_FAILURE.name());
        currentServiceStateUpdator.updateForHeartBeatAlarm(alarmRecord);
        verify(fmFunctionMOFacade).updateCurrentServiceState(alarmRecord.getFdn(), FmSyncStatus100.IN_SERVICE.name());
        verify(systemRecorder).recordEvent("APS", EventLevel.DETAILED, "currentServiceState",
                "is changed to IN_SERVICE from HEART_BEAT_FAILURE for fdn", alarmRecord.getFdn());
    }

    @Test
    public void testUpdateForHeartBeatAlarm_Other_SyncState() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("fdn");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.MAJOR);
        when(fmFunctionMOFacade.read("fdn", FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.ALARM_SUPPRESSED.name());
        currentServiceStateUpdator.updateForHeartBeatAlarm(alarmRecord);
        verify(fmFunctionMOFacade).updateCurrentServiceState(alarmRecord.getFdn(), FmSyncStatus100.HEART_BEAT_FAILURE.name());
        verify(systemRecorder).recordEvent("APS", EventLevel.DETAILED, "currentServiceState", "is changed to HEART_BEAT_FAILURE for NetworkElement",
                alarmRecord.getFdn());
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator#updateForNodeSuspendedAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testUpdateForNodeSuspendedAlarm() {
        final Map<String, Boolean> supervisionAndAutoSyncAttributes = new HashMap<String, Boolean>();
        supervisionAndAutoSyncAttributes.put(ALARM_SUPERVISION_STATE, true);
        supervisionAndAutoSyncAttributes.put(AUTOMATIC_SYNCHRONIZATION, true);
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("fdn");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        when(fmSupervisionMOReader.readSupervisionAndAutoSyncAttributes(alarmRecord.getFdn())).thenReturn(supervisionAndAutoSyncAttributes);
        when(fmFunctionMOFacade.read("fdn", FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.NODE_SUSPENDED.name());
        currentServiceStateUpdator.updateForNodeSuspendedAlarm(alarmRecord);
        verify(fmFunctionMOFacade).updateCurrentServiceState(alarmRecord.getFdn(), FmSyncStatus100.IN_SERVICE.name());
        verify(systemRecorder).recordEvent("APS", EventLevel.DETAILED, "currentServiceState", "is changed to IN_SERVICE from NODE_SUSPENDED for fdn",
                alarmRecord.getFdn());
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator#updateForOutOfSyncAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testUpdateForOutOfSyncAlarm() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("fdn");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        final Map<String, String> additionalInfo = new HashMap<String, String>();
        additionalInfo.put(SOURCE_TYPE, SOURCE_TYPE);
        when(modelCapabilities.isAlarmSyncSupportedByNode(anyString())).thenReturn(false);
        currentServiceStateUpdator.updateForOutOfSyncAlarm(alarmRecord);
        verify(apsInstrumentedBean).incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
    }

    @Test
    public void testUpdateForOutOfSyncAlarm_OutOfSync() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("fdn");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        when(fmFunctionMOFacade.read("fdn", FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(FmSyncStatus100.OUT_OF_SYNC.name());

        currentServiceStateUpdator.updateForOutOfSyncAlarm(alarmRecord);
        verify(apsInstrumentedBean).incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
    }
@Test
public void sendHearbeatStateRequestTest() {
    String failureState = "FAILURE";
    String clearedState = "CLEARED";
	alarmRecord = new ProcessedAlarmEvent();
	alarmRecord.setFdn("fdn");
	alarmRecord.setObjectOfReference("orr");
	String fdn = alarmRecord.getFdn();
	String orr= alarmRecord.getObjectOfReference();
	fmMediationHeartBeatRequest = new FmMediationHeartBeatRequest();
    long currentTimeMsec = System.currentTimeMillis();
    fmMediationHeartBeatRequest.setJobId(HEARTBEATUPDATEREQUEST.concat(Long.toString(currentTimeMsec)));
    fmMediationHeartBeatRequest.setProtocolInfo(OperationType.FM.toString());
    fmMediationHeartBeatRequest.setNodeAddress(fdn.concat(FMALARM_SUPERVISION_MO_SUFFIX));
    currentServiceStateUpdator.sendHearbeatStateRequest(failureState,fdn,orr);
    fmMediationHeartBeatRequest.setState(FAILURE);
    
}
@Test
public void sendHearbeatStateRequestTest1() {
    String failureState = "FAILURE";
	alarmRecord = new ProcessedAlarmEvent();
	alarmRecord.setFdn("fdn");
	alarmRecord.setObjectOfReference("orr");
	String fdn = alarmRecord.getFdn();
	String orr= alarmRecord.getObjectOfReference();
	fmMediationHeartBeatRequest = new FmMediationHeartBeatRequest();
    long currentTimeMsec = System.currentTimeMillis();
    fmMediationHeartBeatRequest.setJobId(HEARTBEATUPDATEREQUEST.concat(Long.toString(currentTimeMsec)));
    fmMediationHeartBeatRequest.setProtocolInfo(OperationType.FM.toString());
    fmMediationHeartBeatRequest.setNodeAddress(fdn.concat(FMALARM_SUPERVISION_MO_SUFFIX));
    currentServiceStateUpdator.sendHearbeatStateRequest(failureState,fdn,orr);
    fmMediationHeartBeatRequest.setState(FAILURE);
    fmMediationHeartBeatRequest.setState(CLEARED);
    fmMediationHeartBeatRequest.setClientType(MediationClientType.EVENT_BASED.name());
    fmMediationHeartBeatRequest.setOor(orr);
    verify(mediationTaskSender).send(fmMediationHeartBeatRequest);
    currentServiceStateUpdator.sendHearbeatStateRequest(failureState,fdn);
	  when(currentServiceStateUpdator.getMultilevelCapabilityValue
	  (alarmRecord)).thenReturn(true); String oor =
	  alarmRecord.getObjectOfReference();
	  currentServiceStateUpdator.sendHearbeatStateRequest(failureState,fdn,orr);
	 
}
@Test
public void sendHearbeatStateRequestTest2() {
	alarmRecord = new ProcessedAlarmEvent();
	fmMediationHeartBeatRequest = new FmMediationHeartBeatRequest();
    long currentTimeMsec = System.currentTimeMillis();
    fmMediationHeartBeatRequest.setJobId(HEARTBEATUPDATEREQUEST.concat(Long.toString(currentTimeMsec)));
    alarmRecord.setFdn("fdn");
	String fdn = alarmRecord.getFdn();
    currentServiceStateUpdator.sendHearbeatStateRequest(FAILURE, fdn);
}


@Test
public void sendHearbeatStateRequestTest3() {
	String failureState = "FAILURE";
	alarmRecord = new ProcessedAlarmEvent();
	alarmRecord.setFdn("fdn");
	alarmRecord.setObjectOfReference("orr");
	String fdn = alarmRecord.getFdn();
	String oor = alarmRecord.getObjectOfReference();
	currentServiceStateUpdator.sendHearbeatStateRequest(failureState,fdn,oor);
    when(currentServiceStateUpdator.getMultilevelCapabilityValue(Mockito.mock(ProcessedAlarmEvent.class))).thenReturn(true);
}
@Test
public void sendHearbeatStateRequestTest4() {
	alarmRecord = new ProcessedAlarmEvent();
	fmMediationHeartBeatRequest = new FmMediationHeartBeatRequest();
    long currentTimeMsec = System.currentTimeMillis();
    fmMediationHeartBeatRequest.setJobId(HEARTBEATUPDATEREQUEST.concat(Long.toString(currentTimeMsec)));
    when(currentServiceStateUpdator.getMultilevelCapabilityValue (Mockito.mock(ProcessedAlarmEvent.class))).thenReturn(true);
	alarmRecord.setFdn("fdn");
	alarmRecord.setObjectOfReference("orr");
	String fdn = alarmRecord.getFdn();
	String oor = alarmRecord.getObjectOfReference();
	currentServiceStateUpdator.updateForHeartBeatAlarm(Mockito.mock(ProcessedAlarmEvent.class));
	
}
@Test
public void sendHearbeatStateRequestTest5() {
	alarmRecord = new ProcessedAlarmEvent();
	fmMediationHeartBeatRequest = new FmMediationHeartBeatRequest();
    long currentTimeMsec = System.currentTimeMillis();
    fmMediationHeartBeatRequest.setJobId(HEARTBEATUPDATEREQUEST.concat(Long.toString(currentTimeMsec)));
    when(currentServiceStateUpdator.getMultilevelCapabilityValue (Mockito.mock(ProcessedAlarmEvent.class))).thenReturn(true);
	alarmRecord.setFdn("fdn");
	alarmRecord.setObjectOfReference("orr");
	
	currentServiceStateUpdator.updateForHeartBeatAlarm(Mockito.mock(ProcessedAlarmEvent.class));	
}
@Test
public void testUpdateForHeartBeatAlarm_Cleared() {
    alarmRecord = new ProcessedAlarmEvent();
    alarmRecord.setFdn("fdn");
    alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
    String fdn = alarmRecord.getFdn();
	String oor = alarmRecord.getObjectOfReference();
	//when(ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())).thenReturn(true);
	 	currentServiceStateUpdator.getMultilevelCapabilityValue (Mockito.mock(ProcessedAlarmEvent.class));
	 	currentServiceStateUpdator.sendHearbeatStateRequest(FAILURE, fdn, oor);
}
@Test
public void testSendingMTRWithOOR() {
	
    alarmRecord = new ProcessedAlarmEvent();
    alarmRecord.setFdn("fdn");
    alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
    String fdn = alarmRecord.getFdn();
	String oor = alarmRecord.getObjectOfReference();
	currentServiceStateUpdator.sendingMTRWithOOR(alarmRecord, fdn, FAILURE);
	 	currentServiceStateUpdator.getMultilevelCapabilityValue (Mockito.mock(ProcessedAlarmEvent.class));
	 	currentServiceStateUpdator.sendHearbeatStateRequest(FAILURE, fdn, oor);
}
@Test
public void testSendingMTRWithOOR1() {
	
    alarmRecord = new ProcessedAlarmEvent();
    alarmRecord.setFdn("fdn");
    alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
    String fdn = alarmRecord.getFdn();
	String oor = alarmRecord.getObjectOfReference();
	currentServiceStateUpdator.sendingMTRWithOOR(alarmRecord, fdn, CLEARED);
	 	when(currentServiceStateUpdator.getMultilevelCapabilityValue (Mockito.mock(ProcessedAlarmEvent.class)));
	 	currentServiceStateUpdator.sendHearbeatStateRequest(CLEARED, fdn, oor);
}
@Test
public void testSendingMTRWithOOR2() {
	
    alarmRecord = new ProcessedAlarmEvent();
    
	 	when(!currentServiceStateUpdator.getMultilevelCapabilityValue (Mockito.mock(ProcessedAlarmEvent.class))).thenReturn(true);
	 when(currentServiceStateUpdator.getMultilevelCapabilityValue (Mockito.mock(ProcessedAlarmEvent.class))).thenReturn(true);
	 	currentServiceStateUpdator.sendHearbeatStateRequest(FAILURE, "fdn", "oor");
}
@Test
public void testSendingMTRWithOOR3() {
when(currentServiceStateUpdator.getMultilevelCapabilityValue (alarmRecord)).thenReturn(true);
String oor = alarmRecord.getObjectOfReference();
currentServiceStateUpdator.sendHearbeatStateRequest(FAILURE, "fdn", "oor");
currentServiceStateUpdator.sendingMTRWithOOR(alarmRecord, "fdn", "orr");
}
}
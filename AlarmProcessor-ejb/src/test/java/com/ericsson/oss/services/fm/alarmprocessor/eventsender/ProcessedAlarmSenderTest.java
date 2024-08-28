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

package com.ericsson.oss.services.fm.alarmprocessor.eventsender;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATEDVISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_CREATED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NORMAL_PROC;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.api.alarmsender.AlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.ServiceProxyProviderBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class ProcessedAlarmSenderTest {

    @InjectMocks
    private ProcessedAlarmSender processedAlarmSender;

    @Mock
    private AlarmProcessingResponse alarmResponse;

    @Mock
    private ModeledEventSender modeledEventSender;

    @Mock
    private ProcessedAlarmEvent processedAlarmEvent;

    @Mock
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Mock
    private AlarmSender alarmSender;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private SystemRecorder systemRecorder;

    @Test
    public void testSendAlarms() {
        processedAlarmEvent = new ProcessedAlarmEvent();
        alarmResponse = new AlarmProcessingResponse();
        final List<ProcessedAlarmEvent> list = new ArrayList<ProcessedAlarmEvent>();
        list.add(processedAlarmEvent);
        alarmResponse.setProcessedAlarms(list);
        processedAlarmSender.sendAlarms(alarmResponse, "123456789");
        verify(modeledEventSender).sendEventToCorbaNbi(processedAlarmEvent, false, "123456789");
        verify(modeledEventSender).sendAlarmMetaData(processedAlarmEvent);
        verify(modeledEventSender).sendAtrInput(processedAlarmEvent);
        verify(modeledEventSender).sendFakeClear(processedAlarmEvent, "123456789", false);

    }

    @Test
    public void testSendAlarms_ProcessingType() {
        processedAlarmEvent = new ProcessedAlarmEvent();
        processedAlarmEvent.setProcessingType(NORMAL_PROC);
        processedAlarmEvent.setFmxGenerated(FMX_CREATED);
        alarmResponse = new AlarmProcessingResponse();
        final List<ProcessedAlarmEvent> list = new ArrayList<ProcessedAlarmEvent>();
        list.add(processedAlarmEvent);
        alarmResponse.setProcessedAlarms(list);
        when(serviceProxyProviderBean.getAlarmSender()).thenReturn(alarmSender);
        processedAlarmSender.sendAlarms(alarmResponse, "123456789");
        verify(modeledEventSender).sendEventToCorbaNbi(processedAlarmEvent, false, "123456789");
        verify(serviceProxyProviderBean.getAlarmSender()).sendAlarm(processedAlarmEvent);
    }
    
    @Test
    public void testSendAlarms_ClearList(){
        processedAlarmEvent = new ProcessedAlarmEvent();
        alarmResponse = new AlarmProcessingResponse();
        final List<ProcessedAlarmEvent> list = new ArrayList<ProcessedAlarmEvent>();
        processedAlarmEvent.setRecordType(FMProcessedEventType.CLEAR_LIST);
        list.add(processedAlarmEvent);
        alarmResponse.setProcessedAlarms(list);
        processedAlarmSender.sendAlarms(alarmResponse, "123456789");
        verify(modeledEventSender, never()).sendEventToSnmpNbi(processedAlarmEvent, "123456789");
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSendAlarms_ProcessingType_Exception() {
        alarmResponse = new AlarmProcessingResponse();
        final List<ProcessedAlarmEvent> list = new ArrayList<ProcessedAlarmEvent>();
        list.add(processedAlarmEvent);
        alarmResponse.setProcessedAlarms(list);
        when(processedAlarmEvent.getFmxGenerated()).thenThrow(Exception.class);
        when(processedAlarmEvent.getEventPOId()).thenReturn(1234L);
        when(processedAlarmEvent.getRecordType()).thenReturn(FMProcessedEventType.ALARM);
        when(processedAlarmEvent.getProcessingType()).thenReturn(NOT_SET);
        final Map<String, Object> alarmAttributes = new HashMap<String, Object>();
        alarmAttributes.put(VISIBILITY, true);
        alarmAttributes.put(CORRELATEDVISIBILITY, true);
        processedAlarmEvent.setEventPOId(1234L);
        processedAlarmSender.sendAlarms(alarmResponse, "123456789");
        verify(openAlarmService).updateAlarm(processedAlarmEvent.getEventPOId(), alarmAttributes);

    }

    @Test
    public void testSendAlarms_Throws_Exception(){
        processedAlarmEvent = new ProcessedAlarmEvent();
        alarmResponse = new AlarmProcessingResponse();
        final List<ProcessedAlarmEvent> list = new ArrayList<ProcessedAlarmEvent>();
        list.add(processedAlarmEvent);
        alarmResponse.setProcessedAlarms(list);
        doThrow(new IllegalArgumentException("Unable to send null events")).when(modeledEventSender).sendEventToSnmpNbi((ProcessedAlarmEvent) anyObject(), anyString());
        processedAlarmSender.sendAlarms(alarmResponse, "123456789");
        verify(modeledEventSender).sendEventToSnmpNbi(processedAlarmEvent, "123456789");
    }
    
    @Test
    public void testSendAlarms_Throws_MultipleExceptions(){
        processedAlarmEvent = new ProcessedAlarmEvent();
        alarmResponse = new AlarmProcessingResponse();
        final List<ProcessedAlarmEvent> list = new ArrayList<ProcessedAlarmEvent>();
        list.add(processedAlarmEvent);
        alarmResponse.setProcessedAlarms(list);
        alarmResponse.setSendFakeClearToNbi(true);
        doThrow(new IllegalArgumentException("Unable to send null events")).when(modeledEventSender).sendEventToCoreOutQueue((ProcessedAlarmEvent) anyObject(), anyString());
        doThrow(new IllegalArgumentException("Unable to send null events")).when(modeledEventSender).sendAlarmMetaData((ProcessedAlarmEvent) anyObject());
        doThrow(new IllegalArgumentException("Unable to send null events")).when(modeledEventSender).sendAtrInput((ProcessedAlarmEvent) anyObject());
        doThrow(new IllegalArgumentException("Unable to send null events")).when(modeledEventSender).sendFakeClear((ProcessedAlarmEvent) anyObject(), anyString(), anyBoolean());
        doThrow(new IllegalArgumentException("Unable to send null events")).when(modeledEventSender).sendFakeClearToBeSentToCorbaNbi((ProcessedAlarmEvent) anyObject(), anyString());
        processedAlarmSender.sendAlarms(alarmResponse, "123456789");
        verify(modeledEventSender).sendEventToCoreOutQueue(processedAlarmEvent, "123456789");
        verify(modeledEventSender).sendAlarmMetaData(processedAlarmEvent);
        verify(modeledEventSender).sendAtrInput(processedAlarmEvent);
        verify(modeledEventSender).sendFakeClear(processedAlarmEvent, "123456789", false);
        verify(modeledEventSender).sendFakeClearToBeSentToCorbaNbi(processedAlarmEvent, "123456789");
    }

    @Test
    public void testsendDuplicateAlarms(){
        processedAlarmEvent = new ProcessedAlarmEvent();
        alarmResponse = new AlarmProcessingResponse();
        final List<ProcessedAlarmEvent> list = new ArrayList<ProcessedAlarmEvent>();
        list.add(processedAlarmEvent);
        alarmResponse.setProcessedAlarms(list);
        final boolean sendFakeClearToNbi = false;
        final boolean sendFakeClearToUiAndNbi = false;
        processedAlarmSender.sendDuplicateAlarms(processedAlarmEvent, "123456789", sendFakeClearToNbi, sendFakeClearToUiAndNbi);
        verify(modeledEventSender).sendEventToCorbaNbi(processedAlarmEvent, false, "123456789");
    }
}

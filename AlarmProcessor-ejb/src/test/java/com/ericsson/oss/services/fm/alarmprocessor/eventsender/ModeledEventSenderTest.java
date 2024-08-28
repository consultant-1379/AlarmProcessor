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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FM_CORE_OUT_QUEUE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.eventbus.EventConfiguration;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.services.fm.alarmprocessor.builders.AlarmTextRouteInputEventBuilder;
import com.ericsson.oss.services.fm.alarmprocessor.builders.MetaDataInformationBuilder;
import com.ericsson.oss.services.fm.models.processedevent.ATRInputEvent;
import com.ericsson.oss.services.fm.models.processedevent.AlarmMetadataInformation;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class ModeledEventSenderTest {

    @InjectMocks
    private ModeledEventSender modeledEventSender;

    @Mock
    private EventSender<ProcessedAlarmEvent> processedAlarmEventSender;

    @Mock
    private EventSender<ATRInputEvent> atrModeledEventSender;

    @Mock
    private EventSender<AlarmMetadataInformation> metaDataInformationEvent;

    @Mock
    private ProcessedAlarmEvent processedAlarmEvent;

    @Mock
    private MetaDataInformationBuilder metaDataInformationBuilder;

    @Mock
    private AlarmMetadataInformation metaDataInformation;

    @Mock
    private AlarmTextRouteInputEventBuilder atrInputEventBuilder;

    @Mock
    private ATRInputEvent atrInputEvent;

    @Test
    public void test_onEvent() {
        when(processedAlarmEvent.isVisibility()).thenReturn(true);
        modeledEventSender.sendEventToCoreOutQueue(processedAlarmEvent, "123456789");
        modeledEventSender.sendEventToSnmpNbi(processedAlarmEvent, "123456789");
        modeledEventSender.sendAlarmMetaData(processedAlarmEvent);
        modeledEventSender.sendAtrInput(processedAlarmEvent);
        modeledEventSender.sendFakeClear(processedAlarmEvent, "123456789", false);
        when(metaDataInformationBuilder.build(processedAlarmEvent)).thenReturn(metaDataInformation);
        when(atrInputEventBuilder.build(processedAlarmEvent)).thenReturn(atrInputEvent);
        verify(atrModeledEventSender, times(1)).send((ATRInputEvent) Matchers.anyObject());
        verify(metaDataInformationEvent, times(1)).send((AlarmMetadataInformation) Matchers.anyObject());

    }

    @Test
    public void test_sendClearAlarm_ErrorMessage() {
        when(processedAlarmEvent.isVisibility()).thenReturn(false);
        when(processedAlarmEvent.getRecordType()).thenReturn(FMProcessedEventType.ERROR_MESSAGE);
        modeledEventSender.sendEventToCoreOutQueue(processedAlarmEvent, "123456789");
        modeledEventSender.sendAlarmMetaData(processedAlarmEvent);
        modeledEventSender.sendAtrInput(processedAlarmEvent);
        modeledEventSender.sendFakeClear(processedAlarmEvent, "123456789", false);
        verify(processedAlarmEventSender).send((ProcessedAlarmEvent) Matchers.anyObject(), anyString(), (EventConfiguration) anyObject());
    }

    @Test
    public void test_sendClearAlarm_ErrorMessage_With_AlarmreceivedTimeNull() {
        when(processedAlarmEvent.isVisibility()).thenReturn(false);
        when(processedAlarmEvent.getRecordType()).thenReturn(FMProcessedEventType.ERROR_MESSAGE);
        modeledEventSender.sendEventToCoreOutQueue(processedAlarmEvent, null);
        modeledEventSender.sendEventToSnmpNbi(processedAlarmEvent, null);
        modeledEventSender.sendAlarmMetaData(processedAlarmEvent);
        modeledEventSender.sendAtrInput(processedAlarmEvent);
        modeledEventSender.sendFakeClear(processedAlarmEvent, null, false);

        verify(processedAlarmEventSender).send(processedAlarmEvent, FM_CORE_OUT_QUEUE);
    }

    @Test
    public void test_sendClearAlarm() {
        when(processedAlarmEvent.isVisibility()).thenReturn(false);
        when(processedAlarmEvent.getRecordType()).thenReturn(FMProcessedEventType.ALARM);
        modeledEventSender.sendEventToCoreOutQueue(processedAlarmEvent, "123456789");
        modeledEventSender.sendAlarmMetaData(processedAlarmEvent);
        modeledEventSender.sendAtrInput(processedAlarmEvent);
        modeledEventSender.sendFakeClear(processedAlarmEvent, "123456789", false);
        verify(processedAlarmEventSender, times(1)).send((ProcessedAlarmEvent) anyObject(), anyString(), (EventConfiguration) anyObject());
    }

    @Test
    public void test_sendToNBI() {
        processedAlarmEvent = new ProcessedAlarmEvent();
        processedAlarmEvent.setVisibility(true);
        processedAlarmEvent.setCorrelatedVisibility(false);
        processedAlarmEvent.setRecordType(FMProcessedEventType.ALARM);
        modeledEventSender.sendEventToCorbaNbi(processedAlarmEvent, false, "123456789");
        verify(processedAlarmEventSender).send((ProcessedAlarmEvent) Matchers.anyObject(), anyString(), (EventConfiguration) anyObject());

    }

    @Test
    public void test_sendToNBI_WithoutEventProperties() {
        processedAlarmEvent = new ProcessedAlarmEvent();
        processedAlarmEvent.setVisibility(true);
        processedAlarmEvent.setCorrelatedVisibility(false);
        processedAlarmEvent.setRecordType(FMProcessedEventType.ALARM);
        modeledEventSender.sendEventToCorbaNbi(processedAlarmEvent, false, "");
        verify(processedAlarmEventSender).send((ProcessedAlarmEvent) Matchers.anyObject(), Matchers.anyString());

    }

    @Test
    public void test_sendToNBI_CorrelatedVisibility_True() {
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(HIDE_OPERATION, FMX_HIDE);
        processedAlarmEvent = new ProcessedAlarmEvent();
        processedAlarmEvent.setVisibility(false);
        processedAlarmEvent.setCorrelatedVisibility(true);
        processedAlarmEvent.setAdditionalInformation(additionalInformation);
        processedAlarmEvent.setRecordType(FMProcessedEventType.ERROR_MESSAGE);
        final boolean result = modeledEventSender.sendEventToCorbaNbi(processedAlarmEvent, false, "");
        assertTrue(result);
    }

    @Test
    public void test_sendToNBI_CorrelatedVisibility() {
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(HIDE_OPERATION, FMX_HIDE);
        processedAlarmEvent = new ProcessedAlarmEvent();
        processedAlarmEvent.setVisibility(false);
        processedAlarmEvent.setCorrelatedVisibility(true);
        processedAlarmEvent.setAdditionalInformation(additionalInformation);
        processedAlarmEvent.setRecordType(FMProcessedEventType.ALARM);
        final boolean result = modeledEventSender.sendEventToCorbaNbi(processedAlarmEvent, false, "");
        assertTrue(result);
    }

}

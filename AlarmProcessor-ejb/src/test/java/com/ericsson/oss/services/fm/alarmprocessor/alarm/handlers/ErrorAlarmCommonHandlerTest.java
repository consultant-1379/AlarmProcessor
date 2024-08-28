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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertTrue;

import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedUpdateAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;

@RunWith(MockitoJUnitRunner.class)
public class ErrorAlarmCommonHandlerTest {

    @InjectMocks
    private ErrorAlarmCommonHandler errorAlarmCommonHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private ServiceStateModifier serviceStateModifier;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Mock
    private CorrelatedUpdateAlarmProcessor correlatedUpdateAlarmProcessor;

    @Test
    public void testHandleAlarm_WithSeverityClear() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);

        errorAlarmCommonHandler.handleAlarm(alarmRecord);
        verify(apsInstrumentedBean).incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());

    }

    @Test
    public void testHandleAlarm_WithSeverity() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        alarmRecord.setFmxGenerated(NOT_SET);
        alarmRecord.setVisibility(false);
        alarmRecord.setCorrelatedPOId(1234L);
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        new ArrayList<String>();
        final Map<String, Object> alarmAttributes = new HashMap<String, Object>();
        additionalInformation.put(HIDE_OPERATION, FMX_HIDE);
        alarmRecord.setAdditionalInformation(additionalInformation);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn((long) 30);
        when(alarmAttributesPopulator.populateDeleteAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmAttributes);
        errorAlarmCommonHandler.handleAlarm(alarmRecord);
        verify(openAlarmService).removeAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
        verify(apsInstrumentedBean).incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());

    }

    @Test
    public void testHandleAlarm_WithShowRequestOnHiddenAlarm() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        alarmRecord.setFmxGenerated(NOT_SET);
        alarmRecord.setVisibility(true);
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        new ArrayList<String>();
        additionalInformation.put(HIDE_OPERATION, FMX_HIDE);
        alarmRecord.setAdditionalInformation(additionalInformation);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn((long) 30);
        when(correlatedAlarm.getVisibility()).thenReturn(false);
        AlarmProcessingResponse alarmProcessingResponse=errorAlarmCommonHandler.handleAlarm(alarmRecord);
        verify(apsInstrumentedBean).incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
        assertTrue(!alarmProcessingResponse.isSendFakeClearToUiAndNbi());
    }

    @Test
    public void testHandleAlarm_WithCorrelatedAlarm_Null() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        alarmRecord.setAlarmState(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(-2L);
        errorAlarmCommonHandler.handleAlarm(alarmRecord);
        verify(openAlarmService, times(1)).insertAlarmRecord((Map<String, Object>) anyObject());
        verify(apsInstrumentedBean).incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());

    }

}

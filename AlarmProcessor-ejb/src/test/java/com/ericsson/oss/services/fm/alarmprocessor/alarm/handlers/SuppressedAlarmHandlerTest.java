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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

@RunWith(MockitoJUnitRunner.class)
public class SuppressedAlarmHandlerTest {

    @InjectMocks
    private SuppressedAlarmHandler suppressedAlarmHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ServiceStateModifier serviceStateModifier;

    @Mock
    private AlarmProcessor newAlarmProcessor;

    @Mock
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Mock
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private AlarmValidator alarmValidator;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Test
    public void testHandleAlarm_WithCorrelated_notNull() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn((long) 30);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(true);
        when(correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        suppressedAlarmHandler.handleAlarm(alarmRecord);
        verify(serviceStateModifier).updateAlarmSuppressedServiceState(alarmRecord.getFdn(), AlarmProcessorConstants.ALARM_SUPPRESSED_STATE);
        verify(correlatedAlarmProcessor).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testHandleAlarm_WithCorrelated_Null() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(-2L);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(true);
        when(correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        suppressedAlarmHandler.handleAlarm(alarmRecord);
        verify(serviceStateModifier).updateAlarmSuppressedServiceState(alarmRecord.getFdn(), AlarmProcessorConstants.ALARM_SUPPRESSED_STATE);
        verify(newAlarmProcessor).processAlarm(alarmRecord);
    }

    @Test
    public void testHandleAlarm_validatorFalse() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(1234L);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(false);
        when(correlatedAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        suppressedAlarmHandler.handleAlarm(alarmRecord);
        verify(serviceStateModifier).updateAlarmSuppressedServiceState(alarmRecord.getFdn(), AlarmProcessorConstants.ALARM_SUPPRESSED_STATE);
        verify(newAlarmProcessor, times(0)).processAlarm(alarmRecord);
    }

}

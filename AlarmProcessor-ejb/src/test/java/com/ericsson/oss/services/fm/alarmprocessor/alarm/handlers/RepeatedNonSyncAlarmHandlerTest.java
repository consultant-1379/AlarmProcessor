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

import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

@RunWith(MockitoJUnitRunner.class)
public class RepeatedNonSyncAlarmHandlerTest {

    @InjectMocks
    private RepeatedNonSyncAlarmHandler repeatedNonSyncAlarmHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private NormalAlarmHandler normalAlarmHandler;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse1;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private AlarmValidator alarmValidator;

    @Mock
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Mock
    private AlarmProcessor newAlarmProcessor;

    @Mock
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private NonSyncAlarmHandler nonSyncAlarmHandler;

    @Test
    public void testHandleAlarm() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        alarmRecord.setRecordType(FMProcessedEventType.NON_SYNCHABLE_ALARM);
        when(nonSyncAlarmHandler.handleAlarm(alarmRecord)).thenReturn(alarmProcessingResponse);
        repeatedNonSyncAlarmHandler.handleAlarm(alarmRecord);
        verify(nonSyncAlarmHandler, times(1)).handleAlarm(alarmRecord);
    }

    @Test
    public void testHandleAlarm_correlated_notnull() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn((long) 30);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(true);
        when(correlatedAlarmProcessor.processRepeated(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        repeatedNonSyncAlarmHandler.handleAlarm(alarmRecord);
        verify(correlatedAlarmProcessor, times(1)).processRepeated(alarmRecord, correlatedAlarm);

    }

    @Test
    public void testHandleAlarm_correlated_null() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(-2L);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(true);
        when(newAlarmProcessor.processAlarm(alarmRecord)).thenReturn(alarmProcessingResponse);
        repeatedNonSyncAlarmHandler.handleAlarm(alarmRecord);
        verify(newAlarmProcessor, times(1)).processAlarm(alarmRecord);
    }
}

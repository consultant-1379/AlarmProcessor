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

package com.ericsson.oss.services.fm.alarmprocessor.processors;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

@RunWith(MockitoJUnitRunner.class)
public class CorrelatedUpdateAlarmProcessorTest {

    @InjectMocks
    private CorrelatedUpdateAlarmProcessor correlatedUpdateAlarmProcessor;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private ClearAlarmProcessor clearAlarmProcessor;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Mock
    private ReplaceAlarmProcessor replaceAlarmProcessor;

    @Test
    public void testProcessNormalAlarm_WithSeverityClear() {
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        when(clearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmRecord);
        correlatedUpdateAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm);
        verify(clearAlarmProcessor, times(1)).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testProcessNormalAlarm_WithSeverity() {
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        correlatedUpdateAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm);
        verify(replaceAlarmProcessor, times(1)).processNormalAlarm(alarmRecord, correlatedAlarm);
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedUpdateAlarmProcessor#processRepeatedAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessRepeatedAlarm_WithSeverityClear() {
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        when(clearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmRecord);
        correlatedUpdateAlarmProcessor.processRepeatedAlarm(alarmRecord, correlatedAlarm);
        verify(clearAlarmProcessor, times(1)).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testProcessRepeatedAlarm_WithSeverity() {
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        correlatedUpdateAlarmProcessor.processRepeatedAlarm(alarmRecord, correlatedAlarm);
        verify(replaceAlarmProcessor, times(1)).processRepeatedAlarm(alarmRecord, correlatedAlarm);
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedUpdateAlarmProcessor#processAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessAlarm_WithSeverityClear() {
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        when(clearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmRecord);
        correlatedUpdateAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(clearAlarmProcessor, times(1)).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testProcessAlarm_WithSeverity() {
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        correlatedUpdateAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(replaceAlarmProcessor, times(1)).processNormalAlarm(alarmRecord, correlatedAlarm);
    }

}

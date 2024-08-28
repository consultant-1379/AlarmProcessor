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
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class NonSyncAlarmHandlerTest {

    @InjectMocks
    private NonSyncAlarmHandler nonSyncAlarmHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Mock
    private AlarmValidator alarmValidator;
    @Mock
    AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private AlarmProcessor newAlarmProcessor;

    @Test
    public void testHandleAlarm() {

        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(1234L);
        when(alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)).thenReturn(true);
        when(correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        nonSyncAlarmHandler.handleAlarm(alarmRecord);
        verify(correlatedAlarmProcessor).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testHandleAlarm_CorrelatedAlarm_Null() {

        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(-2L);
        when(newAlarmProcessor.processAlarm(alarmRecord)).thenReturn(alarmProcessingResponse);
        nonSyncAlarmHandler.handleAlarm(alarmRecord);
        verify(newAlarmProcessor).processAlarm(alarmRecord);
    }

}

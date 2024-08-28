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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_TO_BE_PROCESSED;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.OutOfSyncAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class OutOfSyncAlarmHandlerTest {

    @InjectMocks
    private OutOfSyncAlarmHandler outOfSyncAlarmHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private CurrentServiceStateUpdator currentServiceStateUpdator;

    @Mock
    private AlarmProcessor newAlarmProcessor;

    @Mock
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Mock
    private AlarmValidator alarmValidator;

    @Mock
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Test
    public void testHandleAlarm_AlarmToBeProcessed_State() {
        final Map<String, Boolean> response = new HashMap<String, Boolean>();
        response.put(ALARM_TO_BE_PROCESSED, true);
        when(currentServiceStateUpdator.updateForOutOfSyncAlarm(alarmRecord)).thenReturn(response);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(1234L);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(true);
        when(correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        outOfSyncAlarmHandler.handleAlarm(alarmRecord);
        verify(correlatedAlarmProcessor).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testHandleAlarm_Correlated_null() {

        final Map<String, Boolean> response = new HashMap<String, Boolean>();
        response.put(ALARM_TO_BE_PROCESSED, true);
        when(currentServiceStateUpdator.updateForOutOfSyncAlarm(alarmRecord)).thenReturn(response);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(-2L);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(true);
        when(correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        outOfSyncAlarmHandler.handleAlarm(alarmRecord);
        verify(newAlarmProcessor).processAlarm(alarmRecord);

    }

}

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
public class TechnicianPresentAlarmHandlerTest {

    @InjectMocks
    private TechnicianPresentAlarmHandler technicianPresentAlarmHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ServiceStateModifier functionMOModifier;

    @Mock
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private AlarmValidator alarmValidator;

    @Mock
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private AlarmProcessor newAlarmProcessor;

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.TechnicianPresentAlarmHandler#handleAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */

    private static final String NE_FDN = "NetworkElement=TESTNODE";

    @Test
    public void testHandleAlarm_WithCorrelatedAlarm_NotNull() {
        alarmRecord.setFdn(NE_FDN);
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn((long) 30);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(true);
        when(correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        technicianPresentAlarmHandler.handleAlarm(alarmRecord);
        verify(functionMOModifier).updateTechnicianPresentServiceState(alarmRecord.getFdn(), AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE);
        verify(correlatedAlarmProcessor).processAlarm(alarmRecord, correlatedAlarm);

    }

    @Test
    public void testHandleAlarm_WithCorrelatedAlarm_NotNull_SeverityClear() {
        alarmRecord.setFdn(NE_FDN);
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn((long) 30);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(true);
        when(correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        technicianPresentAlarmHandler.handleAlarm(alarmRecord);
        verify(functionMOModifier, times(0)).updateTechnicianPresentServiceState(alarmRecord.getFdn(),
                AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE);
        verify(correlatedAlarmProcessor).processAlarm(alarmRecord, correlatedAlarm);

    }

    @Test
    public void testHandleAlarm_WithCorrelatedAlarm_Null() {
        alarmRecord.setFdn(NE_FDN);
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(-2L);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(true);
        when(newAlarmProcessor.processAlarm(alarmRecord)).thenReturn(alarmProcessingResponse);
        technicianPresentAlarmHandler.handleAlarm(alarmRecord);
        verify(functionMOModifier).updateTechnicianPresentServiceState(alarmRecord.getFdn(), AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE);
        verify(newAlarmProcessor).processAlarm(alarmRecord);

    }

    @Test
    public void testHandleAlarm_WithCorrelatedAlarm_NotNull_validatorFalse() {
        alarmRecord.setFdn(NE_FDN);
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn((long) 30);
        when((alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm))).thenReturn(false);
        technicianPresentAlarmHandler.handleAlarm(alarmRecord);
        verify(functionMOModifier).updateTechnicianPresentServiceState(alarmRecord.getFdn(), AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE);
        verify(correlatedAlarmProcessor, times(0)).processAlarm(alarmRecord, correlatedAlarm);

    }

}

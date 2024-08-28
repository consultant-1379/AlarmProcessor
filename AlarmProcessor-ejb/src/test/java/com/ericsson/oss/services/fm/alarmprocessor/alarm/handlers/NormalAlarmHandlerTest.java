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

import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class NormalAlarmHandlerTest {

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.NormalAlarmHandler#handleAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */

    @InjectMocks
    private NormalAlarmHandler normalAlarmHandler;

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

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Test
    public void testHandleAlarm() {
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(1234L);
        when(alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)).thenReturn(true);
        when(correlatedAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        normalAlarmHandler.handleAlarm(alarmRecord);
        verify(correlatedAlarmProcessor).processNormalAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testHandleAlarm_CorrelatedAlarm_Null() {
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(-2L);
        when(newAlarmProcessor.processAlarm(alarmRecord)).thenReturn(alarmProcessingResponse);
        normalAlarmHandler.handleAlarm(alarmRecord);
        verify(newAlarmProcessor).processAlarm(alarmRecord);
    }

    @Test
    public void testHandleAlarm_CorrelatedAlarm() {
        final Map<String, String> addionalInformation = new HashMap<String, String>();
        addionalInformation.put(HIDE_OPERATION, FMX_HIDE);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(-2L);
        when(alarmRecord.getAdditionalInformation()).thenReturn(addionalInformation);
        normalAlarmHandler.handleAlarm(alarmRecord);
        verify(apsInstrumentedBean).incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
    }
}

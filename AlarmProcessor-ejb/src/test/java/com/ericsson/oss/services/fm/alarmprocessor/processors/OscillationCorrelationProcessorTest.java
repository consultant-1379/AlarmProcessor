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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
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
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

@RunWith(MockitoJUnitRunner.class)
public class OscillationCorrelationProcessorTest {

    @InjectMocks
    private OscillationCorrelationProcessor oscillationCorrelationProcessor;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private ServiceStateModifier serviceStateModifier;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    final Map<String, Object> pOAttributes = new HashMap<String, Object>();

    final Map<String, Object> alarmAttributes = new HashMap<String, Object>();

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.OscillationCorrelationProcessor#processAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessAlarm_WhenOscillation_On() {

        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setEventPOId(1234L);
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CRITICAL);
        when(configParametersListener.getOscillatingCorrelation()).thenReturn(true);
        when(alarmReader.readAllAttributes(1234L)).thenReturn(pOAttributes);
        when(alarmAttributesPopulator.populateUpdateAlarm(alarmRecord, pOAttributes)).thenReturn(alarmAttributes);
        oscillationCorrelationProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(apsInstrumentedBean).incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
        verify(serviceStateModifier, times(1)).updateFmFunctionBasedOnSpecificProblem(alarmRecord);
        verify(openAlarmService, times(1)).updateAlarm(alarmRecord.getEventPOId(), alarmAttributes);

    }

    @Test
    public void testProcessAlarm_WhenOscillation_OFF() {

        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setCorrelatedPOId(AlarmProcessorConstants.DEFAULT_EVENTPOID_VALUE);
        when(configParametersListener.getOscillatingCorrelation()).thenReturn(false);
        oscillationCorrelationProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(openAlarmService, times(1)).insertAlarmRecord((Map<String, Object>) anyObject());

    }

}

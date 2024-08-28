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

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_PROCESSED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NORMAL_PROC;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventTrendIndication;

@RunWith(MockitoJUnitRunner.class)
public class ReplaceAlarmProcessorTest {

    @InjectMocks
    private ReplaceAlarmProcessor replaceAlarmProcessor;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private ServiceStateModifier serviceStateModifier;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    final Map<String, Object> pOAttributes = new HashMap<String, Object>();

    final Map<String, Object> alarmAttributes = new HashMap<String, Object>();

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.ReplaceAlarmProcessor#processNormalAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, ProcessedAlarmEvent)}
     * .
     */

    @Before
    public void setUp() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setObjectOfReference("MeContext=LTE09ERBS00009");
        alarmRecord.setSpecificProblem("SpecificProblem");
        alarmRecord.setProbableCause("ProbableCause");
        alarmRecord.setEventType("EventType");
        alarmRecord.setRecordType(FMProcessedEventType.ALARM);
        alarmRecord.setAlarmNumber(12345L);
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        alarmRecord.setProcessingType(NORMAL_PROC);
        alarmRecord.setVisibility(true);
        alarmRecord.setCorrelatedVisibility(true);
        alarmRecord.setAlarmNumber(12L);
        alarmRecord.setFmxGenerated(FMX_PROCESSED);
        alarmRecord.setEventPOId(123456L);
        alarmRecord.setCorrelatedPOId(12345L);
        alarmRecord.setCeaseOperator(CEASE_OPERATOR);
        alarmRecord.setPreviousSeverity(ProcessedEventSeverity.CLEARED);
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.MAJOR);
        alarmRecord.setTrendIndication(ProcessedEventTrendIndication.LESS_SEVERE);
        alarmRecord.setCorrelatedPOId(123456L);
        alarmRecord.setAckOperator("Operator");
        alarmRecord.setAlarmingObject(",FMSupervision=1");
        alarmRecord.setProblemText("problemText");
        // alarmRecord.setAdditionalInformation("additionalInformation");
        alarmRecord.setCeaseTime(new Date());
        alarmRecord.setAckTime(new Date());
        alarmRecord.setAlarmState(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        alarmRecord.setEventTime(new Date());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessNormalAlarm() {

        when(configParametersListener.getUpdateInsertTime()).thenReturn(true);
        when(alarmReader.readAllAttributes(Matchers.anyLong())).thenReturn(Matchers.anyMap());
        replaceAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm);
        verify(openAlarmService, times(1)).updateAlarm(Matchers.anyLong(), Matchers.anyMap());
        verify(serviceStateModifier).updateFmFunctionBasedOnSpecificProblem(alarmRecord);
        verify(apsInstrumentedBean).incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.ReplaceAlarmProcessor#processRepeatedAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessRepeatedAlarm() {
        alarmRecord = new ProcessedAlarmEvent();
        correlatedAlarm = new ProcessedAlarmEvent();
        alarmRecord.setRecordType(FMProcessedEventType.REPEATED_ALARM);
        replaceAlarmProcessor.processRepeatedAlarm(alarmRecord, correlatedAlarm);
        verify(apsInstrumentedBean).incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
        verify(openAlarmService).updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.ReplaceAlarmProcessor#processAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessAlarm() {
        replaceAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm);
        verify(apsInstrumentedBean).incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
    }
}

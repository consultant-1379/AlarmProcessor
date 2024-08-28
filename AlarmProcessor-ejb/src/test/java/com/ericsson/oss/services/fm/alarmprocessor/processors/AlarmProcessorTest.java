/*------------------------------------------------------------------------------
 *******************************************************************************
 COPYRIGHT Ericsson 2016
 *
 The copyright to the computer program(s) herein is the property of
 Ericsson Inc. The programs may be used and/or copied only with written
 permission from Ericsson Inc. or in accordance with the terms and
 conditions stipulated in the agreement/contract under which the
 program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.processors;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmsCountOnNodesMapManager;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventTrendIndication;

@RunWith(MockitoJUnitRunner.class)
public class AlarmProcessorTest {

    @InjectMocks
    private AlarmProcessor alarmProcessor;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Mock
    private AlarmsCountOnNodesMapManager alarmsCountOnNodesMapManager;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor#processAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Before
    public void setUp() {
        alarmRecord = new ProcessedAlarmEvent();
        final Map<String, String> additionalInformationOfAlarms = new HashMap<>();
        additionalInformationOfAlarms.put("sourceType", "ERBS");
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
        alarmRecord.setFdn("NetworkElement=APS_Groovy_003");
        alarmRecord.setAdditionalInformation(additionalInformationOfAlarms);
        alarmRecord.setCeaseTime(new Date());
        alarmRecord.setAckTime(new Date());
        alarmRecord.setAlarmState(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        alarmRecord.setEventTime(new Date());
    }

    @Test
    public void testProcessAlarm_WithSeverityClear() {
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        alarmProcessor.processAlarm(alarmRecord);
        verify(clearAlarmsCacheManager, times(1)).addClearAlarm(alarmRecord);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessAlarm_WithSeverity() {
        alarmProcessor.processAlarm(alarmRecord);
        verify(openAlarmService, times(1)).insertAlarmRecord(Matchers.anyMap());
        verify(alarmsCountOnNodesMapManager, times(1)).incrementAlarmsCountRequest("APS_Groovy_003", "ERBS");
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor#processClearAllAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */

    @Test
    public void testProcessClearAllAlarm() {

        alarmRecord.setPresentSeverity(ProcessedEventSeverity.MAJOR);
        alarmProcessor.processClearAllAlarm(alarmRecord);
        verify(openAlarmService, times(1)).insertAlarmRecord(Matchers.anyMap());
    }

}

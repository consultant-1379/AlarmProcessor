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

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.verify;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_ALARM_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_UPDATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_DETAIL;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_PROCESSED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NORMAL_PROC;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventTrendIndication;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;

@RunWith(MockitoJUnitRunner.class)
public class CorrelatedClearAlarmProcessorTest {

    @InjectMocks
    private CorrelatedClearAlarmProcessor correlatedClearAlarmProcessor;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private OscillationCorrelationProcessor oscillationCorrelationProcessor;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    Map<String, Object> alarmAttributes = new HashMap<String, Object>();

    // final Map<String, String> additionalInformation = alarmRecord.getAdditionalInformation();

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedClearAlarmProcessor#processAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
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

    @Test
    public void testProcessAlarm() {
        correlatedAlarm = new ProcessedAlarmEvent();
        correlatedAlarm.setEventPOId(12345L);
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setAlarmState(ProcessedEventState.CLEARED_ACKNOWLEDGED);
        alarmRecord.setAckOperator(correlatedAlarm.getAckOperator());
        alarmRecord.setAckTime(correlatedAlarm.getAckTime());
        alarmRecord.setLastUpdatedTime(new Date());
        alarmRecord.setProblemDetail(AlarmProcessorConstants.DELETE_ALARM_PROBLEM_DETAIL_FMX);
        alarmRecord.setEventPOId(3456L);
        alarmRecord.setCorrelatedPOId(6589L);

        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(HIDE_OPERATION, FMX_HIDE);
        alarmRecord.setAdditionalInformation(additionalInformation);
        alarmRecord.setVisibility(false);
        final Map<String, Object> alarmAttributes = new HashMap<String, Object>();
        correlatedAlarm.setCeaseOperator("Operator");
        correlatedAlarm.setCeaseTime(new Date());
        correlatedAlarm.setLastAlarmOperation(ProcessedLastAlarmOperation.CHANGE);
        correlatedAlarm.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        correlatedAlarm.setPreviousSeverity(ProcessedEventSeverity.MAJOR);
        correlatedAlarm.setAckTime(new Date());
        correlatedAlarm.setAckOperator("ackOperator");
        alarmAttributes.put(CEASE_OPERATOR, correlatedAlarm.getCeaseOperator());
        alarmAttributes.put(CEASE_TIME, correlatedAlarm.getCeaseTime());
        alarmAttributes.put(LAST_ALARM_OPERATION, correlatedAlarm.getLastAlarmOperation().name());
        alarmAttributes.put(PRESENT_SEVERITY, correlatedAlarm.getPresentSeverity().name());
        alarmAttributes.put(PREVIOUS_SEVERITY, correlatedAlarm.getPreviousSeverity().name());
        alarmAttributes.put(PROBLEM_DETAIL, AlarmProcessorConstants.DELETE_ALARM_PROBLEM_DETAIL_FMX);
        alarmAttributes.put(ACK_TIME, correlatedAlarm.getAckTime());
        alarmAttributes.put(LAST_UPDATED, new Date());
        alarmAttributes.put(ADDITIONAL_INFORMATION, "add");
        alarmAttributes.put(VISIBILITY, alarmRecord.getVisibility());
        alarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_ACKNOWLEDGED.name());
        alarmAttributes.put(ACK_OPERATOR, correlatedAlarm.getAckOperator());
        correlatedClearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(apsInstrumentedBean).incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
        verify(openAlarmService).removeAlarm(anyLong(), anyMap());
    }

    @Test
    public void testProcessAlarm_WithSeverity_Clear() {
        alarmProcessingResponse = new AlarmProcessingResponse();
        correlatedAlarm = new ProcessedAlarmEvent();
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(HIDE_OPERATION, FMX_PROCESSED);
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        correlatedClearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(apsInstrumentedBean).incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());

    }

    @Test
    public void testProcessAlarm_WithSeverity() {
        alarmProcessingResponse = new AlarmProcessingResponse();
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(HIDE_OPERATION, FMX_PROCESSED);
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.MAJOR);
        correlatedClearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(oscillationCorrelationProcessor).processAlarm(alarmRecord, correlatedAlarm);

    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedClearAlarmProcessor#populateAlarmAttributesForRemoving(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testPopulateAlarmAttributesForRemoving() {
        correlatedAlarm = new ProcessedAlarmEvent();
        correlatedAlarm.setEventPOId(12345L);
        correlatedAlarm.setCeaseOperator("Operator");
        correlatedAlarm.setCeaseTime(new Date());
        correlatedAlarm.setLastAlarmOperation(ProcessedLastAlarmOperation.CHANGE);
        correlatedAlarm.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        correlatedAlarm.setPreviousSeverity(ProcessedEventSeverity.MAJOR);
        correlatedAlarm.setAckTime(new Date());
        correlatedAlarm.setAckOperator("ackOperator");
        correlatedClearAlarmProcessor.populateAlarmAttributesForRemoving(alarmRecord, correlatedAlarm);

    }

}

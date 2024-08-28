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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.ALARM_SUPERVISION_STATE;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.AUTOMATIC_SYNCHRONIZATION;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;

@RunWith(MockitoJUnitRunner.class)
public class NodeSuspendAlarmHandlerTest {

    @InjectMocks
    private NodeSuspendAlarmHandler nodeSuspendAlarmHandler;

    @Mock
    private CurrentServiceStateUpdator currentServiceStateUpdator;

    @Mock
    private AlarmProcessor newAlarmProcessor;

    @Mock
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Mock
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private final Map<String, Object> alarmAttributes = new HashMap<String, Object>();

    @Mock
    private Map<String, Boolean> managedObjectAttributes;

    @Mock
    private AlarmValidator alarmValidator;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Test
    public void testOnEventForNodeSuspendedAlarmWhenSyncIsOngoing() {
        when(alarmRecord.getRecordType()).thenReturn(FMProcessedEventType.NODE_SUSPENDED);
        when(alarmRecord.getFdn()).thenReturn("NetworkElement=1");
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.INDETERMINATE);
        when(correlatedAlarm.getCorrelatedPOId()).thenReturn(2L);
        when(correlatedAlarm.getEventPOId()).thenReturn(2L);
        when(alarmRecord.getCorrelatedPOId()).thenReturn(2L);
        when(alarmRecord.getFmxGenerated()).thenReturn("Test");
        final long milliSeconds = new Date().getTime();
        when(alarmRecord.getEventTime()).thenReturn(new Date(milliSeconds + 1000L));
        when(correlatedAlarm.getEventTime()).thenReturn(new Date(milliSeconds));
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        final AlarmProcessingResponse alarmProcessingresponse = new AlarmProcessingResponse();
        alarmProcessingresponse.setInitiateAlarmSync(false);
        when(correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingresponse);
        when(managedObjectAttributes.get("alarmToBeProcessed")).thenReturn(true);
        managedObjectAttributes.put("alarmToBeProcessed", true);
        when(alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)).thenReturn(true);
        when(currentServiceStateUpdator.updateForNodeSuspendedAlarm(alarmRecord)).thenReturn(managedObjectAttributes);
        final AlarmProcessingResponse alarmProcessingresponse1 = nodeSuspendAlarmHandler.handleAlarm(alarmRecord);
        assertTrue(!alarmProcessingresponse1.isInitiateAlarmSync());

    }

    @Test
    public void testOnEventForClearNodeSuspendedAlarm() throws ParseException {
        when(alarmRecord.getRecordType()).thenReturn(FMProcessedEventType.NODE_SUSPENDED);
        when(alarmRecord.getFdn()).thenReturn("NetworkElement");
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarm);
        when(correlatedAlarm.getEventPOId()).thenReturn(20L);
        when(alarmRecord.getFmxGenerated()).thenReturn("Test");
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.MAJOR);
        when(alarmRecord.getEventTime()).thenReturn(new SimpleDateFormat("yyyyMMddhhmmss").parse("20141303111523"));
        when(correlatedAlarm.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        when(alarmRecord.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        when(alarmRecord.getCeaseOperator()).thenReturn("APSOperator");
        when(alarmRecord.getCorrelatedPOId()).thenReturn(20L);
        when(correlatedAlarm.getRecordType()).thenReturn(FMProcessedEventType.ALARM);
        when(managedObjectAttributes.get(ALARM_SUPERVISION_STATE)).thenReturn(true);
        when(managedObjectAttributes.get(AUTOMATIC_SYNCHRONIZATION)).thenReturn(true);
        when(managedObjectAttributes.get("alarmToBeProcessed")).thenReturn(true);
        managedObjectAttributes.put("alarmToBeProcessed", true);
        when(alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)).thenReturn(true);
        when(currentServiceStateUpdator.updateForNodeSuspendedAlarm(alarmRecord)).thenReturn(managedObjectAttributes);
        final long milliSeconds = new Date().getTime();
        when(alarmRecord.getEventTime()).thenReturn(new Date(milliSeconds + 1000L));
        when(correlatedAlarm.getEventTime()).thenReturn(new Date(milliSeconds));
        when(correlatedAlarm.getVisibility()).thenReturn(true);
        final AlarmProcessingResponse alarmProcessingresponse = new AlarmProcessingResponse();
        alarmProcessingresponse.setInitiateAlarmSync(true);
        when(correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingresponse);
        final AlarmProcessingResponse alarmProcessingresponse1 = nodeSuspendAlarmHandler.handleAlarm(alarmRecord);
        assertTrue(alarmProcessingresponse1.isInitiateAlarmSync());
    }
}

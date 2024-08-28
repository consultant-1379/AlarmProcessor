/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.fm.alarmprocessor.util;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmHandlerBean;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_RECEIVED_TIME;

@RunWith(MockitoJUnitRunner.class)
public class DuplicateAlarmsPurgerTest {

    @InjectMocks
    private DuplicateAlarmsPurger duplicateAlarmsPurger;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private ProcessedAlarmSender processedAlarmSender;

    @Mock
    private AlarmHandlerBean alarmHandlerBean;

    private final List<ProcessedAlarmEvent> correlatedAlarms = new ArrayList<ProcessedAlarmEvent>();

    public void setUp() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setAlarmNumber(1235L);
        alarmRecord.setObjectOfReference("oor");
        alarmRecord.setSpecificProblem("SpecificProblem");
        alarmRecord.setProbableCause("probableCause");
        alarmRecord.setEventType("evenType");
        alarmRecord.setRecordType(FMProcessedEventType.ALARM);
        alarmRecord.setEventPOId(1234L);
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(ALARM_RECEIVED_TIME, "1690875404787");
        alarmRecord.setAdditionalInformation(additionalInformation);
        correlatedAlarms.add(alarmRecord);
    }

    @Test
    public void testRemoveDuplicateAlarmsFromDatabase() {
        setUp();
        when(alarmReader.readAllAttributes(1234L)).thenReturn(new HashMap<String, Object>());
        duplicateAlarmsPurger.removeDuplicateAlarmsFromDatabase(correlatedAlarms);
        verify(openAlarmService).removeAlarm(anyLong(), anyMap());
        verify(alarmAttributesPopulator).updateLastDeliveredTime((ProcessedAlarmEvent) anyObject(), (ProcessedAlarmEvent) anyObject(), anyMap());
    }
}

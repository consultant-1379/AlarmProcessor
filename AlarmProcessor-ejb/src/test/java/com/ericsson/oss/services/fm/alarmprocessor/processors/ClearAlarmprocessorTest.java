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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CLEAR_ALARMS_CACHE;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.cache.Cache;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.models.alarm.cache.ClearAlarmsListWrapper;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmsCountOnNodesMapManager;

@RunWith(MockitoJUnitRunner.class)
public class ClearAlarmprocessorTest {

    @InjectMocks
    private ClearAlarmProcessor clearAlarmProcessor;

    @Mock
    @NamedCache(CLEAR_ALARMS_CACHE)
    private Cache<String, ClearAlarmsListWrapper> clearAlarmsCache;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private FmFunctionMoService fmFunctionMOFacade;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private AlarmAttributesPopulator alarmatAttributesPopulator;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private AlarmsCountOnNodesMapManager alarmsCountOnNodesMapManager;


    @Test
    public void testProcessAlarm(){
        alarmRecord.setEventPOId(1234L);
        when(correlatedAlarm.getRecordType()).thenReturn(FMProcessedEventType.TECHNICIAN_PRESENT);
        clearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(openAlarmService, times(1)).removeAlarm(anyLong(), (Map<String, Object>) anyObject());

    }
    
    @Test
    public void testProcessAlarmForVisibility(){ 
        alarmRecord.setEventPOId(1234L);
        when(correlatedAlarm.getRecordType()).thenReturn(FMProcessedEventType.TECHNICIAN_PRESENT);
        when(correlatedAlarm.getVisibility()).thenReturn(true);
        when(correlatedAlarm.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_ACKNOWLEDGED);
        clearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(openAlarmService, times(1)).removeAlarm(anyLong(), (Map<String, Object>) anyObject());

    }

    @Test
    public void testProcessAlarmForActiveUnacknowledgedAlarmState() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setEventPOId(1234L);
        alarmRecord.setFdn("NetworkElement=LTE01ERBS11");
        final Map<String, String> additionalInformationOfAlarms = new HashMap<>();
        additionalInformationOfAlarms.put("sourceType", "ERBS");
        alarmRecord.setAdditionalInformation(additionalInformationOfAlarms);
        when(correlatedAlarm.getRecordType()).thenReturn(FMProcessedEventType.TECHNICIAN_PRESENT);
        when(correlatedAlarm.getVisibility()).thenReturn(true);
        when(correlatedAlarm.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        clearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(openAlarmService, times(1)).updateAlarm(anyLong(), (Map<String, Object>) anyObject());
        verify(alarmsCountOnNodesMapManager, times(1)).incrementAlarmsCountRequest("LTE01ERBS11", "ERBS");
    }

    @Test
    public void testProcessAlarmForAlarmSuppressedState() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setEventPOId(1234L);
        alarmRecord.setFdn("NetworkElement=LTE01ERBS11");
        final Map<String, String> additionalInformationOfAlarms = new HashMap<>();
        additionalInformationOfAlarms.put("sourceType", "ERBS");
        alarmRecord.setAdditionalInformation(additionalInformationOfAlarms);
        when(correlatedAlarm.getRecordType()).thenReturn(FMProcessedEventType.ALARM_SUPPRESSED_ALARM);
        when(correlatedAlarm.getVisibility()).thenReturn(true);
        when(correlatedAlarm.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        clearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        verify(openAlarmService, times(1)).updateAlarm(anyLong(), (Map<String, Object>) anyObject());
        verify(alarmsCountOnNodesMapManager, times(1)).incrementAlarmsCountRequest("LTE01ERBS11", "ERBS");
    }
}

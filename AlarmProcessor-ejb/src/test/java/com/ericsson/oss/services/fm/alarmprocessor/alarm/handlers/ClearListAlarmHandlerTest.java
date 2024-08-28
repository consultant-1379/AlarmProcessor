/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability.FmDatabaseAvailabilityCacheManager;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class ClearListAlarmHandlerTest {

    @InjectMocks
    private ClearListAlarmHandler clearListAlarmHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private Iterator<PersistenceObject> iterator;

    @Mock
    private PersistenceObject alarmPO;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Mock
    private FmDatabaseAvailabilityCacheManager fmDatabaseAvailabilityCacheManager;

    Map<String, Object> alarmAttributes = new HashMap<String, Object>();

    @Test
    public void testClearListhandler() {
        Mockito.mock(Iterator.class);
        alarmAttributes.put("fdn", "NetworkElement=LTE05ERBS00005");
        alarmAttributes.put("eventPoId", 12345L);
        alarmAttributes.put("presentSeverity", "CLEARED");
        alarmAttributes.put("alarmState", "ACTIVE_ACKNOWLEDGED");
        when(alarmRecord.getObjectOfReference()).thenReturn("ObjectOfReference");
        when(alarmRecord.getFdn()).thenReturn("NetworkElement=LTE05ERBS00005");
        when(alarmReader.readAlarms("NetworkElement=LTE05ERBS00005", false, false)).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true).thenReturn(false);
        when(iterator.next()).thenReturn(alarmPO);
        when(alarmPO.getAllAttributes()).thenReturn(alarmAttributes);
        clearListAlarmHandler.handleAlarm(alarmRecord);
        verify(openAlarmService, times(1)).removeAlarm(anyLong(), anyMap());
        verify(systemRecorder).recordEvent("APS", EventLevel.DETAILED, "NODE  ", "NetworkElement=LTE05ERBS00005",
                "NetworkElement is deleted and all corresponding Open Alarms are to be cleared");
    }
}

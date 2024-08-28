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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CLEAR_ALARMS_CACHE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.INSERT_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_TEXT;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.Cache.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.models.alarm.cache.ClearAlarmsListWrapper;

@RunWith(MockitoJUnitRunner.class)
public class ClearAlarmsCacheManagerTest {

    @InjectMocks
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    @NamedCache(CLEAR_ALARMS_CACHE)
    private Cache<String, ClearAlarmsListWrapper> clearAlarmsCache;

    @Mock
    private ClearAlarmsListWrapper clearAlarmsListWrapper;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private ProcessedAlarmEvent clearEvent;

    @Mock
    private Iterator<Entry<String, ClearAlarmsListWrapper>> iterator;

    @Mock
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private AlarmAttributesPopulator alarmattributesPopulator;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private CurrentServiceStateUpdator currentServiceStateUpdator;

    @Mock
    private FmFunctionMoService fmFunctionMoService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAddAlarmToClearAlarmsCache() {
        final List<ProcessedAlarmEvent> alarmsList = new ArrayList<ProcessedAlarmEvent>();
        when(clearAlarmsCache.get("Test")).thenReturn(clearAlarmsListWrapper);
        when(clearAlarmsListWrapper.getAlarmsList()).thenReturn(alarmsList);
        when(alarmRecord.getFdn()).thenReturn("fdn");
        clearAlarmsCacheManager.addClearAlarm(alarmRecord);
        verify(clearAlarmsCache).put(anyString(), (ClearAlarmsListWrapper) anyObject());
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager#checkAndProcessClearInCache(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testCheckAndProcessClearInCache_ClearAndUnackAlarm() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmProcessingResponse = new AlarmProcessingResponse();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        alarmRecord.setAlarmState(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        alarmRecord.setObjectOfReference("objectOfReference");
        final List<ProcessedAlarmEvent> alarmsList = new ArrayList<ProcessedAlarmEvent>();
        alarmsList.add(alarmRecord);
        when(clearAlarmsCache.get(anyString())).thenReturn(clearAlarmsListWrapper);
        when(clearAlarmsListWrapper.getAlarmsList()).thenReturn(alarmsList);
        when(alarmCorrelator.correlateAlarm(alarmRecord, alarmRecord)).thenReturn(true);
        final Map<String, Object> attributes = new HashMap<String, Object>();
        final Map<String, Object> alarmAttributes = new HashMap<String, Object>();
        alarmAttributes.put(PRESENT_SEVERITY, ProcessedEventSeverity.CLEARED.name());
        when(alarmReader.readAllAttributes(alarmRecord.getCorrelatedPOId())).thenReturn(attributes);
        when(alarmattributesPopulator.populateClearAlarm(alarmRecord)).thenReturn(alarmAttributes);
        clearAlarmsCacheManager.checkAndProcessForClearAlarm(alarmRecord, alarmProcessingResponse);
        verify(openAlarmService).updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager#checkAndProcessClearInCache(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testCheckAndProcessClearInCache_ClearAndAckAlarm() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmProcessingResponse = new AlarmProcessingResponse();
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        alarmRecord.setObjectOfReference("objectOfReference");
        alarmRecord.setAlarmState(ProcessedEventState.ACTIVE_ACKNOWLEDGED);
        alarmRecord.setEventPOId(1234L);
        final List<ProcessedAlarmEvent> alarmsList = new ArrayList<ProcessedAlarmEvent>();
        alarmsList.add(alarmRecord);
        clearEvent = new ProcessedAlarmEvent();
        clearEvent.setCorrelatedPOId(1234L);
        when(clearAlarmsCache.get(anyString())).thenReturn(clearAlarmsListWrapper);
        when(clearAlarmsListWrapper.getAlarmsList()).thenReturn(alarmsList);
        when(alarmCorrelator.correlateAlarm(alarmRecord, alarmRecord)).thenReturn(true);
        final List<String> outputAttributes = new ArrayList<String>();
        outputAttributes.add(PROBLEM_TEXT);
        outputAttributes.add(INSERT_TIME);
        final Map<String, Object> attributes = new HashMap<String, Object>();
        final Map<String, Object> alarmAttributes = new HashMap<String, Object>();
        clearAlarmsCacheManager.checkAndProcessForClearAlarm(alarmRecord, alarmProcessingResponse);
        when(alarmReader.readAttributes(alarmRecord.getCorrelatedPOId(), outputAttributes)).thenReturn(attributes);
        when(alarmattributesPopulator.populateClearAlarm(alarmRecord)).thenReturn(alarmAttributes);
        verify(openAlarmService).removeAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
    }

    /**
     * Test method for {@link com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager#getFdnsFromClearAlarmsCache()}.
     */
    @Test
    public void testGetFdnsFromClearAlarmsCache() {
        alarmRecord = new ProcessedAlarmEvent();
        when(clearAlarmsCache.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false);
        final Entry<String, ClearAlarmsListWrapper> entry = Mockito.mock(Entry.class);
        when(iterator.next()).thenReturn(entry);
        when(entry.getValue()).thenReturn(clearAlarmsListWrapper);
        final List<ProcessedAlarmEvent> alarmsList = new ArrayList<ProcessedAlarmEvent>();
        alarmRecord.setFdn("fdn");
        alarmsList.add(alarmRecord);
        when(clearAlarmsListWrapper.getAlarmsList()).thenReturn(alarmsList);
        final Set<String> networkElementFdns = clearAlarmsCacheManager.getFdnsFromCache();
        assertTrue(networkElementFdns.contains("fdn"));
    }

}

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

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.LAST_DELIVERED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;

@RunWith(MockitoJUnitRunner.class)
public class SupervisionSwitchoverHandlerTest {

    @InjectMocks
    private SupervisionSwitchoverHandler supervisionSwitchoverHandler;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private Iterator<PersistenceObject> poIterator;

    @Mock
    private PersistenceObject persistenceObject;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private CurrentServiceStateUpdator currentServiceStateUpdater;

    final Map<String, Object> openAlarmAttributes = new HashMap<String, Object>();

    public void setUp() {

        openAlarmAttributes.put(PRESENT_SEVERITY, "CRITICAL");
        openAlarmAttributes.put(PREVIOUS_SEVERITY, "UNDEFINED");
        openAlarmAttributes.put(ALARM_STATE, ProcessedEventState.ACTIVE_UNACKNOWLEDGED.toString());
        openAlarmAttributes.put(FDN, "LTE01");
        openAlarmAttributes.put(RECORD_TYPE, "HEARTBEAT_ALARM");
        openAlarmAttributes.put(LAST_DELIVERED, System.currentTimeMillis());
        openAlarmAttributes.put(VISIBILITY, true);

        when(openAlarmService.getOpenAlarmPO(anyMap(), anyMap())).thenReturn(poIterator);
        when(poIterator.hasNext()).thenReturn(true).thenReturn(false);
        when(poIterator.next()).thenReturn(persistenceObject);
        when(persistenceObject.getAllAttributes()).thenReturn(openAlarmAttributes);
    }

    @Test
    public void testHandleAlarm_visibilityTRUE_ACTIVE_UNACKNOWLEDGED() {
        setUp();
        final ProcessedAlarmEvent alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("LTE01");
        when(persistenceObject.getPoId()).thenReturn(1234L);
        when(configParametersListener.getAlarmThresholdInterval()).thenReturn(1364645L);
        when(configParametersListener.getClearAlarmDelayToQueue()).thenReturn(646465465L);
        supervisionSwitchoverHandler.handleAlarm(alarmRecord);
        verify(openAlarmService, times(1)).updateAlarm(anyLong(), anyMap());

    }
 @Test
    public void testHandleAlarm1_visibilityTRUE_ACTIVE_UNACKNOWLEDGED() {
        setUp();
        final ProcessedAlarmEvent alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("LTE01");
        alarmRecord.setSyncState(true);
        when(persistenceObject.getPoId()).thenReturn(1234L);
        when(configParametersListener.getAlarmThresholdInterval()).thenReturn(1364645L);
        when(configParametersListener.getClearAlarmDelayToQueue()).thenReturn(646465465L);
        supervisionSwitchoverHandler.handleAlarm(alarmRecord);
        verify(openAlarmService, times(1)).updateAlarm(anyLong(), anyMap());

    }

    @Test
    public void testHandleAlarm_visibilityTRUE_ACTIVE_ACKNOWLEDGED() {
        setUp();
        final ProcessedAlarmEvent alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("LTE01");
        openAlarmAttributes.put(ALARM_STATE, ProcessedEventState.ACTIVE_ACKNOWLEDGED.toString());
        when(persistenceObject.getPoId()).thenReturn(1234L);
        when(configParametersListener.getAlarmThresholdInterval()).thenReturn(1364645L);
        when(configParametersListener.getClearAlarmDelayToQueue()).thenReturn(646465465L);
        supervisionSwitchoverHandler.handleAlarm(alarmRecord);
        verify(openAlarmService, times(1)).removeAlarm(anyLong(), anyMap());
    }

    @Test
    public void testHandleAlarm_visibilityFALSE_ACTIVE_UNACKNOWLEDGED() {
        setUp();
        final ProcessedAlarmEvent alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("LTE01");
        alarmRecord.setSyncState(false);
        openAlarmAttributes.put(ALARM_STATE, ProcessedEventState.ACTIVE_UNACKNOWLEDGED.toString());
        openAlarmAttributes.put(VISIBILITY, false);
        when(persistenceObject.getPoId()).thenReturn(1234L);
        when(configParametersListener.getAlarmThresholdInterval()).thenReturn(1364645L);
        when(configParametersListener.getClearAlarmDelayToQueue()).thenReturn(646465465L);
        supervisionSwitchoverHandler.handleAlarm(alarmRecord);
        verify(openAlarmService, times(1)).removeAlarm(anyLong(), anyMap());
    }
}

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.staging;

import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.getKeyFromAlarm;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NORMAL_PROC;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import javax.ejb.TimerConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class AlarmStagingHandlerTest {

    @InjectMocks
    private AlarmStagingHandler alarmStagingHandler;

    @Mock
    private TimerConfig timerConfig;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private TransientAlarmStagingCacheManager cacheManager;

    @Mock
    private AlarmStagingTimer alarmStagingTimer;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    private ProcessedAlarmEvent alarmRecord;
    private ProcessedAlarmEvent correlatedAlarm;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setObjectOfReference("Node1");
        alarmRecord.setAlarmNumber(1234L);
    }

    @Test
    public void testCheckAndStageAlarm_Staged() {
        correlatedAlarm = new ProcessedAlarmEvent();
        correlatedAlarm.setLastUpdatedTime(new Date(System.currentTimeMillis()));
        correlatedAlarm.setEventPOId(1234L);
        correlatedAlarm.setVisibility(false);
        correlatedAlarm.setProcessingType(NORMAL_PROC);
        when(configParametersListener.getTransientAlarmStagingThresholdTime()).thenReturn(300);
        assertTrue(alarmStagingHandler.checkAndStageAlarm(alarmRecord, correlatedAlarm));
        final String key = getKeyFromAlarm(alarmRecord);
        verify(cacheManager, times(1)).stageTransientAlarm(key, alarmRecord);
    }

    @Test
    public void testCheckAndStageAlarm_NotStaged_Visibility_True() {
        correlatedAlarm = new ProcessedAlarmEvent();
        correlatedAlarm.setLastUpdatedTime(new Date(System.currentTimeMillis()));
        correlatedAlarm.setEventPOId(1234L);
        correlatedAlarm.setVisibility(true);
        correlatedAlarm.setProcessingType(NORMAL_PROC);
        when(configParametersListener.getTransientAlarmStagingThresholdTime()).thenReturn(300);
        assertFalse(alarmStagingHandler.checkAndStageAlarm(alarmRecord, correlatedAlarm));
    }

    @Test
    public void testCheckAndStageAlarm_NotStaged() {
        correlatedAlarm = new ProcessedAlarmEvent();
        correlatedAlarm.setLastUpdatedTime(new Date(System.currentTimeMillis() - 400));
        correlatedAlarm.setEventPOId(1234L);
        correlatedAlarm.setVisibility(false);
        correlatedAlarm.setProcessingType(NORMAL_PROC);
        when(configParametersListener.getTransientAlarmStagingThresholdTime()).thenReturn(300);
        assertFalse(alarmStagingHandler.checkAndStageAlarm(alarmRecord, correlatedAlarm));
    }

    @Test
    public void testCheckAndStageAlarm_NotLastUpdatedTime() {
        correlatedAlarm = new ProcessedAlarmEvent();
        correlatedAlarm.setLastUpdatedTime(null);
        correlatedAlarm.setEventPOId(1234L);
        correlatedAlarm.setVisibility(false);
        correlatedAlarm.setProcessingType(NORMAL_PROC);
        when(configParametersListener.getTransientAlarmStagingThresholdTime()).thenReturn(300);
        assertFalse(alarmStagingHandler.checkAndStageAlarm(alarmRecord, correlatedAlarm));
    }
}

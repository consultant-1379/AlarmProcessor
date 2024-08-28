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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmHandlerBean;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class AlarmStagingTimerTest {

    @InjectMocks
    private AlarmStagingTimer alarmStagingTimer;

    @Mock
    private TimerService timerService;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private TransientAlarmStagingCacheManager cacheManager;

    @Mock
    private AlarmHandlerBean alarmHandlerBean;

    @Mock
    private ProcessedAlarmSender processedAlarmSender;

    @Mock
    private ProcessedAlarmEvent alarm;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private AlarmProcessingResponse setUp() {
        final AlarmProcessingResponse response = new AlarmProcessingResponse();
        final List<ProcessedAlarmEvent> stagedAlarms = new ArrayList<ProcessedAlarmEvent>();
        stagedAlarms.add(alarm);
        when(cacheManager.getStagedAlarms(Matchers.anyString())).thenReturn(stagedAlarms);
        when(cacheManager.unstageTransientAlarm(Matchers.anyString())).thenReturn(alarm);

        alarmStagingTimer.startTimer(300, "Node1@@@1234");
        return response;
    }

    @Test
    public void testStartTimer() {
        final int duration = 300;
        alarmStagingTimer.startTimer(duration, "1234");
        verify(timerService, times(1)).createSingleActionTimer(Matchers.anyInt(), (TimerConfig) Matchers.anyObject());
    }

    @Test
    public void testTimeout_Exception() {
        when(cacheManager.getStagedAlarms(Matchers.anyString())).thenReturn(null);
        when(cacheManager.unstageTransientAlarm(Matchers.anyString())).thenThrow(new RuntimeException());

        alarmStagingTimer.timeOut((Timer) Matchers.anyObject());
        verify(configParametersListener, times(1)).getTransientAlarmStagingThresholdTime();
        verify(timerService, times(1)).createSingleActionTimer(Matchers.anyInt(), (TimerConfig) Matchers.anyObject());
    }

    @Test
    public void testTimeout_Normal() {
        final AlarmProcessingResponse response = setUp();
        when(alarmHandlerBean.processAlarm(alarm)).thenReturn(response);

        alarmStagingTimer.timeOut((Timer) Matchers.anyObject());
        verify(alarmHandlerBean, times(1)).processAlarm(alarm);
        verify(processedAlarmSender, times(1)).sendAlarms(response, null);
    }

    @Test
    public void testTimeout_Repeated() {
        final AlarmProcessingResponse response = setUp();
        when(alarmHandlerBean.processAlarm(alarm)).thenReturn(response);

        alarmStagingTimer.timeOut((Timer) Matchers.anyObject());
        verify(alarmHandlerBean, times(1)).processAlarm(alarm);
        verify(processedAlarmSender, times(1)).sendAlarms(response, null);
    }

    @Test
    public void testTimeout_Clear() {
        setUp();

        alarmStagingTimer.timeOut((Timer) Matchers.anyObject());
        verify(alarmHandlerBean, times(1)).processAlarm((ProcessedAlarmEvent) Matchers.anyObject());
        verify(processedAlarmSender, times(1)).sendAlarms((AlarmProcessingResponse) Matchers.anyObject(), Matchers.anyString());
    }
}

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

package com.ericsson.oss.services.fm.alarmprocessor.processing.analyser;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.jms.Connection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.UnProcessedAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.UnProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder.ProcessingState;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.common.listeners.ListenersInitializer;
import com.ericsson.oss.services.fm.common.targettype.handler.TargetTypeHandler;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class AlarmProcessingAnalyserTest {

    @InjectMocks
    private AlarmProcessingAnalyser alarmProcessingAnalyser;

    @Mock
    private TimerService timerService;

    @Mock
    private Timer alarmProcessingRateCheckTimer;

    @Mock
    private ActiveThreadsHolder threadsHolder;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private TargetTypeHandler targetTypeHandler;

    @Mock
    private AlarmValidator alarmValidator;

    @Mock
    private UnProcessedAlarmSender unProcessedAlarmSender;

    @Mock
    private UnProcessedAlarmHandler unProcessedAlarmHandler;

    @Mock
    private List<EventNotification> events;

    @Mock
    private EventNotification event;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private ListenersInitializer listenersInitializer;

    @Mock
    private Connection secondaryConnection;

    @Mock
    private BufferedData data;

    @Mock
    private Iterator<EventNotification> iterator;

    @Test
    public void testCreateTimer() {
        when(configParametersListener.getThreadStateCheckInterval()).thenReturn(10);
        alarmProcessingAnalyser.createTimer();
    }

    @Test
    public void testCheckThreadState_success() {
        final Map<Long, BufferedData> activeThreadsAndAlarms = new HashMap<Long, BufferedData>();
        activeThreadsAndAlarms.put(1234L, new BufferedData(System.currentTimeMillis(), false, events));

        when(configParametersListener.getThresholdHoldTime()).thenReturn(10);
        when(threadsHolder.getActiveThreadsAndAlarms()).thenReturn(activeThreadsAndAlarms);
        when(threadsHolder.getProcessingState()).thenReturn(ProcessingState.RUNNING_NORMALLY);

        alarmProcessingAnalyser.analyzeProcessingThreads();
        verify(threadsHolder, times(1)).size();
    }

    @Test
    public void testCheckThreadState_success_hard_sendEventsNBI() {
        final Map<Long, BufferedData> activeThreadsAndAlarms = new HashMap<Long, BufferedData>();
        activeThreadsAndAlarms.put(1234L, new BufferedData(System.currentTimeMillis() - 4000, false, events));

        when(configParametersListener.getThresholdHoldTime()).thenReturn(1);
        when(threadsHolder.getActiveThreadsAndAlarms()).thenReturn(activeThreadsAndAlarms);
        when(threadsHolder.getProcessingState()).thenReturn(ProcessingState.SOFT_THRESHOLD_CROSSED);
        when(threadsHolder.get(1234L)).thenReturn(data);
        when(data.getEvents()).thenReturn(events);

        when(events.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false);

        final EventNotification eventNotification = new EventNotification();
        eventNotification.setRecordType("NODE_SUSPENDED");
        eventNotification.setFmxGenerated("FMX_PROCESSED");
        final Map<String, String> additionalAttributes = new HashMap<String, String>();
        additionalAttributes.put(FDN, "TestNode1");
        eventNotification.setAdditionalAttributes(additionalAttributes);

        when(iterator.next()).thenReturn(eventNotification);
        when(targetTypeHandler.get("TestNode1")).thenReturn("ERBS");

        when(alarmValidator.isAlarmToBeHandled((ProcessedAlarmEvent) Matchers.anyObject())).thenReturn(true);

        alarmProcessingAnalyser.analyzeProcessingThreads();
        verify(unProcessedAlarmHandler, times(1)).prepareAndSendUnprocessedAlarms((List<EventNotification>) Matchers.anyObject(),
                Matchers.anyBoolean(), Matchers.anyLong(), Matchers.anyString());
    }

    @Test
    public void testCheckThreadState_failure() {
        final Map<Long, BufferedData> activeThreadsAndAlarms = new HashMap<Long, BufferedData>();
        activeThreadsAndAlarms.put(1234L, new BufferedData(System.currentTimeMillis() - 4000, false, events));

        when(configParametersListener.getThresholdHoldTime()).thenReturn(1);
        when(threadsHolder.getActiveThreadsAndAlarms()).thenReturn(activeThreadsAndAlarms);
        when(threadsHolder.getProcessingState()).thenReturn(ProcessingState.RUNNING_NORMALLY);
        when(listenersInitializer.startListening("jms:queue/fmalarmqueue", false, configParametersListener.getSecondaryThreadsCount())).thenReturn(
                secondaryConnection);

        alarmProcessingAnalyser.analyzeProcessingThreads();
        verify(threadsHolder, times(1)).setSecondaryConnection(secondaryConnection);
    }

    @Test
    public void testCancelTimer() {
        alarmProcessingAnalyser.cancelTimer();
        verify(alarmProcessingRateCheckTimer, times(1)).cancel();
    }

    @Test
    public void testRecreateTimerWithNewInterval() {
        alarmProcessingAnalyser.recreateTimerWithNewInterval(20);
        verify(alarmProcessingRateCheckTimer, times(1)).cancel();
    }
}

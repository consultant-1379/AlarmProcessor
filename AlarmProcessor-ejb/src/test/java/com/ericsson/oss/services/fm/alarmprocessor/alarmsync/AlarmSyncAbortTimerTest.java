package com.ericsson.oss.services.fm.alarmprocessor.alarmsync;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.AlarmSyncAbortTimer;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;

@RunWith(MockitoJUnitRunner.class)
public class AlarmSyncAbortTimerTest {

    @InjectMocks
    private AlarmSyncAbortTimer timeMonitor;

    @Mock
    private FmFunctionMoService  fmFunctionMOFacade;

    @Mock
    private TimerConfig timerConfig;

    @Mock
    private EventSender<EventNotificationBatch> eventSender;

    @Mock
    private MembershipChangeProcessor clusterStateProvider;

    @Resource
    @Mock
    private TimerService timerService;

    @Mock
    private Timer ongoingSyncTimer;

    @Mock
    private ConfigParametersListener configurationProvider;

    private final List<String> fmFunctionFDNList = new ArrayList<String>();

    @Test
    public void checkDurationAndGenerateSyncAbortTest() {
        when(clusterStateProvider.getMasterState()).thenReturn(true);
        when(configurationProvider.getTimerIntervalToDiscardOngoingAlarmSync()).thenReturn(3);
        fmFunctionFDNList.add("NetworkElement=1");
        when(fmFunctionMOFacade.readNodeListForLongOngoingSync(configurationProvider.getTimerIntervalToDiscardOngoingAlarmSync())).thenReturn(
                fmFunctionFDNList);
        timeMonitor.checkDurationAndGenerateSyncAbort();
        verify(eventSender, times(0)).send((EventNotificationBatch) anyObject());
    }

    @Test
    public void checkDurationAndGenerateSyncAbortTestWithException() {
        when(clusterStateProvider.getMasterState()).thenReturn(true);
        when(configurationProvider.getTimerIntervalToDiscardOngoingAlarmSync()).thenReturn(3);
        when(fmFunctionMOFacade.readNodeListForLongOngoingSync(configurationProvider.getTimerIntervalToDiscardOngoingAlarmSync())).thenReturn(
                fmFunctionFDNList);
        timeMonitor.checkDurationAndGenerateSyncAbort();
        verify(eventSender, times(0)).send((EventNotificationBatch) anyObject());
    }

}

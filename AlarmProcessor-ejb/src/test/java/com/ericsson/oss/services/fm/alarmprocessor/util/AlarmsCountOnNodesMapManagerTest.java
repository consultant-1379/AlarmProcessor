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

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;


@RunWith(MockitoJUnitRunner.class)
public class AlarmsCountOnNodesMapManagerTest {

    @InjectMocks
    private AlarmsCountOnNodesMapManager alarmsCountOnNodesMapManager;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private Timer loggingNodeTypesHashMapTimer;

    @Mock
    private TimerConfig timerConfig;

    @Resource
    @Mock
    private TimerService timerService;

    @Mock
    private SystemRecorder systemRecorder;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testincrementAlarmsCountRequest() {
        alarmRecord = new ProcessedAlarmEvent();
        final Map<String, String> additionalInformationOfAlarms = new HashMap<>();
        additionalInformationOfAlarms.put("sourceType", "ERBS");
        alarmRecord.setFdn("APS_Groovy_003");
        alarmRecord.setAdditionalInformation(additionalInformationOfAlarms);
        alarmsCountOnNodesMapManager.incrementAlarmsCountRequest(alarmRecord.getFdn(), alarmRecord.getAdditionalInformation().get("sourceType"));
    }

    @Test
    public void testincrementAlarmsCountRequestForSameNode() {
        alarmRecord = new ProcessedAlarmEvent();
        final Map<String, String> additionalInformationOfAlarms = new HashMap<>();
        final Map<String, Object> alarmsCountForEachNodeName = new ConcurrentHashMap<>();
        final Map<String, Object> alarmsCountForEachNodeType = new ConcurrentHashMap<>();
        additionalInformationOfAlarms.put("sourceType", "ERBS");
        alarmRecord.setFdn("APS_Groovy_003");
        alarmRecord.setAdditionalInformation(additionalInformationOfAlarms);
        alarmsCountForEachNodeName.put("APS_Groovy_003", 1);
        alarmsCountForEachNodeType.put("ERBS", 1);
        alarmsCountOnNodesMapManager.incrementAlarmsCountRequest(alarmRecord.getFdn(), alarmRecord.getAdditionalInformation().get("sourceType"));
    }

    @Test
    public void testHashMapClearedAtMidnight() throws InterruptedException {
        alarmRecord = new ProcessedAlarmEvent();
        final Map<String, String> additionalInformationOfAlarms = new HashMap<>();
        final Map<String, Object> alarmsCountForEachNodeName = new ConcurrentHashMap<>();
        final Map<String, Object> alarmsCountForEachNodeType = new ConcurrentHashMap<>();
        additionalInformationOfAlarms.put("sourceType", "ERBS");
        alarmRecord.setFdn("APS_Groovy_003");
        alarmRecord.setAdditionalInformation(additionalInformationOfAlarms);
        alarmsCountForEachNodeName.put("APS_Groovy_003", 1);
        alarmsCountForEachNodeType.put("ERBS", 1);
        alarmsCountOnNodesMapManager.scheduleTasks();
    }

    @Test
    public void testTimeUntilNextMidnight() {
        final long timeUntilMidnight = alarmsCountOnNodesMapManager.getTimeUntilNextMidnight();
        assertEquals(true, timeUntilMidnight >= 0);
    }

    @Test
    public void testStartTimerToLogHashMaps() {
        final long duration = 15;
        alarmsCountOnNodesMapManager.startTimerToLogHashMaps(duration);
        verify(timerService, times(1)).createIntervalTimer(Matchers.anyLong(), Matchers.anyLong(), (TimerConfig) Matchers.anyObject());
    }

    @Test
    public void handleTimeoutTest() {
        alarmRecord = new ProcessedAlarmEvent();
        final Map<String, String> additionalInformationOfAlarms = new HashMap<>();
        final Map<String, Object> alarmsCountForEachNodeType = new ConcurrentHashMap<>();
        additionalInformationOfAlarms.put("sourceType", "ERBS");
        alarmRecord.setFdn("APS_Groovy_003");
        alarmRecord.setAdditionalInformation(additionalInformationOfAlarms);
        alarmsCountForEachNodeType.put("ERBS", 1);
        alarmsCountOnNodesMapManager.handleTimeout();
        verify(systemRecorder, times(1)).recordEventData(Matchers.anyString(), Matchers.anyMap());
    }
}
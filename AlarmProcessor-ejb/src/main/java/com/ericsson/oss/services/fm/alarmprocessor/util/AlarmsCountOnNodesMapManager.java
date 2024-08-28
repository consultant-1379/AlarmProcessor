/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.util;


import java.time.*;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.NODE_TYPE_ALARM_COUNT;

/**
* Class manages actions related to cache. Cache holding the count of alarms on each node and also each node type.
*/
@ApplicationScoped
@Startup
@Singleton
public class AlarmsCountOnNodesMapManager {

    @Resource
    private TimerService timerService;

    @Inject
    private SystemRecorder systemRecorder;

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmsCountOnNodesMapManager.class);
    private static Map<String, Integer> alarmsCountForEachNodeName = new ConcurrentHashMap<>();
    private static Map<String, Integer> alarmsCountForEachNodeType = new ConcurrentHashMap<>();
    private static Map<String, Object> alarmsCountForAllNodeTypes = new ConcurrentHashMap<>();

    // Get the server's time zone
    private ZoneId serverTimeZone = ZoneId.systemDefault();

    // Schedule task to clear HashMap at next midnight server time
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void incrementAlarmsCountRequest (final String nodeName, final String nodeType) {
        final boolean isNodeNameExists = alarmsCountForEachNodeName.containsKey(nodeName);
        if (isNodeNameExists) {
            alarmsCountForEachNodeName.put(nodeName, alarmsCountForEachNodeName.get(nodeName) + 1);
        } else {
            alarmsCountForEachNodeName.put(nodeName, 1);
        }
        final boolean isNodeTypeExists = alarmsCountForEachNodeType.containsKey(nodeType);
        if (isNodeTypeExists) {
            alarmsCountForEachNodeType.put(nodeType, alarmsCountForEachNodeType.get(nodeType) + 1);
        } else {
           alarmsCountForEachNodeType.put(nodeType, 1);
        }
    }

    @PostConstruct
    public void scheduleTasks() {
        scheduleMidnightHashMapClearing();
        startTimerToLogHashMaps(15);
    }

    //Method to schedule task to clear HashMap at midnight server time
    private void scheduleMidnightHashMapClearing() {
        scheduler.scheduleAtFixedRate(() -> {
            LOGGER.debug("Node Type Hash Map before clearing: {}", alarmsCountForEachNodeType);
            alarmsCountForEachNodeName.clear();
            alarmsCountForEachNodeType.clear();
            LOGGER.info("Cleared the node type hashmap for the day");
        }, getTimeUntilNextMidnight(), 24 * 60 * 60L, TimeUnit.SECONDS);
    }

    //Method to calculate the delay until next midnight server time
    public long getTimeUntilNextMidnight() {
        ZonedDateTime now = ZonedDateTime.now(serverTimeZone);
        ZonedDateTime nextMidnight = now.toLocalDate().atStartOfDay(serverTimeZone).plusDays(1);
        return Duration.between(now, nextMidnight).getSeconds();
    }

    public void startTimerToLogHashMaps(final long timeInterval) {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createIntervalTimer(timeInterval * 60 * 1000L, timeInterval * 60 * 1000L, timerConfig);
        LOGGER.info("Timer to log the hash maps has been started with time interval {} ", timeInterval);
    }
    
    @Timeout
    public void handleTimeout() {
        final List<Map<String, Object>> nodeTypesList = new ArrayList<>();
        for (Map.Entry<String, Integer> set : alarmsCountForEachNodeType.entrySet()) {
            Map<String,Object> nodeTypesAndTheirAlarmCounts = new HashMap<>();
            nodeTypesAndTheirAlarmCounts.put("Type", set.getKey());
            nodeTypesAndTheirAlarmCounts.put("Count", set.getValue());
            nodeTypesList.add(nodeTypesAndTheirAlarmCounts);
        }
        alarmsCountForAllNodeTypes.put("AlarmsCountForEachNodeType", nodeTypesList);
        systemRecorder.recordEventData(NODE_TYPE_ALARM_COUNT, alarmsCountForAllNodeTypes);
    }
}
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Connection;

import com.ericsson.oss.services.fm.common.connector.JMSQueueConnector;
import com.ericsson.oss.services.fm.common.connector.JmsConsumerInfo;

/**
 * This is the class holding the buffer of Active threads and corresponding Alarms in APS.
 */
@ApplicationScoped
public class ActiveThreadsHolder {

    private volatile Map<Long, BufferedData> activeThreadsAndAlarms = new ConcurrentHashMap<Long, BufferedData>();
    private volatile List<JmsConsumerInfo> secondaryConnections = new CopyOnWriteArrayList<JmsConsumerInfo>();

    @Inject
    private JMSQueueConnector jmsQueueConnector;

    private Connection secondaryConnection;

    /**
     * ENUM for processing state of alarm processing threads.
     */
    public enum ProcessingState {
        SOFT_THRESHOLD_CROSSED, HARD_THRESHOLD_CROSSED, RUNNING_NORMALLY
    }

    private volatile ProcessingState processingState = ProcessingState.RUNNING_NORMALLY;

    public Map<Long, BufferedData> getActiveThreadsAndAlarms() {
        return new HashMap<Long, BufferedData>(activeThreadsAndAlarms);
    }

    public void put(final Long key, final BufferedData value) {
        activeThreadsAndAlarms.put(key, value);
    }

    public BufferedData get(final Long key) {
        return activeThreadsAndAlarms.get(key);
    }

    public int size() {
        return activeThreadsAndAlarms.size();
    }

    public void remove(final Long threadId) {
        activeThreadsAndAlarms.remove(threadId);
    }

    public void setActiveThreadsAndAlarms(final Map<Long, BufferedData> activeThreadsAndAlarms) {
        this.activeThreadsAndAlarms = activeThreadsAndAlarms;
    }

    public ProcessingState getProcessingState() {
        return processingState;
    }

    public void setProcessingState(final ProcessingState processingState) {
        this.processingState = processingState;
    }

    public void setSecondaryConnections(final List<JmsConsumerInfo> connections) {
        this.secondaryConnections = connections;
    }

    public List<JmsConsumerInfo> getSecondaryConnections() {
        return secondaryConnections;
    }

    public Connection getSecondaryConnection() {
        return secondaryConnection;
    }

    public void setSecondaryConnection(final Connection secondaryConnection) {
        this.secondaryConnection = secondaryConnection;
    }

    public void stopConsumers() {
        jmsQueueConnector.destroyQueueConnection(secondaryConnections, secondaryConnection);
    }
}

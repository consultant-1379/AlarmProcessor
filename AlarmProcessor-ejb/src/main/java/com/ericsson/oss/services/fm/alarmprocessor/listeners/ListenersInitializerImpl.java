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

package com.ericsson.oss.services.fm.alarmprocessor.listeners;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder;
import com.ericsson.oss.services.fm.common.connector.JMSQueueConnector;
import com.ericsson.oss.services.fm.common.connector.JmsConsumerInfo;
import com.ericsson.oss.services.fm.common.listeners.ListenersInitializer;

/**
 * Class responsible for starting/stopping the MessageListeners on the given JMS channels.
 */
@Stateless
@Local(ListenersInitializer.class)
public class ListenersInitializerImpl implements ListenersInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenersInitializerImpl.class);

    private final List<JmsConsumerInfo> primaryConsumersList = new CopyOnWriteArrayList<>();

    @Inject
    private JMSQueueConnector jmsQueueConnector;

    @Inject
    private EventNotificationBatchListener listener;

    @Inject
    private FmAlarmsListener fmAlarmsListener;

    @Inject
    private ActiveThreadsHolder threadsHolder;

    private Connection queueConnection;

    /**
     * Activates message listener - actually it registers bean instance as a MessageListener for the provided jms channel. If the number of consumers
     * supplied is null, reads the consumers from JVM properties.
     *
     * @param jndiChannelName
     *            The queue name for which listeners to be activated
     * @param primaryConsumers
     *            If the listeners are primary consumers
     * @param numberOfConsumers
     *            The number of listeners to be started
     * @return jms connection
     */
    @Override
    public Connection startListening(final String jndiChannelName, final boolean primaryConsumers, final Integer numberOfConsumers) {
        if (primaryConsumers) {
            if (isActive()) {
                LOGGER.warn("Primary message observers for {} are already active! returning now", jndiChannelName);
                return null;
            }
        } else {
            if (!threadsHolder.getSecondaryConnections().isEmpty()) {
                LOGGER.warn("Secondary message observers for {} are already active! returning now", jndiChannelName);
                return null;
            }
        }

        try {
            if (numberOfConsumers == null) {
                queueConnection = jmsQueueConnector.startConnection();
                if (queueConnection != null) {
                    final List<JmsConsumerInfo> consumers = jmsQueueConnector.createQueueConsumers(jndiChannelName, fmAlarmsListener,
                            numberOfConsumers, queueConnection);
                    primaryConsumersList.addAll(consumers);
                    LOGGER.info("Primary consumers of size {} are registered successfully ", primaryConsumersList.size());
                } else {
                    LOGGER.warn("Primary sessions were not started, hence consumers also not started");
                    return null;
                }
                return queueConnection;
            } else {
                final Connection connection = jmsQueueConnector.startConnection();
                if (connection != null) {
                    final List<JmsConsumerInfo> connections = jmsQueueConnector.createQueueConsumers(jndiChannelName, listener, numberOfConsumers,
                            connection);
                    threadsHolder.setSecondaryConnections(connections);
                    LOGGER.info("Secondary consumers of size {} are registered successfully", threadsHolder.getSecondaryConnections().size());
                } else {
                    LOGGER.warn("Secondary sessions were not started, hence consumers also not started");
                    return null;
                }
                return connection;
            }
        } catch (final Exception exception) {
            LOGGER.error("Listeners not started for {} due to the exception  :: {} ", jndiChannelName, exception.getMessage());
            LOGGER.debug("Listeners not started for {} due to the exception  :: ", jndiChannelName, exception);
            return null;
        }
    }

    @Override
    public boolean startListening(final String jndiChannelName) {
        // Not supported
        return false;
    }

    /**
     * Pre-destroy method for stopping all the active consumers.
     */
    @PreDestroy
    public void preDestroy() {
        try {
            LOGGER.info("Predestroy method invoked in ListenersInitializerImpl");
            jmsQueueConnector.destroyQueueConnection(threadsHolder.getSecondaryConnections(), queueConnection);
            jmsQueueConnector.destroyQueueConnection(primaryConsumersList, queueConnection);
        } catch (final Exception exception) {
            LOGGER.error("Exception caught while destroying Queue connection {}", exception.getMessage());
            LOGGER.info("Exception caught while destroying Queue connection", exception);
        }
    }

    /**
     * Checks whether consumers are created or not.
     */
    public boolean isActive() {
        return !primaryConsumersList.isEmpty();
    }

}

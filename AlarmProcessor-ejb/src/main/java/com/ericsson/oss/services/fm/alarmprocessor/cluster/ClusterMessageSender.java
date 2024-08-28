/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.cluster;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CDI Event sender which will fire events whenever node is added or removed.
 */
@ApplicationScoped
public class ClusterMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterMessageSender.class);

    @Inject
    private AlarmProcessorCluster clusterSingleton;

    /**
     * Send a Serializable cluster message.
     * 
     * @param Serializable
     *            message the cluster message to be sent
     */
    public void sendClusterMessage(final Serializable message) {
        if (message != null) {
            LOGGER.info("The message in sendClusterMessage is: {}", message);
            clusterSingleton.sendClusterMessage(message);
        }
    }
}
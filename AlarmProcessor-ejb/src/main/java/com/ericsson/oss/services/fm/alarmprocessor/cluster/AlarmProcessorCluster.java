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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_PROCESSOR_CLUSTER_GROUP;

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cluster.classic.ServiceClusterBean;

/**
 * Class responsible for joining AlarmProcessor instances to the cluster,AlarmProcessorClusterGroup.
 */
@Singleton
@Startup
public class AlarmProcessorCluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmProcessorCluster.class);

    private final ServiceClusterBean serviceClusterBean = new ServiceClusterBean(ALARM_PROCESSOR_CLUSTER_GROUP);

    @Inject
    private AlarmProcessorMembershipChangeListener membershipChangeListener;

    @EJB
    private AlarmProcessorHealthCheck alarmProcessorHealthCheck;

    @PostConstruct
    public void initialize() {
        joinCluster();
        alarmProcessorHealthCheck.setHealthState(true);
    }

    @PreDestroy
    public void preDestroy() {
        leaveCluster();
    }

    /**
     * Join the current member to AlarmProcessorClusterGroup.
     */
    void joinCluster() {
        if (!serviceClusterBean.isClusterMember()) {
            serviceClusterBean.joinCluster(membershipChangeListener);
            LOGGER.info("AlarmProcessor has successfully joined AlarmProcessorClusterGroup");
        }
    }

    /**
     * Leave from AlarmProcessorClusterGroup cluster.
     */
    public void leaveCluster() {
        if (serviceClusterBean.isClusterMember()) {
            serviceClusterBean.leaveCluster();
            LOGGER.info("AlarmProcessor has successfully left AlarmProcessorClusterGroup");
        }
        membershipChangeListener.stopListeners();
    }

    /**
     * Send a message in the cluster.
     *
     * @param msg The message.
     *
     **/
    public void sendClusterMessage(final Serializable msg) {
        serviceClusterBean.send(msg);
    }
}

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.cluster;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.PROPERTY_NODEID;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.ejb.Asynchronous;
import javax.ejb.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent;
import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent.ClusterMemberInfo;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;

/**
 * Asynchronously processes cluster topology changes for AlarmProcessorClusterGroup.
 */
@Singleton
public class MembershipChangeProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MembershipChangeProcessor.class);

    private final List<ClusterMemberInfo> memberList = new CopyOnWriteArrayList<ClusterMemberInfo>();

    /**
     * Method processes addition of member to the cluster.
     * @param memberShipChangeEvent
     *            Event
     */
    @Asynchronous
    public void processClusterJoinEvent(final MembershipChangeEvent memberShipChangeEvent) {
        updateAlarmProcessorMemberlist(memberShipChangeEvent);
        LOGGER.info("The APS member list after updating the members from the join event: {}", memberShipChangeEvent);
    }

    /**
     * Method processes removal of member from the cluster.
     * @param memberShipChangeEvent
     *            Event
     */
    @Asynchronous
    public void processClusterLeaveEvent(final MembershipChangeEvent memberShipChangeEvent) {
        LOGGER.info("AlarmProcessorClusterGroup leave event detected: {}", memberShipChangeEvent);
        deleteRemovedMembers(memberShipChangeEvent.getRemovedMembers());
    }

    /**
     * Remove members who have left the cluster from list of known members.
     * @param {link List} removedMembers Members to be removed from the cluster.
     */
    public void deleteRemovedMembers(final List<ClusterMemberInfo> removedMembers) {
        LOGGER.debug("AlarmProcessorClusterGroup:Before removing the members that left the cluster, current member list size is {}",
                memberList.size());
        memberList.removeAll(removedMembers);
        LOGGER.info("AlarmProcessorClusterGroup:After removing the members that left the cluster, current member list size is {}", memberList.size());
    }

    /**
     * isMaster returns true if the jboss nodeId of the first member in the cluster members list is equal to the current jboss nodeId.
     * @return {link boolean} isMaster
     */
    public boolean getMasterState() {
        boolean isMaster = false;
        final String nodeId = System.getProperty(PROPERTY_NODEID);
        final String firstMemNodeId = memberList.get(0).getNodeId();
        LOGGER.debug("The current jboss nodeId: {} and the first member nodeId: {} in the cluster members list", nodeId, firstMemNodeId);
        if (nodeId != null && firstMemNodeId != null && firstMemNodeId.equals(nodeId)) {
            isMaster = true;
        }
        return isMaster;
    }

    private void updateAlarmProcessorMemberlist(final MembershipChangeEvent memberShipChangeEvent) {
        for (final ClusterMemberInfo cmi : memberShipChangeEvent.getAllClusterMembers()) {
            if (!memberList.contains(cmi) && AlarmProcessorConstants.APS_SERVICE_ID.equals(cmi.getServiceId())) {
                memberList.add(cmi);
            }
        }
    }
}
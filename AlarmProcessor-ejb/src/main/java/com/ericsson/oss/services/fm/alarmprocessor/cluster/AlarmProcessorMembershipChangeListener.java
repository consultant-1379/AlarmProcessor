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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent;
import com.ericsson.oss.itpf.sdk.cluster.classic.MembershipChangeListener;
import com.ericsson.oss.services.fm.alarmprocessor.orphanclear.PendingClearAlarmProcessorManager;

/**
 * Class that is responsible for listening to Membership changes in AlarmProcessorClusterGroup cluster.
 */
@SuppressWarnings({ "PMD.LawOfDemeter" })
public class AlarmProcessorMembershipChangeListener implements MembershipChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmProcessorMembershipChangeListener.class);

    @Inject
    private MembershipChangeProcessor memberShipChangeProcessor;

    @Inject
    private PendingClearAlarmProcessorManager pendingClearAlarmProcessorManager;

    /**
     * Method called when the membership changed.
     * @param memberShipChangeEvent the change rule event.
     */
    @Override
    public void onMembershipChange(final MembershipChangeEvent memberShipChangeEvent) {
        this.pendingClearAlarmProcessorManager.onMembershipChange(memberShipChangeEvent.isMaster());
        if (memberShipChangeEvent.getRemovedMembers().isEmpty()) {
            LOGGER.debug("Cluster topology change is caused by new members joining the cluster");
            this.memberShipChangeProcessor.processClusterJoinEvent(memberShipChangeEvent);
        } else {
            LOGGER.debug("Cluster topology change is caused by members leaving the cluster");
            this.memberShipChangeProcessor.processClusterLeaveEvent(memberShipChangeEvent);
        }
    }

    public void stopListeners() {
        this.pendingClearAlarmProcessorManager.shutDown();
    }
}

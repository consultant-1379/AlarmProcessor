///*------------------------------------------------------------------------------
// *******************************************************************************
// * COPYRIGHT Ericsson 2015
// *
// * The copyright to the computer program(s) herein is the property of
// * Ericsson Inc. The programs may be used and/or copied only with written
// * permission from Ericsson Inc. or in accordance with the terms and
// * conditions stipulated in the agreement/contract under which the
// * program(s) have been supplied.
// *******************************************************************************
// *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.fm.alarmprocessor.cluster;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.PROPERTY_NODEID;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent;
import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent.ClusterMemberInfo;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor;

@RunWith(MockitoJUnitRunner.class)
public class MembershipChangeProcessorTest {

    @InjectMocks
    private MembershipChangeProcessor membershipChangeProcessor;

    private ClusterMemberInfo clusterMemberInfo;

    private final List<ClusterMemberInfo> clusterMemberList = new ArrayList<ClusterMemberInfo>();

    @Mock
    private MembershipChangeEvent memberShipChangeEvent;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        System.setProperty(PROPERTY_NODEID, "FM_Serv_su_0_instance");

        final String nodeId1 = "NODE_ID_1";
        final String serviceId1 = "AlarmProcessor";
        final String versionId1 = "VERSION_ID_1";

        clusterMemberInfo = new ClusterMemberInfo(nodeId1, serviceId1, versionId1);
        clusterMemberList.add(clusterMemberInfo);
    }

    @Test
    public void testProcessClusterJoinEvent() {
        when(memberShipChangeEvent.getAllClusterMembers()).thenReturn(clusterMemberList);
        membershipChangeProcessor.processClusterJoinEvent(this.memberShipChangeEvent);

        //test master false
        Assert.assertFalse(membershipChangeProcessor.getMasterState());

        //test master true
        System.setProperty(PROPERTY_NODEID, "NODE_ID_1");
        Assert.assertTrue(membershipChangeProcessor.getMasterState());
    }

    @Test
    public void testProcessClusterLeaveEvent() {
        when(memberShipChangeEvent.getRemovedMembers()).thenReturn(clusterMemberList);
        membershipChangeProcessor.processClusterLeaveEvent(this.memberShipChangeEvent);

    }

    @After
    public void cleanUp() {
        System.clearProperty(PROPERTY_NODEID);
    }
}

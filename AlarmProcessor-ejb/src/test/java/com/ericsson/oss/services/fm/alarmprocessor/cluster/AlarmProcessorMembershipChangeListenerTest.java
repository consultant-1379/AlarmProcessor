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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent;
import com.ericsson.oss.itpf.sdk.cluster.MembershipChangeEvent.ClusterMemberInfo;
import com.ericsson.oss.services.fm.alarmprocessor.orphanclear.PendingClearAlarmProcessorManager;

@RunWith(MockitoJUnitRunner.class)
public class AlarmProcessorMembershipChangeListenerTest {

    @InjectMocks
    private AlarmProcessorMembershipChangeListener memershipListener;

    @Mock
    private MembershipChangeProcessor membershipChangeProcessor;

    @Mock
    private MembershipChangeEvent membershipChangeEvent;
    
    @Mock
    private PendingClearAlarmProcessorManager pendingClearAlarmProcessorManager;

    private final List<ClusterMemberInfo> clusterMemberList = new ArrayList<MembershipChangeEvent.ClusterMemberInfo>();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMembershipJoinEvent() {
        when(membershipChangeEvent.getRemovedMembers()).thenReturn(clusterMemberList);
        memershipListener.onMembershipChange(membershipChangeEvent);
        verify(membershipChangeProcessor, times(1)).processClusterJoinEvent(membershipChangeEvent);
    }

    @Test
    public void testMembershipRemoveEvent() {

        final ClusterMemberInfo memberInfo = new ClusterMemberInfo("NODE_1", "AlarmProcessor", "1.1.1");
        clusterMemberList.add(memberInfo);
        when(membershipChangeEvent.getRemovedMembers()).thenReturn(clusterMemberList);
        memershipListener.onMembershipChange(membershipChangeEvent);
        verify(membershipChangeProcessor, times(1)).processClusterLeaveEvent(membershipChangeEvent);
    }

}

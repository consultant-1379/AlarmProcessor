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
package com.ericsson.oss.services.fm.alarmprocessor.cluster;

import static org.mockito.Mockito.verify;

import java.io.Serializable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.ericsson.oss.services.fm.alarmprocessor.cluster.AlarmProcessorCluster;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.ClusterMessageSender;

@RunWith(MockitoJUnitRunner.class)
public class ClusterMessageSenderTest {

    @InjectMocks
    private ClusterMessageSender clusterMessageSender;

    @Mock
    private AlarmProcessorCluster clusterSingleton;

    @Mock
    private Logger logger;

    @Test
    public void testSendClusterMessage() {
        final Serializable message = "message";
        clusterMessageSender.sendClusterMessage(message);
        verify(clusterSingleton).sendClusterMessage(message);
    }

}

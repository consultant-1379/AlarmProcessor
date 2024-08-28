/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.fm.alarmprocessor.healthcheck;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.cluster.AlarmProcessorHealthCheck;

@RunWith(MockitoJUnitRunner.class)
public class AlarmProcessorHealthCheckProviderTest {

    @InjectMocks
    private AlarmProcessorHealthCheckProvider alarmProcessorHealthCheckProvider;

    @Mock
	AlarmProcessorHealthCheck alarmProcessorHealthCheck;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAppHealthStatusTrue() {
        when(alarmProcessorHealthCheck.checkHealthState()).thenReturn(true);
        alarmProcessorHealthCheckProvider.getAppHealthStatus();
    }

    @Test
    public void testGetAppHealthStatusFalse() {
        when(alarmProcessorHealthCheck.checkHealthState()).thenReturn(false);
        alarmProcessorHealthCheckProvider.getAppHealthStatus();
    }
}

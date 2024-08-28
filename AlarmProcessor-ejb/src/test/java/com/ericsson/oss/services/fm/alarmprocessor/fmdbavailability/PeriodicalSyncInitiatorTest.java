/*------------------------------------------------------------------------------
 *******************************************************************************
 COPYRIGHT Ericsson 2015
 *
 The copyright to the computer program(s) herein is the property of
 Ericsson Inc. The programs may be used and/or copied only with written
 permission from Ericsson Inc. or in accordance with the terms and
 conditions stipulated in the agreement/contract under which the
 program(s) have been supplied.
 *******************************************************************************
----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.PROPERTY_NODEID;


import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.AlarmProcessorCluster;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.ServiceProxyProviderBean;

@RunWith(MockitoJUnitRunner.class)
public class PeriodicalSyncInitiatorTest {

    private static final String NODE_ID = "svc-1-fmserv";

    @InjectMocks
    private final PeriodicSyncInitiator periodicSyncInitiator = new PeriodicSyncInitiator();

    @Mock
    private AlarmProcessorCluster clusterSingleton;

    @Mock
    private MembershipChangeProcessor membershipProcessor;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private Timer timer;

    @Mock
    private TimerService timerService;

    @Mock
    private DatabaseStatusProcessor databaseStatusProcessor;

    @Mock
    private FmDatabaseAvailabilityHandler fmDBAvailabilityHandler;

    @Mock
    private DpsAvailabilityCallBackImpl dpsAvailabilityCallBackImpl;

    @Mock
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Mock
    private DataPersistenceService dps;

    @Mock
    private RetryManager retryManager;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        System.setProperty(PROPERTY_NODEID, NODE_ID);
    }

    @Test
    public void testTimeoutMaster_WithFdnsInCache() {
        when(membershipProcessor.getMasterState()).thenReturn(true);
        when(databaseStatusProcessor.isDatabaseAvailable()).thenReturn(true);
        when(configParametersListener.isDbInReadOnlyMode()).thenReturn(false);
        when(serviceProxyProviderBean.getDataPersistenceService()).thenReturn(dps);
        periodicSyncInitiator.timeOut();
        verify(fmDBAvailabilityHandler, times(1)).checkCacheAndInitiateSync();
        verify(fmDBAvailabilityHandler, times(1)).checkLocalSetAndInitiateSync();
    }


    @Test
    public void testTimeoutMaster_Listerner_AlreadyStarted() {
        when(membershipProcessor.getMasterState()).thenReturn(true);
        when(databaseStatusProcessor.isDatabaseAvailable()).thenReturn(true);
        when(configParametersListener.isDbInReadOnlyMode()).thenReturn(false);
        when(serviceProxyProviderBean.getDataPersistenceService()).thenReturn(dps);
        periodicSyncInitiator.timeOut();
        verify(fmDBAvailabilityHandler, times(1)).checkCacheAndInitiateSync();
    }

    @Test
    public void testTimeoutMaster_DB_NotAvailable() {
        when(membershipProcessor.getMasterState()).thenReturn(true);
        when(databaseStatusProcessor.isDatabaseAvailable()).thenReturn(false);
        when(serviceProxyProviderBean.getDataPersistenceService()).thenReturn(dps);
        periodicSyncInitiator.timeOut();
        verify(fmDBAvailabilityHandler, times(0)).checkCacheAndInitiateSync();
        verify(fmDBAvailabilityHandler, times(0)).checkLocalSetAndInitiateSync();
    }

    @Test
    public void testInit() {
        when(timerService.createSingleActionTimer(anyLong(), any(TimerConfig.class))).thenReturn(timer);
        periodicSyncInitiator.init();
        periodicSyncInitiator.cleanUp();
        verify(timer, times(2)).cancel();
    }

    @After
    public void cleanUp() {
        System.clearProperty(PROPERTY_NODEID);
    }
}

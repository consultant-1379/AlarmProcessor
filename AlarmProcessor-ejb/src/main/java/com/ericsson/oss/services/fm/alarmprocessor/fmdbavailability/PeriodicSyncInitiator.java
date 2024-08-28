/*------------------------------------------------------------------------------
 *******************************************************************************
 COPYRIGHT Ericsson 2017
 *
 The copyright to the computer program(s) herein is the property of
 Ericsson AB. The programs may be used and/or copied only with written
 permission from Ericsson AB. or in accordance with the terms and
 conditions stipulated in the agreement/contract under which the
 program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.cluster.AlarmProcessorCluster;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.ServiceProxyProviderBean;

/**
 * Class that is responsible for starting timer which periodically checks for the Database status and invokes alarm synchronization for nodes present
 * in Cache if required.
 */
@Singleton
@Startup
public class PeriodicSyncInitiator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicSyncInitiator.class);

    private static boolean isInvokedFirstTime = true;

    @Resource
    private TimerService timerService;

    @Inject
    private AlarmProcessorCluster alarmProcessorCluster;

    @Inject
    private MembershipChangeProcessor membershipChangeProcessor;

    @Inject
    private FmDatabaseAvailabilityHandler fmDbAvailabilityHandler;

    @Inject
    private DatabaseStatusProcessor versantDbStatusProcessor;

    @Inject
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Inject
    private DpsAvailabilityCallBackImpl dpsAvailabilityCallBackImpl;

    @Inject
    private ConfigParametersListener configParametersListener;

    private Timer databaseStatusCheckTimer;

    @PostConstruct
    public void init() {
        startAlarmTimer();
    }

    @Timeout
    public void timeOut() {
        LOGGER.debug("Timeout triggered and initiating sync based on Database availablity");
        try {
            if (isInvokedFirstTime) {
                registerDpsAvailabilityCallback();
                isInvokedFirstTime = false;
            }

            if (versantDbStatusProcessor.isDatabaseAvailable() && !configParametersListener.isDbInReadOnlyMode()) {
                if (membershipChangeProcessor.getMasterState()) {
                    fmDbAvailabilityHandler.checkCacheAndInitiateSync();
                }
                // local set should be verified irrespective of master state
                fmDbAvailabilityHandler.checkLocalSetAndInitiateSync();
            }
        } catch (final Exception exception) {
            // Canceling the timer as the same failed timer will be retried by EJB.
            if (databaseStatusCheckTimer != null) {
                databaseStatusCheckTimer.cancel();
                databaseStatusCheckTimer = null;
            }
            LOGGER.error("The exception in timeout method is:", exception);
        }
        startAlarmTimer();
    }

    @PreDestroy
    public void cleanUp() {
        if (databaseStatusCheckTimer != null) {
            LOGGER.info("Cancelling the timer");
            databaseStatusCheckTimer.cancel();
        }
    }

    private void registerDpsAvailabilityCallback() {
        LOGGER.info("Registering DpsAvailability Callback ...!");
        serviceProxyProviderBean.getDataPersistenceService().registerDpsAvailabilityCallback(dpsAvailabilityCallBackImpl);
        LOGGER.info("Registered DpsAvailability Callback ...!");
    }

    private void startAlarmTimer() {
        if (databaseStatusCheckTimer != null) {
            databaseStatusCheckTimer.cancel();
            databaseStatusCheckTimer = null;
        }
        databaseStatusCheckTimer = timerService.createSingleActionTimer(60 * 1000L, createNonPersistentTimerConfig());
    }

    private TimerConfig createNonPersistentTimerConfig() {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        return timerConfig;
    }
}

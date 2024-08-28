/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.orphanclear;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.SyncInitiator;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor;

/**
 * Expire clear alarm main processor.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@SuppressWarnings({ "PMD.AvoidCatchingGenericException", "PMD.LawOfDemeter" })
public class ExpireClearAlarmProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpireClearAlarmProcessor.class);

    @Inject
    private MembershipChangeProcessor membershipChangeAsyncProcessor;

    @Inject
    private ExpireClearAlarmManager expireClearAlarmManager;

    @Inject
    private SyncInitiator syncInitiator;

    @Inject
    private CorrelatedExpiredClearAlarmHandler correlatedExpiredClearAlarmHandler;

    /**
     * onServiceStart.
     * @param isMaster is master flag.
     */
    @Asynchronous
    public void onServiceStart(final AtomicBoolean isMaster) {
        LOGGER.info("ExpireClearAlarmProcessor started as {}", isMaster);
        while (isMaster.get() && !Thread.currentThread().isInterrupted()) {
            this.checkExpiration();
        }
    }

    /**
     * Check alarm clear expiration.
     */
    private void checkExpiration() {
        try {
            final ClearAlarmExpirable expired = this.expireClearAlarmManager.takeExpired();
            if (expired != null) {
                if (expired.hasCorrelatedAlarm()) {
                    // Remove the expired from the clustered cache.
                    if (this.correlatedExpiredClearAlarmHandler.removeClearAlarm(expired)) {
                        // Process clear alarm.
                        this.correlatedExpiredClearAlarmHandler.onEvent(expired.getAlarm());
                    } else {
                        LOGGER.warn("Expired clear alarm could not be removed");
                    }
                } else {
                    this.processClearAlarmExpirable(expired);
                }
            } else {
                LOGGER.trace("ExpireClearAlarmProcessor expired timeout");
            }
        } catch (final InterruptedException ie) {
            LOGGER.warn("ExpireClearAlarmProcessor interrupted");
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            LOGGER.warn("ExpireClearAlarmProcessor exception : ", e);
        }
    }

    /**
     * Mange the expired clear alarm.
     *
     * @param expired the expired clear alarm obj.
     */
    private void processClearAlarmExpirable(final ClearAlarmExpirable expired) {
        LOGGER.debug("Process EXPIRED {}", expired);
        if (this.membershipChangeAsyncProcessor.getMasterState()) {
            LOGGER.debug("Current APS Instance is master");
            try {
                final Set<String> neFdns = new HashSet<>();
                neFdns.add(expired.getAlarm().getFdn());
                this.syncInitiator.initiateAlarmSync(neFdns);
            } catch (final Exception exception) {
                LOGGER.error("Exception in timeOut method due to :{}", exception);
            }
        }
    }
}

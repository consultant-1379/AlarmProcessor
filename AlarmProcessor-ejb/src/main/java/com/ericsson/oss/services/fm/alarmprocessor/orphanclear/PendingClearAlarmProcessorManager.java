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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CLEAR_ALARMS_CACHE;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.cache.Cache;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean;
import com.ericsson.oss.itpf.sdk.cache.util.ServiceFrameworkCacheEntryListenerConfiguration;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.models.alarm.cache.ClearAlarmsListWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pending Clear Alarm Processor Handler class.
 */
@ApplicationScoped
@SuppressWarnings({ "PMD.AvoidCatchingGenericException", "PMD.LawOfDemeter" })
public class PendingClearAlarmProcessorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingClearAlarmProcessorManager.class);

    private final AtomicBoolean isMaster = new AtomicBoolean(false);

    @Inject
    private RetryManager retryManager;

    @Inject
    private ExpireClearAlarmProcessor expireClearAlarmProcessor;

    @Inject
    private ExpireClearAlarmManager expireClearAlarmManager;

    private Cache<String, ClearAlarmsListWrapper> cache;

    /**
     * This method start the Processor for pending clear alarm when master and stop when no more master.
     *
     * @param newValue
     *            the master value
     */
    public void onMembershipChange(final Boolean newValue) {
        LOGGER.info("PendingClearAlarmProcessorHandler onMembershipChange: isMaster change from {} to {}", this.isMaster.get(), newValue);
        if (!newValue.equals(this.isMaster.get()) && newValue) {
            // alignment from cache at startup
            final ClearAlarmCacheListener listener = new ClearAlarmCacheListener(this.expireClearAlarmManager);
            final CacheEntryListenerConfiguration<String, ClearAlarmsListWrapper> listenerConfig =
                    new ServiceFrameworkCacheEntryListenerConfiguration<>(listener);
            this.getCache().registerCacheEntryListener(listenerConfig);
            listener.onSync(this.getCache().iterator());
            this.isMaster.set(true);
            this.expireClearAlarmProcessor.onServiceStart(this.isMaster);
        }
    }

    /**
     * The shut down method.
     */
    public void shutDown() {
        LOGGER.debug("PendingClearAlarmProcessorHandler shutdown");
        this.isMaster.set(false);
    }

    /**
     * The get cache method.
     * @return the ClearAlarmsCache
     */
    protected Cache<String, ClearAlarmsListWrapper> getCache() {
        if (this.cache == null) {
            final RetryPolicy policy = RetryPolicy.builder().attempts(10).waitInterval(1, TimeUnit.SECONDS).retryOn(Exception.class).build();
            // creates and executes a retry-able command
            this.retryManager.executeCommand(policy, retryContext -> {
                this.initCache();
                return null;
            });
        }
        return this.cache;
    }

    private void initCache() {
        if (this.cache == null) {
            final CacheProviderBean bean = new CacheProviderBean();
            this.cache = bean.createOrGetModeledCache(CLEAR_ALARMS_CACHE);
        }
    }

}

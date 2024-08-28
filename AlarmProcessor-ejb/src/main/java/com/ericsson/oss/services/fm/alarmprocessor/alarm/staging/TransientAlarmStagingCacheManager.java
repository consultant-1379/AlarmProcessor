/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.staging;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class responsible for performing operations related to transientAlarmStagingCache.
 */
@ApplicationScoped
public class TransientAlarmStagingCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransientAlarmStagingCacheManager.class);

    private static final String TRANSIENT_ALARM_STAGING_CACHE = "TransientAlarmStagingCache";

    @Inject
    private SystemRecorder systemRecorder;

    private volatile Cache<String, List<ProcessedAlarmEvent>> transientAlarmStagingCache;

    /**
     * Method that adds the new transient alarm to the staging cache against a given Key.
     *
     * @param key
     *            the Key for the entry
     *
     * @param transientAlarm
     *            The transient Alarm to be inserted in the cache.
     */
    public void stageTransientAlarm(final String key, final ProcessedAlarmEvent transientAlarm) {
        if (transientAlarmStagingCache == null) {
            initializeCache();
        }
        if (null != transientAlarmStagingCache) {
            List<ProcessedAlarmEvent> transientAlarms = transientAlarmStagingCache.get(key);
            if (transientAlarms == null) {
                transientAlarms = new ArrayList<ProcessedAlarmEvent>();
            }
            transientAlarms.add(transientAlarm);
            put(key, transientAlarms);
            LOGGER.info("Added an Alarm to the transientAlarmStagingCache. Alarms in the cache agaisnt the key {} are {}", key,
                    transientAlarms.size());
        } else {
            LOGGER.error("Key: {} and TransientAlarm: {} will not be added to cache as transientAlarmStagingCache is not initialized.", key,
                    transientAlarm);
            throw new RuntimeException("TransientAlarmStagingCache is not initialized");
        }
    }

    /**
     * Method that returns an UnStaged alarm from the list of staged alarms for a given key.
     */
    public ProcessedAlarmEvent unstageTransientAlarm(final String key) {
        if (transientAlarmStagingCache == null) {
            initializeCache();
        }
        if (null != transientAlarmStagingCache) {
            final List<ProcessedAlarmEvent> stagedAlarms = get(key);
            if (stagedAlarms != null && !stagedAlarms.isEmpty()) {
                final ProcessedAlarmEvent alarm = stagedAlarms.remove(0);
                if (stagedAlarms.size() == 0) {
                    remove(key);
                } else {
                    put(key, stagedAlarms);
                }
                return alarm;
            } else {
                remove(key);
            }
        } else {
            LOGGER.error("Failed to fetch the Staged alarms for the Key:{} as transientAlarmStagingCache is not initialized.", key);
            throw new RuntimeException("TransientAlarmStagingCache is not initialized");
        }
        return null;
    }

    /**
     * Method that returns Staged alarms list for the given key.
     */
    public List<ProcessedAlarmEvent> getStagedAlarms(final String key) {
        if (transientAlarmStagingCache == null) {
            initializeCache();
        }
        if (null != transientAlarmStagingCache) {
            return get(key);
        } else {
            LOGGER.error("Failed to fetch the Staged alarms for the Key:{} as transientAlarmStagingCache is not initialized.", key);
            throw new RuntimeException("TransientAlarmStagingCache is not initialized");
        }
    }

    public Iterator<Entry<String, List<ProcessedAlarmEvent>>> iterator() {
        if (transientAlarmStagingCache == null) {
            initializeCache();
        }
        return transientAlarmStagingCache.iterator();
    }

    private List<ProcessedAlarmEvent> get(final String key) {
        if (key != null) {
            final List<ProcessedAlarmEvent> stagedAlarms = transientAlarmStagingCache.get(key);
            return stagedAlarms == null ? new ArrayList<ProcessedAlarmEvent>() : stagedAlarms;
        } else {
            return new ArrayList<ProcessedAlarmEvent>();
        }
    }

    /**
     * a Synchronized Method that puts the new transient alarms into the staging cache.
     */
    private synchronized void put(final String key, final List<ProcessedAlarmEvent> transientAlarms) {
        transientAlarmStagingCache.put(key, transientAlarms);
    }

    private void remove(final String key) {
        transientAlarmStagingCache.remove(key);
    }

    private void initializeCache() {
        try {
            final CacheProviderBean cacheProviderBean = new CacheProviderBean();
            transientAlarmStagingCache = cacheProviderBean.createOrGetModeledCache(TRANSIENT_ALARM_STAGING_CACHE);
            systemRecorder.recordEvent("Cache Initialization", EventLevel.DETAILED, TRANSIENT_ALARM_STAGING_CACHE,
                    "transientAlarmStagingCache initialization successfully completed.", "");
        } catch (final Exception exception) {
            LOGGER.error("Exception in initializeCache for transientAlarmStagingCache is: ", exception);
            throw new RuntimeException("TransientAlarmStagingCache is failed to be initialized");
        }
    }
}

/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CLEARALARMSCACHE_KEY_DELIMITER;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FM_DB_AVAILABILITY_CACHE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.MAX_READ_ENTRIES_FROM_CACHE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.INTERNAL_ALARM_FDN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.itpf.sdk.cache.infinispan.producer.CacheEntryIterator;
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;

/**
 * Class for managing operation on FmAvailabilityCache.
 */
@ApplicationScoped
public class FmDatabaseAvailabilityCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FmDatabaseAvailabilityCacheManager.class);

    private final Set<String> failedFdns = new CopyOnWriteArraySet<>();

    // Cache to hold FDNs of NetworkElement if for any reason alarms failed to persist in database.
    @Inject
    @NamedCache(FM_DB_AVAILABILITY_CACHE)
    private Cache<String, String> fmDatabaseAvailabilityCache;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * Method adds FDNs to cache when database is down to initiate sync once it is up. The entry is added first time with node fdn as key and value as
     * "fdn@@@currentTimeMsec". If the node entry is already available the entry is updated as "lastSyncTimeFromcache@@currentTimeMsec". The time
     * stamp of last sync is maintained to avoid multiple sync's for the same node with in less time.
     *
     */
    public void addFdn(final String fdn) {
        try {
            if (fdn != null && !fdn.isEmpty() && !fdn.equals(INTERNAL_ALARM_FDN)) {
                final String existingValue = fmDatabaseAvailabilityCache.get(fdn);
                final String currentTimeMsec = String.valueOf(System.currentTimeMillis());
                final StringBuilder value = new StringBuilder();
                if (existingValue == null) {
                    value.append(FDN).append(CLEARALARMSCACHE_KEY_DELIMITER).append(currentTimeMsec);
                } else if (!FDN.equals(existingValue.split(CLEARALARMSCACHE_KEY_DELIMITER)[0])) {
                    final String syncTime = existingValue.split(CLEARALARMSCACHE_KEY_DELIMITER)[0];
                    value.append(syncTime).append(CLEARALARMSCACHE_KEY_DELIMITER).append(currentTimeMsec);
                }
                if (value.length() != 0) {
                    LOGGER.info("Adding fdn: {} to FmDBAvailabilityCache with value {} to initiate sync once DPS becomes available.", fdn,
                            value);
                    fmDatabaseAvailabilityCache.put(fdn, value.toString());
                }
            }
        } catch (final Exception exception) {
            LOGGER.warn("Exception occured while adding Fdn to FmDatabaseAvailabilityCache,Exception details are:{}", exception.getMessage());
            LOGGER.debug("Exception occured while adding Fdn to FmDatabaseAvailabilityCache,Exception details are: ", exception);
            systemRecorder.recordError("APS", ErrorSeverity.ALERT, "FmAvailabilityCache",
                    "Exception in adding fdn to FmDBAvailabilityCache. Will add fdn to local memory.",
                    fdn);
            // Adding to local set as adding to cache failed
            failedFdns.add(fdn);
        }
    }

    /**
     * Puts all entries in map into cache.
     *
     * @param fdnsWithSyncTimeStamp
     *            The fdns with time stamp of sync.
     */
    public void putAll(final Map<String, String> fdnsWithSyncTimeStamp) {
        try {
            fmDatabaseAvailabilityCache.putAll(fdnsWithSyncTimeStamp);
        } catch (final Exception exception) {
            LOGGER.error("Exception {} while putting fdns {} into FmAvailabilityCache", exception.getMessage(), fdnsWithSyncTimeStamp);
            LOGGER.debug("Exception {} while putting fdns {} into FmAvailabilityCache", exception, fdnsWithSyncTimeStamp);
        }
    }

    /**
     * Returns set of NetworkElement FDNs from FmAvailabilityCache. Maximum of 1000 entries are only returned from cache in one go. Entries are also
     * removed from Cache after adding to local memory. The node fdn is added to map if the difference between current time and last sync time stamp
     * in cache for the node is greater than 5 minutes(TO DO make this configurable).
     *
     * @return Set
     */
    public Map<String, String> getFdnsFromCache() {
        Map<String, String> networkElements = null;
        CacheEntryIterator<String, String> cacheIterator = null;
        try {
            cacheIterator = (CacheEntryIterator) fmDatabaseAvailabilityCache.iterator();
            networkElements = getFdnsFromCache(cacheIterator);
        } catch (final Exception exception) {
            LOGGER.error("Exception in getAllFdns From FmAvailabilityCache {}", exception.getMessage());
            LOGGER.debug("Exception in getAllFdns From FmAvailabilityCache {}", exception);
        } finally {
            if(cacheIterator != null) {
                cacheIterator.close();
            }
        }
        return networkElements;
    }

    private Map<String, String> getFdnsFromCache(final CacheEntryIterator<String, String> cacheIterator){
        final Set<String> networkElementFdns = new HashSet<>();
        Map<String, String> networkElements = new HashMap<>();
        int count = 0;
		while (cacheIterator.hasNext()) {
            if (count > MAX_READ_ENTRIES_FROM_CACHE) {
                break;
            }
            final Entry<String, String> entry = cacheIterator.next();
            final String fdn = entry.getKey();
            final String value = entry.getValue();
            final List<String> timestamps = extractTimeStamp(value);
            final String lastSyncTimeStamp = timestamps.get(0);
            final String addedTimeToCache = timestamps.get(1);
            final long currentTimeMsec = Long.parseLong(timestamps.get(2));

            if (fdn != null) {
                if (!FDN.equals(lastSyncTimeStamp)) {
                    final long lastSyncTimeStampAsLong = Long.parseLong(lastSyncTimeStamp);
                    final long addedTimeStampAsLong = Long.parseLong(addedTimeToCache);
                    final long diff = currentTimeMsec - lastSyncTimeStampAsLong;
                    final long thresholdTime = configParametersListener.getThresholdTimeForSyncInitiation();
                    LOGGER.debug("The time difference is {} ", diff);
                    if (diff > thresholdTime * 60 * 1000 && addedTimeStampAsLong > lastSyncTimeStampAsLong) {
                        networkElementFdns.add(fdn);
                        networkElements.put(fdn, value.split(CLEARALARMSCACHE_KEY_DELIMITER)[1]);
                        ++count;
                    }
                } else {
                    networkElements.put(fdn, addedTimeToCache);
                    ++count;
                }
            }
        }
        return networkElements;
    }

    private List<String> extractTimeStamp(final String value) {
        final List<String> timeStamps = new ArrayList<>();
        String lastSyncTimeStamp = "";
        String addedTimeToCache = "";
        final long currentTimeMsec = System.currentTimeMillis();
        if (value.contains(CLEARALARMSCACHE_KEY_DELIMITER)) {
            lastSyncTimeStamp = value.split(CLEARALARMSCACHE_KEY_DELIMITER)[0];
            timeStamps.add(lastSyncTimeStamp);
            addedTimeToCache = value.split(CLEARALARMSCACHE_KEY_DELIMITER)[1];
            timeStamps.add(addedTimeToCache);
        } else {
            //This is for handling the entries added with old format(NetworkElement=NODE,fdn). The current time is added as added time stamp.
            lastSyncTimeStamp = value;
            timeStamps.add(lastSyncTimeStamp);
            addedTimeToCache = String.valueOf(currentTimeMsec);
            timeStamps.add(addedTimeToCache);
        }
        timeStamps.add(String.valueOf(currentTimeMsec));
        return timeStamps;
    }

    /**
     * Removes the set of keys provided from failed fdns set.
     *
     * @param fdns
     *            The fdns to be removed from set.
     */
    public void removeAllFromSet(final Set<String> fdns) {
        try {
            failedFdns.removeAll(fdns);
        } catch (final Exception exception) {
            LOGGER.error("Exception {} while removing fdns {} from set", exception.getMessage(), fdns);
            LOGGER.debug("Exception {} while removing fdns {} from set", exception, fdns);
        }
    }

    public Set<String> getFailedFdns() {
        return failedFdns;
    }
}

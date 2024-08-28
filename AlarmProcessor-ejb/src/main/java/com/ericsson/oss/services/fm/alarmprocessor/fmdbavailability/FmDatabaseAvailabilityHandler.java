/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CLEARALARMSCACHE_KEY_DELIMITER;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.SyncInitiator;

/**
 * Sends unprocessed alarms to NorthBound and initiate alarm synchronization for Network elements whose alarms failed to persist in database.
 */
@Stateless
public class FmDatabaseAvailabilityHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FmDatabaseAvailabilityHandler.class);

    @Inject
    private SyncInitiator syncInitiator;

    @Inject
    private FmDatabaseAvailabilityCacheManager fmDataBaseAvailabilityCacheManager;

    /**
     * Iterates over FmAvailabilityCache and initiates alarm Synchronization for all the network elements present in the cache.
     */
    public void checkCacheAndInitiateSync() {
        final Map<String, String> networkElementFdns = fmDataBaseAvailabilityCacheManager.getFdnsFromCache();
        if (!networkElementFdns.isEmpty()) {
            LOGGER.debug("Initiating sync for network elements : {} after DB became available", networkElementFdns);
            syncInitiator.initiateAlarmSync(networkElementFdns.keySet());
            final String currentTimeInMsec = String.valueOf(System.currentTimeMillis());
            final Map<String, String> fdnsWithSyncTimeStamp = new HashMap<String, String>();
            final Iterator<Entry<String, String>> iterator = networkElementFdns.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, String> entry = iterator.next();
                final String fdn = entry.getKey();
                final String existingAddedTime = entry.getValue();
                final StringBuilder value = new StringBuilder();
                value.append(currentTimeInMsec).append(CLEARALARMSCACHE_KEY_DELIMITER).append(existingAddedTime);
                LOGGER.debug("Added fdn {} to map with value {}", fdn, value.toString());
                fdnsWithSyncTimeStamp.put(fdn, value.toString());
            }
            fmDataBaseAvailabilityCacheManager.putAll(fdnsWithSyncTimeStamp);
        }
    }

    /**
     * Initiates sync for the fdn's stored in local set and initiates sync on those nodes. The fdn's are added to local set when adding to cache is
     * failed.
     */
    public void checkLocalSetAndInitiateSync() {
        // As this is a corner case it's ok to initiate sync on same node multiple times when same fdn is added to local set in different instances.
        final Set<String> neFdnsFromSet = fmDataBaseAvailabilityCacheManager.getFailedFdns();
        if (!neFdnsFromSet.isEmpty()) {
            LOGGER.debug("checkLocalSetAndInitiateSync::Initiating sync for network elements : {} after DB became available", neFdnsFromSet);
            syncInitiator.initiateAlarmSync(neFdnsFromSet);
            fmDataBaseAvailabilityCacheManager.removeAllFromSet(neFdnsFromSet);
        }
    }
}
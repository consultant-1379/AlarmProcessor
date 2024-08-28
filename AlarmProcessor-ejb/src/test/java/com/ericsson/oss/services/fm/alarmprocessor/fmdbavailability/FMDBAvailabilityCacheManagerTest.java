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

package com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FM_DB_AVAILABILITY_CACHE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.Cache.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.itpf.sdk.cache.infinispan.producer.CacheEntryIterator;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;


@RunWith(MockitoJUnitRunner.class)
public class FMDBAvailabilityCacheManagerTest {

    @InjectMocks
    private FmDatabaseAvailabilityCacheManager fmdbAvailabilityCacheManager;

    @Mock
    @NamedCache(FM_DB_AVAILABILITY_CACHE)
    private Cache<String, String> fmDbAvailabilityCache;

    @Mock
    ConfigParametersListener configParametersListener;

    @Test
    public void testUpdateFmDBAvailabilityCacheWithFdn() {
        when(fmDbAvailabilityCache.containsKey("TestFdn")).thenReturn(false);
        fmdbAvailabilityCacheManager.addFdn("TestFdn");
        verify(fmDbAvailabilityCache).put(anyString(), anyString());
    }

    /**
     * Test method for {@link com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager#getFdnsFromClearAlarmsCache()}.
     */
    @Test
    public void testGetFdnsFromClearAlarmsCache() {
        final Set<String> neFdns = new HashSet<>();
        neFdns.add("NetworkElement=TEST");
        neFdns.add("NetworkElement=TEST1");
        final CacheEntryIterator<String, String> fdnIterator = mock(CacheEntryIterator.class);
        when(fmDbAvailabilityCache.iterator()).thenReturn(fdnIterator);
        when(fdnIterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        final Entry<String, String> entry = mock(Entry.class);
        when(fdnIterator.next()).thenReturn(entry);
        when(entry.getKey()).thenReturn("NetworkElement=TEST", "NetworkElement=TEST1");
        when(entry.getValue()).thenReturn(String.valueOf(System.currentTimeMillis()-60001)+"@@@"+String.valueOf(System.currentTimeMillis()-60000))
                              .thenReturn("fdn@@@1234");
        when(configParametersListener.getThresholdTimeForSyncInitiation()).thenReturn(1);
        final Map<String, String> networkElementFdns = fmdbAvailabilityCacheManager.getFdnsFromCache();
        assertTrue(!networkElementFdns.isEmpty());
        for (final String key : networkElementFdns.keySet()) {
            assertTrue(key.contains("NetworkElement=TEST"));
        }
    }

    @Test
    public void testGetFdnsFromClearAlarmsCacheException() {
        final Map<String, String> networkElementFdns = fmdbAvailabilityCacheManager.getFdnsFromCache();
        assertNull(networkElementFdns);
    }

}

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
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.SyncInitiator;

@RunWith(MockitoJUnitRunner.class)
public class FmDatabaseAvailabilityHandlerTest {

    @InjectMocks
    private FmDatabaseAvailabilityHandler fmDBAvailabilityHandler;

    @Mock
    @NamedCache(FM_DB_AVAILABILITY_CACHE)
    private Cache<String, String> fmDbAvailabilityCache;

    @Mock
    private FmDatabaseAvailabilityCacheManager fmDBAvailabilityCacheManager;

    @Mock
    private SyncInitiator syncInitiator;

    @Test
    public void testCheckCacheAndInitiateSync() {
        final Map<String, String> neFdns = new HashMap<String, String>();
        neFdns.put("NetworkElement=TEST", FDN);
        neFdns.put("NetworkElement=TEST1", FDN);
        final Iterator<Entry<String, String>> fdnItr = Mockito.mock(Iterator.class);
        when(fmDbAvailabilityCache.iterator()).thenReturn(fdnItr);
        when(fdnItr.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        final Entry<String, String> entry = Mockito.mock(Entry.class);
        when(fdnItr.next()).thenReturn(entry);
        when(entry.getKey()).thenReturn("NetworkElement=TEST", "NetworkElement=TEST1");
        when(fmDBAvailabilityCacheManager.getFdnsFromCache()).thenReturn(neFdns);
        fmDBAvailabilityHandler.checkCacheAndInitiateSync();
        verify(fmDBAvailabilityCacheManager, times(1)).putAll(anyMap());
        verify(syncInitiator, times(1)).initiateAlarmSync(neFdns.keySet());
    }

    @Test
    public void testCheckCacheAndInitiateSyncWhenSetIsNotEmpty() {
        final Set<String> neFdns = new HashSet<>();
        neFdns.add("NetworkElement=TEST");
        neFdns.add("NetworkElement=TEST1");
        when(fmDBAvailabilityCacheManager.getFailedFdns()).thenReturn(neFdns);
        fmDBAvailabilityHandler.checkLocalSetAndInitiateSync();
        verify(fmDBAvailabilityCacheManager, times(0)).putAll(anyMap());
        verify(syncInitiator, times(1)).initiateAlarmSync(neFdns);
    }
}

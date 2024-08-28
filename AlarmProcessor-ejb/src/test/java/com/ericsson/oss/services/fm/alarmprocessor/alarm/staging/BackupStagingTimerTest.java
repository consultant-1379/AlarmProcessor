/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.fm.alarmprocessor.alarm.staging;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.cache.Cache.Entry;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.oss.itpf.sdk.cache.infinispan.producer.CacheEntryIterator;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmHandlerBean;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class BackupStagingTimerTest {

    @InjectMocks
    private BackupStagingTimer backupStagingTimer;

    @Mock
    private Timer backupStagingTimer1;

    @Mock
    private TimerService timerService;

    @Mock
    private MembershipChangeProcessor membershipChangeProcessor;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private TransientAlarmStagingCacheManager transientAlarmStagingCacheManager;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private ProcessedAlarmSender processedAlarmSender;

    @Mock
    private AlarmHandlerBean alarmHandlerBean;

    @Mock
    private CacheEntryIterator cacheEntryIterator;

    @Mock
    Entry<String, Object> entry;

    private ProcessedAlarmEvent alarmRecord;
    private final List<ProcessedAlarmEvent> stagedAlarms = new ArrayList<ProcessedAlarmEvent>();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setObjectOfReference("Node1");
        alarmRecord.setAlarmNumber(1234L);
        stagedAlarms.add(alarmRecord);
    }

    @Test
    public void test_unstageLongPendingAlarms() {
        when(membershipChangeProcessor.getMasterState()).thenReturn(true);
        when(transientAlarmStagingCacheManager.iterator()).thenReturn(cacheEntryIterator);
        when(cacheEntryIterator.hasNext()).thenReturn(true).thenReturn(false);
        when(cacheEntryIterator.next()).thenReturn(entry);
        when(entry.getValue()).thenReturn(stagedAlarms);
        backupStagingTimer.checkMasterAndUnstageAlarms();
        verify(transientAlarmStagingCacheManager, times(1)).unstageTransientAlarm(null);
	}

}

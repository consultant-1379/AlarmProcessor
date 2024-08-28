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

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.cache.Cache.Entry;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cache.infinispan.producer.CacheEntryIterator;

import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmHandlerBean;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Timer Bean responsible to check if there are any Alarms present in the Transient Alarm Staging Cache for longer time that expected. If there are
 * any such alarms, un-stage and process the same further.
 *
 * The timer is configurable based on the value of TransientAlarmStagingThresholdTime(*600 times) For eg: - If TransientAlarmStagingThresholdTime is
 * 300 milliseconds, this backup timer is run for every 3 minutes. - Similarly, if TransientAlarmStagingThresholdTime is 1000 milliseconds, this
 * backup timer is run for every 10 minutes.
 */
@Singleton
@Startup
public class BackupStagingTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupStagingTimer.class);

    private Timer backupStagingTimer;

    @Resource
    private TimerService timerService;

    @Inject
    private MembershipChangeProcessor membershipChangeProcessor;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private TransientAlarmStagingCacheManager transientAlarmStagingCacheManager;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private ProcessedAlarmSender processedAlarmSender;

    @Inject
    private AlarmHandlerBean alarmHandlerBean;

    @PostConstruct
    public void startTimer() {
        if (configParametersListener.getTransientAlarmStaging()) {
            // Convert milliseconds to corresponding minutes
            final long timeDuration = configParametersListener.getTransientAlarmStagingThresholdTime() * 600L;
            LOGGER.info("Creating the backupStagingTimer with {} milliseconds", timeDuration);
            backupStagingTimer = timerService.createIntervalTimer(timeDuration, timeDuration, createNonPersistentTimerConfig());
        } else {
            LOGGER.info("Not starting the backupStagingTimer at the Startup as the TransientAlarmStaging is not enabled.");
        }
    }

    /**
     * Method that iterates-over all the staged transient alarms present in the cache to check if any alarm is present for more than the expected
     * amount of time.
     */
    @Timeout
    public void checkMasterAndUnstageAlarms() {
        if (membershipChangeProcessor.getMasterState()) {
            LOGGER.debug("Current APS Instance is master for Backup Staging Timer");
            checkAndUnstageLongPendingAlarms();
        } else {
            LOGGER.debug("Ignoring the Backup Staging Timer as the current APS Instance is slave.");
        }
    }

    @PreDestroy
    public void cancelTimer() {
        LOGGER.debug("Cancelling the backupStagingTimer");
        if (backupStagingTimer != null) {
            try {
                backupStagingTimer.cancel();
                backupStagingTimer = null;
            } catch (final NoSuchObjectLocalException exception) {
                LOGGER.warn("Exception {} occurred while trying to cancle the timer {}.", exception.getMessage(), backupStagingTimer);
            }
        }
    }

    private TimerConfig createNonPersistentTimerConfig() {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        return timerConfig;
    }

    private void checkAndUnstageLongPendingAlarms() {
        String cacheKey = "";
        ProcessedAlarmEvent stagedAlarm = new ProcessedAlarmEvent();
        CacheEntryIterator<String, List<ProcessedAlarmEvent>> cacheIterator = null;
        try {
            LOGGER.debug("Iterating through the TransientAlarmStagingCache to read and list the entries!!!!");
            cacheIterator = (CacheEntryIterator<String, List<ProcessedAlarmEvent>>) transientAlarmStagingCacheManager.iterator();
            while (cacheIterator.hasNext()) {
                final Entry<String, List<ProcessedAlarmEvent>> entry = cacheIterator.next();
                final List<ProcessedAlarmEvent> stagedAlarms = entry.getValue();
                cacheKey = entry.getKey();
                LOGGER.debug("Iterating through {} entries against the key:{}", stagedAlarms.size(), cacheKey);
                for (final ProcessedAlarmEvent stagedAlarmFromCache : stagedAlarms) {
                    LOGGER.trace("Entry in the cache {}", stagedAlarmFromCache);
                    stagedAlarm = stagedAlarmFromCache;
                    final Date lastUpdatedTime = stagedAlarmFromCache.getLastUpdatedTime();
                    unstageLongPendingAlarms(lastUpdatedTime, stagedAlarmFromCache, cacheKey);
                }
            }
        } catch (final Exception exception) {
            cancelTimer();
            transientAlarmStagingCacheManager.stageTransientAlarm(cacheKey, stagedAlarm);
            startTimer();
            LOGGER.error("Exception {} in Timeout method of backupStagingTimer while processing the alarm with key:{}", exception, cacheKey);
        } finally {
            if(cacheIterator != null) {
                cacheIterator.close();
             }
        }
    }

    private void unstageLongPendingAlarms(final Date lastUpdatedTime, final ProcessedAlarmEvent stagedAlarmFromCache, final String cacheKey){
        AlarmProcessingResponse alarmProcessingResponse;
        if (lastUpdatedTime == null
                || System.currentTimeMillis() < lastUpdatedTime.getTime()
                        + configParametersListener.getTransientAlarmStagingThresholdTime()) {
            LOGGER.debug("Unstaged the alarm {} from backup staging timer as it has stayed in the cache.", stagedAlarmFromCache);
            alarmProcessingResponse = alarmHandlerBean.processAlarm(stagedAlarmFromCache);
            processedAlarmSender.sendAlarms(alarmProcessingResponse, null);
            apsInstrumentedBean.incrementAlarmsUnstagedByBackupTimerCount();
            transientAlarmStagingCacheManager.unstageTransientAlarm(cacheKey);
        }
    }
}

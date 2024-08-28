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

import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.getKeyFromAlarm;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NORMAL_PROC;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class responsible for performing operations related to Staging algorithm.
 */
@ApplicationScoped
public class AlarmStagingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmStagingHandler.class);

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private TransientAlarmStagingCacheManager transientAlarmStagingCacheManager;

    @Inject
    private AlarmStagingTimer alarmStagingTimer;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    /**
     * Checks and stages the transient alarm into the cache to delay the processing for the configured amount of time.
     *
     * @param alarmRecord
     *            Current alarm record being verified if its a transient alarm and needs to be staged
     * @param correlatedAlarm
     *            Correlated alarm which which the current record is received within a very short-time
     *
     * @return boolean a boolean that represents whether the current alarm is staged or not
     */
    public boolean checkAndStageAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        boolean staged = false;
        try {
            final int stageThreasholdPeriod = configParametersListener.getTransientAlarmStagingThresholdTime();
            final String key = getKeyFromAlarm(alarmRecord);
            final Date originalAlarmLastUpdatedTime = correlatedAlarm.getLastUpdatedTime();
            long currentTimeMillis = 0L;
            List<ProcessedAlarmEvent> stagedAlarms = new ArrayList<ProcessedAlarmEvent>();
            if (originalAlarmLastUpdatedTime != null) {
                stagedAlarms = transientAlarmStagingCacheManager.getStagedAlarms(key);
                if (stagedAlarms != null && stagedAlarms.size() > 0) {
                    LOGGER.debug("There are already {} alarms staged for the key:{}. So checking against the alarms in cache before staging.",
                            stagedAlarms.size(), key);
                    final ProcessedAlarmEvent lastStagedAlarm = stagedAlarms.get(stagedAlarms.size() - 1);
                    currentTimeMillis = System.currentTimeMillis();
                    if (lastStagedAlarm != null && lastStagedAlarm.getLastUpdatedTime() != null
                            && isThresholdTimeCrossed(stageThreasholdPeriod, lastStagedAlarm.getLastUpdatedTime().getTime(), currentTimeMillis)) {
                        staged = stageAlarmAndStartTimer(alarmRecord, stageThreasholdPeriod, key);
                    } else {
                        LOGGER.debug("The TransientAlarmStagingThresholdTime for the alarm in the cache is crossed!!! Not staging the alarm.");
                        return staged;
                    }
                } else {
                    final long lastUpdatedTimeMillis = originalAlarmLastUpdatedTime.getTime();
                    currentTimeMillis = System.currentTimeMillis();
                    if (isThresholdTimeCrossed(stageThreasholdPeriod, lastUpdatedTimeMillis, currentTimeMillis)
                            && isHiddenNormalProcAlarm(correlatedAlarm)) {
                        LOGGER.debug("This is the first alarm to be staged against the Key:{}", key);
                        staged = stageAlarmAndStartTimer(alarmRecord, stageThreasholdPeriod, key);
                    } else {
                        LOGGER.debug("The TransientAlarmStagingThresholdTime is crossed!!! Not staging the alarm.");
                        return staged;
                    }
                }
            } else {
                LOGGER.debug("The LastUpdatedTime is null for the correlated alarm in the DB!!! Not staging the alarm.");
            }
        } catch (final Exception exception) {
            LOGGER.error("Exception while trying to stage the alarmRecord:{} and with its correlatedAlarm:{} is {}", alarmRecord, correlatedAlarm,
                    exception.getMessage());
            // Throwing runtime exception here so alarm will be retried processing in APS!
            throw new RuntimeException(exception);
        }
        return staged;
    }

    private boolean stageAlarmAndStartTimer(final ProcessedAlarmEvent alarmRecord, final int stageThreasholdPeriod, final String key) {
        // Set last updated time here if its not present already, just to understand the stage time.
        if (alarmRecord.getLastUpdatedTime() == null) {
            alarmRecord.setLastUpdatedTime(new Date(System.currentTimeMillis()));
        }
        // Stage the alarm into the Cache
        transientAlarmStagingCacheManager.stageTransientAlarm(key, alarmRecord);
        // Trigger the timer that will un-stage this alarm after the configured amount of time.
        alarmStagingTimer.startTimer(stageThreasholdPeriod, key);
        // Record the event and exit!!!
        LOGGER.debug("Alarm {} has been staged into the cache with the key:{} and timeout:{}", alarmRecord, key, stageThreasholdPeriod);
        apsInstrumentedBean.incrementAlarmsStagedCount();
        return true;
    }

    private boolean isHiddenNormalProcAlarm(final ProcessedAlarmEvent correlatedAlarm) {
        // The Correlated alarm has to be a NormalProc-Hidden alarm.
        return !correlatedAlarm.getVisibility() && NORMAL_PROC.equals(correlatedAlarm.getProcessingType());
    }

    private boolean isThresholdTimeCrossed(final int stageThreasholdPeriod, final long lastUpdatedTimeMillis, final long currentTimeMillis) {
        // The correlated alarm is updated before 300 milliseconds.
        LOGGER.debug("The time value of the alarm are... lastUpdatedTime:{}, threshold:{}, currenttimemilllis:{}", lastUpdatedTimeMillis,
                stageThreasholdPeriod, currentTimeMillis);
        return (currentTimeMillis - lastUpdatedTimeMillis) < stageThreasholdPeriod;
    }
}

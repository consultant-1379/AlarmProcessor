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

package com.ericsson.oss.services.fm.alarmprocessor.configuration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.modeling.annotation.constraints.NotNull;
import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.staging.BackupStagingTimer;
import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.AlarmSyncAbortTimer;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.AlarmProcessingAnalyser;
import com.ericsson.oss.services.fm.alarmprocessor.protection.AlarmOverloadProtectionService;

/**
 * Class is Responsible for listen for Configuration change on OscillationCorrelation,updateInsertTime parameters.
 */
@ApplicationScoped
public class ConfigParametersListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigParametersListener.class);

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private AlarmSyncAbortTimer alarmSyncAbortTimer;

    @Inject
    private AlarmProcessingAnalyser alarmProcessingAnalyser;

    @Inject
    private AlarmOverloadProtectionService alarmOverloadProtectionService;

    @Inject
    @NotNull
    @Configured(propertyName = "oscillationAlarmCorrelation")
    private Boolean oscillationAlarmCorrelation;

    @Inject
    @NotNull
    @Configured(propertyName = "updateInsertTime")
    private Boolean updateInsertTime;

    @Inject
    @NotNull
    @Configured(propertyName = "timerIntervalToDiscardOngoingAlarmSync")
    private int timerIntervalToDiscardOngoingAlarmSync;

    @Inject
    @NotNull
    @Configured(propertyName = "retryLimitForAlarmProcessing")
    private int retryLimitForAlarmProcessing;

    @Inject
    @NotNull
    @Configured(propertyName = "periodicAlarmSynchronization")
    private Boolean periodicAlarmSynchronization;

    @Inject
    @NotNull
    @Configured(propertyName = "timerIntervalToInitiateAlarmSync")
    private int timerIntervalToInitiateAlarmSync;

    @Inject
    @NotNull
    @Configured(propertyName = "timerIntervalToInitiateAlarmSyncMultiplier") // 5000 msec
    private int timerIntervalToInitiateAlarmSyncMultiplier;

    @Inject
    @NotNull
    @Configured(propertyName = "thresholdTimeForSyncInitiation")
    private int thresholdTimeForSyncInitiation;

    @Inject
    @NotNull
    @Configured(propertyName = "alarmThresholdInterval")
    private long alarmThresholdInterval;

    @Inject
    @NotNull
    @Configured(propertyName = "alarmDelayToQueue")
    private long alarmDelayToQueue;

    @Inject
    @NotNull
    @Configured(propertyName = "clearAlarmDelayToQueue")
    private long clearAlarmDelayToQueue;

    @Inject
    @NotNull
    @Configured(propertyName = "isCircuitBreakerEnabled")
    private Boolean isCircuitBreakerEnabled;

    @Inject
    @NotNull
    @Configured(propertyName = "threadStateCheckInterval")
    private int threadStateCheckInterval;

    @Inject
    @NotNull
    @Configured(propertyName = "hardThresholdRatio")
    private int hardThresholdRatio;

    @Inject
    @NotNull
    @Configured(propertyName = "softThresholdRatio")
    private int softThresholdRatio;

    @Inject
    @NotNull
    @Configured(propertyName = "secondaryThreadsCount")
    private int secondaryThreadsCount;

    @Inject
    @NotNull
    @Configured(propertyName = "thresholdHoldTime")
    private int thresholdHoldTime;

    @Inject
    @NotNull
    @Configured(propertyName = "isDbInReadOnlyMode")
    private Boolean isDbInReadOnlyMode;

    @Inject
    @NotNull
    @Configured(propertyName = "transientAlarmStaging")
    private Boolean transientAlarmStaging;

    @Inject
    @NotNull
    @Configured(propertyName = "transientAlarmStagingThresholdTime")
    private int transientAlarmStagingThresholdTime;

    //Default value is true.
    @Inject
    @NotNull
    @Configured(propertyName = "alarmOverloadProtectionOn")
    private Boolean alarmOverloadProtectionOn;

    //Default value is 55200.
    @Inject
    @NotNull
    @Configured(propertyName = "alarmOverloadProtectionThreshold")
    private Long alarmOverloadProtectionThreshold;

    //Default value is 70.
    @Inject
    @NotNull
    @Configured(propertyName = "alarmOverloadProtectionLowerThreshold")
    private Integer alarmOverloadProtectionLowerThreshold;

    @Inject
    private BackupStagingTimer backupStagingTimer;

    //Default value is false.
    @Inject
    @NotNull
    @Configured(propertyName = "MIGRATION_ON_GOING")
    private boolean migrationOnGoing;

    //Default value is 5
    @Inject
    @Configured(propertyName = "redeliveryAttemptsForFMAlarmQueue")
    private Integer redeliveryAttempts;

    //Default value is 1000
    @Inject
    @Configured(propertyName = "redeliveryDelayForFMAlarmQueue")
    private Integer redeliveryDelay;

    public int getRetryLimitForAlarmProcessing() {
        return retryLimitForAlarmProcessing;
    }

    public Boolean getOscillatingCorrelation() {
        return oscillationAlarmCorrelation;
    }

    public Boolean getUpdateInsertTime() {
        return updateInsertTime;
    }

    public int getThreadStateCheckInterval() {
        return threadStateCheckInterval;
    }

    public void setThreadStateCheckInterval(final int threadStateCheckInterval) {
        this.threadStateCheckInterval = threadStateCheckInterval;
    }

    public Boolean isCircuitBreakerEnabled() {
        return isCircuitBreakerEnabled;
    }

    public int getHardThresholdRatio() {
        return hardThresholdRatio;
    }

    public int getSoftThresholdRatio() {
        return softThresholdRatio;
    }

    public int getSecondaryThreadsCount() {
        return secondaryThreadsCount;
    }

    public int getThresholdHoldTime() {
        return thresholdHoldTime;
    }

    public int getTimerIntervalToDiscardOngoingAlarmSync() {
        return timerIntervalToDiscardOngoingAlarmSync;
    }

    public int getTimerIntervalToInitiateAlarmSync() {
        return timerIntervalToInitiateAlarmSync;
    }

    public int getTimerIntervalToInitiateAlarmSyncMultiplier() {
        return timerIntervalToInitiateAlarmSyncMultiplier;
    }
    public int getThresholdTimeForSyncInitiation() {
        return thresholdTimeForSyncInitiation;
    }

    public Boolean getPeriodicAlarmSynchronization() {
        return periodicAlarmSynchronization;
    }

    public long getAlarmThresholdInterval() {
        return alarmThresholdInterval;
    }

    public long getAlarmDelayToQueue() {
        return alarmDelayToQueue;
    }

    public long getClearAlarmDelayToQueue() {
        return clearAlarmDelayToQueue;
    }

    public Boolean isDbInReadOnlyMode() {
        return isDbInReadOnlyMode;
    }

    public void setIsDbInReadOnlyMode(final Boolean isDbInReadOnlyMode) {
        this.isDbInReadOnlyMode = isDbInReadOnlyMode;
    }

    public Boolean getTransientAlarmStaging() {
        return transientAlarmStaging;
    }

    public int getTransientAlarmStagingThresholdTime() {
        return transientAlarmStagingThresholdTime;
    }

    public Boolean getAlarmOverloadProtectionOn() {
        return alarmOverloadProtectionOn;
    }

    public Long getAlarmOverloadProtectionThreshold() {
        return alarmOverloadProtectionThreshold;
    }

    public Integer getAlarmOverloadProtectionLowerThreshold() {
        return alarmOverloadProtectionLowerThreshold;
    }

    public Boolean isMigrationOnGoing() {
        return migrationOnGoing;
    }

    public Integer getRedeliveryAttempts() {
        return redeliveryAttempts;
    }

    private void setRedeliveryAttempts(final Integer redeliveryAttempts) {
        this.redeliveryAttempts = redeliveryAttempts;
    }

    public Integer getRedeliveryDelay() {
        return redeliveryDelay;
    }

    private void setRedeliveryDelay(final Integer redeliveryDelay) {
        this.redeliveryDelay = redeliveryDelay;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on oscillationAlarmCorrelation and forward to oscillation correlation changes.
     *
     * @param newValue the new value.
     */
    public void listenForOscillationCorrelationChanges(@Observes @ConfigurationChangeNotification(propertyName = "oscillationAlarmCorrelation")
                    final Boolean newValue) {
        LOGGER.info("oscillationAlarmCorrelation  is changed and new value for oscillationAlarmCorrelation is: {}", newValue);
        oscillationAlarmCorrelation = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on updateInsertTime and forward update to insert time changes.
     *
     * @param newValue the new value.
     */
    public void listenForUpdateInsertTimeChanges(@Observes @ConfigurationChangeNotification(propertyName = "updateInsertTime")
                    final Boolean newValue) {
        LOGGER.info("updateInsertTime attribute is changed and new value for updateInsertTime is: {}", newValue);
        updateInsertTime = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on timerIntervalToDiscardOngoingAlarmSync and forward timer interval to discard ongoing
     * alarmSync changes.
     *
     * @param newValue the new value.
     */
    public void listenForTimerIntervalToDiscardOngoingAlarmSync(@Observes @ConfigurationChangeNotification(propertyName =
                    "timerIntervalToDiscardOngoingAlarmSync") final int newValue) {
        LOGGER.info("timerIntervalToDiscardOngoingAlarmSync attribute is changed and new value for timerIntervalToDiscardOngoingAlarmSync is: {}",
                newValue);
        timerIntervalToDiscardOngoingAlarmSync = newValue;
        alarmSyncAbortTimer.recreateTimerWithNewInterval(timerIntervalToDiscardOngoingAlarmSync);
    }

    /**
     * Method listens for @ConfigurationChangeNotification on retryLimitForAlarmProcessing and forward retry limit for alarm processing changes.
     *
     * @param newValue the new value.
     */
    public void listenForRetryLimitForAlarmProcessing(@Observes @ConfigurationChangeNotification(propertyName = "retryLimitForAlarmProcessing")
                    final int newValue) {
        LOGGER.info("retryLimitForAlarmProcessing attribute is changed and new value for retryLimitForAlarmProcessing is: {}", newValue);
        retryLimitForAlarmProcessing = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on periodicAlarmSynchronization and forward periodic alarm synchronization changes.
     *
     * @param newValue the new value.
     */
    void listenForPeriodicAlarmSynchronizationChanges(@Observes @ConfigurationChangeNotification(propertyName = "periodicAlarmSynchronization")
                    final Boolean newValue) {
        LOGGER.info("periodicAlarmSynchronization attribute is changed and new value for periodicAlarmSynchronization is: {}", newValue);
        periodicAlarmSynchronization = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on thresholdTimeForSyncInitiation and updates internally.
     *
     * @param newValue the new value.
     */
    public void listenForThresholdTimeForSyncInitiation(@Observes @ConfigurationChangeNotification(propertyName = "thresholdTimeForSyncInitiation")
                    final int newValue) {
        LOGGER.warn("thresholdTimeForSyncInitiation attribute is changed and new value for thresholdTimeForSyncInitiation is: {}", newValue);
        thresholdTimeForSyncInitiation = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on timerIntervalToInitiateAlarmSync and forward timer interval to initiate alarm sync
     * changes.
     *
     * @param newValue the new value.
     */
    public void listenForTimerIntervalToInitiateAlarmSync(@Observes @ConfigurationChangeNotification(propertyName =
                    "timerIntervalToInitiateAlarmSync") final int newValue) {
        LOGGER.info("timerIntervalToInitiateAlarmSync attribute is changed and new value for timerIntervalToInitiateAlarmSync is: {}", newValue);
        timerIntervalToInitiateAlarmSync = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on timerIntervalToInitiateAlarmSyncMultiplier and forward timer interval to initiate alarm sync
     * changes.
     *
     * @param newValue the new value.
     */
    public void listenForTimerIntervalToInitiateAlarmSyncMultiplier(@Observes @ConfigurationChangeNotification(propertyName =
        "timerIntervalToInitiateAlarmSyncMultiplier") final int newValue) {
        LOGGER.info("timerIntervalToInitiateAlarmSyncMultiplier attribute is changed and new value for timerIntervalToInitiateAlarmSyncMultiplier is: {}", newValue);
        timerIntervalToInitiateAlarmSyncMultiplier = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on alarmThresholdInterval and to assess delay to NBI JMS Queue.
     *
     * @param newValue the new value.
     */
    public void listenForAalarmThresholdInterval(@Observes @ConfigurationChangeNotification(propertyName = "alarmThresholdInterval")
                    final long newValue) {
        LOGGER.info("alarmThresholdInterval attribute is changed and new value for alarmThresholdInterval is: {}", newValue);
        alarmThresholdInterval = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on alarmDelayToQueue and forward delay to NBI JMS Queue.
     *
     * @param newValue the new value.
     */
    public void listenForAlarmDelayToQueue(@Observes @ConfigurationChangeNotification(propertyName = "alarmDelayToQueue") final long newValue) {
        LOGGER.info("alarmDelayToQueue attribute is changed and new value for alarmDelayToQueue is: {}", newValue);
        alarmDelayToQueue = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on clearAlarmDelayToQueue and forward delay to NBI JMS Queue. changes.
     *
     * @param newValue the new value.
     */
    public void listenForClearAlarmDelayToQueue(@Observes @ConfigurationChangeNotification(propertyName = "clearAlarmDelayToQueue")
                    final long newValue) {
        LOGGER.info("clearAlarmDelayToQueue attribute is changed and new value for clearAlarmDelayToQueue is: {}", newValue);
        clearAlarmDelayToQueue = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on isCircuitBreakerEnabled.
     *
     * @param newValue the new value.
     */
    public void listenForCircuitBreakerEnabled(@Observes @ConfigurationChangeNotification(propertyName = "isCircuitBreakerEnabled")
                    final Boolean newValue) {
        LOGGER.info("isCircuitBreakerEnabled attribute is changed and new value for isCircuitBreakerEnabled is: {}", newValue);
        isCircuitBreakerEnabled = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on threadStateCheckInterval and change the timer interval to check active threads' state.
     *
     * @param newValue the new value.
     */
    public void listenForThreadStateCheckInterval(@Observes @ConfigurationChangeNotification(propertyName = "threadStateCheckInterval")
                    final int newValue) {
        LOGGER.info("threadStateCheckInterval attribute is changed and new value for threadStateCheckInterval is: {}", newValue);
        threadStateCheckInterval = newValue;
        alarmProcessingAnalyser.recreateTimerWithNewInterval(threadStateCheckInterval);
    }

    /**
     * Method listens for @ConfigurationChangeNotification on hardThresholdRatio.
     *
     * @param newValue the new value.
     */
    public void listenForHardThresholdRatio(@Observes @ConfigurationChangeNotification(propertyName = "hardThresholdRatio") final int newValue) {
        LOGGER.info("hardThresholdRatio attribute is changed and new value for hardThresholdRatio is: {}", newValue);
        hardThresholdRatio = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on softThresholdRatio.
     *
     * @param newValue the new value.
     */
    public void listenForSoftThresholdRatio(@Observes @ConfigurationChangeNotification(propertyName = "softThresholdRatio") final int newValue) {
        LOGGER.info("softThresholdRatio attribute is changed and new value for softThresholdRatio is: {}", newValue);
        softThresholdRatio = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on secondaryThreadsCount.
     *
     * @param newValue the new value.
     */
    public void listenForSecondaryThreadsCount(@Observes @ConfigurationChangeNotification(propertyName = "secondaryThreadsCount")
                    final int newValue) {
        LOGGER.info("secondaryThreadsCount attribute is changed and new value for secondaryThreadsCount is: {}", newValue);
        secondaryThreadsCount = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on thresholdHoldTime.
     *
     * @param newValue the new value.
     */
    public void listenForThresholdHoldTime(@Observes @ConfigurationChangeNotification(propertyName = "thresholdHoldTime") final int newValue) {
        LOGGER.info("thresholdHoldTime attribute is changed and new value for thresholdHoldTime is: {}", newValue);
        thresholdHoldTime = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on isDbInReadOnlyMode.
     *
     * @param newValue the new value.
     */
    public void listenForIsDbInReadOnlyModeChanges(@Observes @ConfigurationChangeNotification(propertyName = "isDbInReadOnlyMode")
                    final Boolean newValue) {
        LOGGER.warn("Setting the database read only mode to: {}", newValue);
        isDbInReadOnlyMode = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on transientAlarmStaging.
     *
     * @param newValue the new value.
     */
    public void listenForTransientAlarmStaging(@Observes @ConfigurationChangeNotification(propertyName = "transientAlarmStaging")
                    final Boolean newValue) {
        LOGGER.info("transientAlarmStaging attribute is changed and new value for transientAlarmStaging is: {}", newValue);
        transientAlarmStaging = newValue;
        if (newValue) {
            backupStagingTimer.startTimer();
        } else {
            backupStagingTimer.cancelTimer();
        }
    }

    /**
     * Method listens for @ConfigurationChangeNotification on transientAlarmStagingThresholdTime.
     *
     * @param newValue the new value.
     */
    public void listenForTransientAlarmStagingThresholdTime(@Observes @ConfigurationChangeNotification(propertyName =
                    "transientAlarmStagingThresholdTime") final int newValue) {
        LOGGER.info("transientAlarmStagingThresholdTime attribute is changed and new value for transientAlarmStagingThresholdTime is: {}", newValue);
        transientAlarmStagingThresholdTime = newValue;
        if (transientAlarmStaging) {
            LOGGER.info("Re-creating the Backup Staging Timer as per the new value of transientAlarmStagingThresholdTime.");
            backupStagingTimer.cancelTimer();
            backupStagingTimer.startTimer();
        } else {
            LOGGER.info("TransientAlarmStaging is not enabled. So not starting the Backup Staging Timer.");
        }
    }

    /**
     * Method listens for @ConfigurationChangeNotification on alarmOverloadProtectionOn.
     * @param newValue the new value.
     */
    public void listenForAlarmOverloadProtectionOn(
            @Observes @ConfigurationChangeNotification(propertyName = "alarmOverloadProtectionOn") final Boolean newValue) {
        LOGGER.info("alarmOverloadProtectionOn attribute is changed and new value for alarmOverloadProtectionOn is: {}", newValue);
        alarmOverloadProtectionOn = newValue;
        alarmOverloadProtectionService.setAlarmOverloadProtection(newValue);
    }

    /**
     * Method listens for @ConfigurationChangeNotification on alarmOverloadProtectionThreshold.
     * @param newValue the new value.
     */
    public void listenForAlarmOverloadProtectionThreshold(
            @Observes @ConfigurationChangeNotification(propertyName = "alarmOverloadProtectionThreshold") final Long newValue) {
        LOGGER.info("alarmOverloadProtectionThreshold attribute is changed and new value for alarmOverloadProtectionThreshold is: {}", newValue);
        alarmOverloadProtectionThreshold = newValue;
        alarmOverloadProtectionService.setRaisingThreshold(newValue);
    }

    /**
     * Method listens for @ConfigurationChangeNotification on alarmOverloadProtectionLowerThreshold.
     * @param newValue the new value.
     */
    public void listenForAlarmOverloadProtectionLowerThreshold(
            @Observes @ConfigurationChangeNotification(propertyName = "alarmOverloadProtectionLowerThreshold") final Integer newValue) {
        LOGGER.info("alarmOverloadProtectionLowerThreshold attribute is changed and new value for alarmOverloadProtectionLowerThreshold is: {}",
                newValue);
        alarmOverloadProtectionLowerThreshold = newValue;
        alarmOverloadProtectionService.setWarningClearThresholdPercentage(newValue);
    }

    /**
     * Method listens for @ConfigurationChangeNotification on MIGRATION_ON_GOING parameter changes.
     *
     * @param newValue
     *            new value of MIGRATION_ON_GOING.
     *
     */
    public void listenForMigrationOnGoingChanges(@Observes @ConfigurationChangeNotification(propertyName = "MIGRATION_ON_GOING")
                    final Boolean newValue) {
        systemRecorder.recordEvent("APS", EventLevel.DETAILED, "MIGRATION_ON_GOING", "update of MIGRATION_ON_GOING invoked with new value: ",
                newValue.toString());
        migrationOnGoing = newValue;
    }

    /**
     * Method listens for @ConfigurationChangeNotification on redeliveryAttemptsForAlarmProcessor parameter changes.
     * @param newValue
     *            new value of redeliveryAttemptsForFMAlarmQueue.
     */
    public void listenForRedeliveryAttemptsForFMAlarmQueue(@Observes @ConfigurationChangeNotification(
            propertyName = "redeliveryAttemptsForFMAlarmQueue") final int changedRedeliveryAttempts) {
        LOGGER.info("redeliveryAttemptsForFMAlarmQueue value is changed to  {} ", changedRedeliveryAttempts);
        setRedeliveryAttempts(changedRedeliveryAttempts);
    }

    /**
     * Method listens for @ConfigurationChangeNotification on redeliveryDelayForFMAlarmQueue parameter changes.
     * @param newValue
     *            new value of redeliveryDelayForFMAlarmQueue.
     */
    public void listenForRedeliveryDelayForFMAlarmQueue(
            @Observes @ConfigurationChangeNotification(propertyName = "redeliveryDelayForFMAlarmQueue") final int changedRedeliveryDelay) {
        LOGGER.info("redeliveryDelayForFMAlarmQueue value is changed to  {} ", changedRedeliveryDelay);
        setRedeliveryDelay(changedRedeliveryDelay);
    }
}

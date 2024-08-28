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

package com.ericsson.oss.services.fm.alarmprocessor.instrumentation;

import static com.ericsson.oss.services.fm.common.addinfo.CorrelationType.NOT_APPLICABLE;
import static com.ericsson.oss.services.fm.common.addinfo.CorrelationType.PRIMARY;
import static com.ericsson.oss.services.fm.common.addinfo.CorrelationType.SECONDARY;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.ROOT;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.CollectionType;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.Visibility;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

/**
 * Bean responsible for Instrumenting AlarmProcessor statistics. It profiles Alarms Received ,Alarms Processed,Alarms count by Severity and the Alarms
 * Failed to persist in Database.
 */

@InstrumentedBean(displayName = "Alarm Processing Metrics", description = "Alarm Processor Records")
@ApplicationScoped
@Profiled
public class APSInstrumentedBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(APSInstrumentedBean.class);

    private int alarmsReceived;

    private int alarmsProcessed;

    private int alarmsFailedToProcess;

    private int deletedShortLivedAlarmsCount;

    private int failedFmxUpdateAlarmsCount;

    // Alarm Severities
    private int criticalAlarms;

    private int indeterminateAlarms;

    private int majorAlarms;

    private int minorAlarms;

    private int warningAlarms;

    private int clearAlarms;

    // Alarm Processed types
    private int newlyProcessedAlarms;

    private int correlatedAlarms;

    private int discardedAlarms;

    // Alarm root
    private int notApplicableAlarms;

    private int primaryAlarms;

    private int secondaryAlarms;

    // For Circuit Breaker

    private int alarmsProcessedBySecondaryConsumers;

    private int alarmsProcessingState;

    // For Staging Algorithm

    private int alarmsStaged;

    private int alarmsUnstaged;

    private int alarmsUnstagedByBackupTimer;

    public APSInstrumentedBean() {
        alarmsProcessed = 0;
        alarmsReceived = 0;
        alarmsFailedToProcess = 0;
        deletedShortLivedAlarmsCount = 0;
        failedFmxUpdateAlarmsCount = 0;
        indeterminateAlarms = 0;
        criticalAlarms = 0;
        majorAlarms = 0;
        minorAlarms = 0;
        warningAlarms = 0;
        clearAlarms = 0;
        newlyProcessedAlarms = 0;
        correlatedAlarms = 0;
        discardedAlarms = 0;
        notApplicableAlarms = 0;
        primaryAlarms = 0;
        secondaryAlarms = 0;

        alarmsProcessedBySecondaryConsumers = 0;
        // Values are 1 for RUNNING_NORMALLY, 2 for SOFT_THRESHOLD_CROSSED, and 3 for HARD_THRESHOLD_CROSSED
        alarmsProcessingState = 1;

        alarmsStaged = 0;
        alarmsUnstaged = 0;
        alarmsUnstagedByBackupTimer = 0;
    }

    @MonitoredAttribute(displayName = "Number of Indeterminate Alarms processed by APS", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getIndeterminateAlarmsProcessed() {
        return indeterminateAlarms;
    }

    @MonitoredAttribute(displayName = "Number of Critical Alarms processed by APS", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getCriticalAlarmsProcessedByAPS() {
        return criticalAlarms;
    }

    @MonitoredAttribute(displayName = "Number of Major Alarms processed by APS", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getMajorAlarmsProcessedByAPS() {
        return majorAlarms;
    }

    @MonitoredAttribute(displayName = "Number of Minor Alarms processed by APS", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getMinorAlarmsProcessedByAPS() {
        return minorAlarms;
    }

    @MonitoredAttribute(displayName = "Number of Warning Alarms processed by APS",  visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getWarningAlarmsProcessedByAPS() {
        return warningAlarms;
    }

    @MonitoredAttribute(displayName = "Number of Clear Alarms processed by APS", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getClearAlarmsProcessedByAPS() {
        return clearAlarms;
    }

    @MonitoredAttribute(displayName = "Number of Alarms Failed to process by APS", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getFailedAlarmCountByAPS() {
        return alarmsFailedToProcess;
    }

    @MonitoredAttribute(displayName = "Number of Short-lived Alarms Deleted from the Open Alarm DB", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getDeletedShortLivedAlarmsCount() {
        return deletedShortLivedAlarmsCount;
    }

    @MonitoredAttribute(displayName = "Number of failed FMX UPDATEs as the alarm does not exist in FM DB", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getFailedFmxUpdateAlarmsCount() {
        return failedFmxUpdateAlarmsCount;
    }

    @MonitoredAttribute(displayName = "Number of Alarms Received to Fault Management", visibility = Visibility.EXTERNAL,
            collectionType = CollectionType.TRENDSUP)
    public int getAlarmCountReceivedByAPS() {
        return alarmsReceived;
    }

    @MonitoredAttribute(displayName = "Number of Alarms Successfully Processed by Fault Management", visibility = Visibility.EXTERNAL,
            collectionType = CollectionType.TRENDSUP)
    public int getAlarmProcessedByAPS() {
        return alarmsProcessed;
    }

    @MonitoredAttribute(displayName = "AlarmRootNotApplicableProcessedByAPS", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getAlarmRootNotApplicableProcessedByAPS() {
        return notApplicableAlarms;
    }

    @MonitoredAttribute(displayName = "AlarmRootPrimaryProcessedByAPS", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getAlarmRootPrimaryProcessedByAPS() {
        return primaryAlarms;
    }

    @MonitoredAttribute(displayName = "AlarmRootSecondaryProcessedByAPS", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getAlarmRootSecondaryProcessedByAPS() {
        return secondaryAlarms;
    }

    @MonitoredAttribute(displayName = "Number of Alarms Processed by SecondaryConsumers", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getAlarmsProcessedBySecondaryConsumers() {
        return alarmsProcessedBySecondaryConsumers;
    }

    @MonitoredAttribute(displayName = "State of Alarm Processing performance w.r.t Circuit Breaker", visibility = Visibility.ALL,
            collectionType = CollectionType.DYNAMIC)
    public int getAlarmsProcessingState() {
        return alarmsProcessingState;
    }

    @MonitoredAttribute(displayName = "Number of Alarms Staged into the TransientAlarmStagingCache", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getAlarmsStaged() {
        return alarmsStaged;
    }

    @MonitoredAttribute(displayName = "Number of Alarms Unstaged from the TransientAlarmStagingCache", visibility = Visibility.ALL,
            collectionType = CollectionType.TRENDSUP)
    public int getAlarmsUnstaged() {
        return alarmsUnstaged;
    }

    @MonitoredAttribute(displayName = "Number of Alarms Unstaged from the TransientAlarmStagingCache by the Backup Timer",
            visibility = Visibility.ALL, collectionType = CollectionType.TRENDSUP)
    public int getAlarmsUnstagedByBackupTimer() {
        return alarmsUnstagedByBackupTimer;
    }

    public void incrementAlarmsStagedCount() {
        alarmsStaged++;
        LOGGER.debug("alarmsStaged count: {} ", alarmsStaged);
    }

    public void incrementAlarmsUnstagedCount() {
        alarmsUnstaged++;
        LOGGER.debug("alarmsUnstaged count: {} ", alarmsUnstaged);
    }

    public void incrementAlarmsUnstagedByBackupTimerCount() {
        alarmsUnstagedByBackupTimer++;
        LOGGER.debug("alarmsUnstaged count: {} ", alarmsUnstagedByBackupTimer);
    }

    public void increaseAlarmCountReceivedByAPS() {
        alarmsReceived++;
        LOGGER.debug("alarmCountReceivedByAPS {} ", alarmsReceived);
    }

    public void incrementAlarmCount() {
        alarmsProcessed++;
        LOGGER.debug("alarmProcessedByAPS {} ", alarmsProcessed);
    }

    public void incrementAlarmsProcessedBySecondaryConsumers(final int incrementBy) {
        alarmsProcessedBySecondaryConsumers += incrementBy;
        LOGGER.debug("alarmsProcessedBySecondaryConsumers incemented to {}", alarmsProcessedBySecondaryConsumers);
    }

    public void setAlarmsProcessingState(final int state) {
        alarmsProcessingState = state;
        LOGGER.debug("alarmsProcessingState changed to {}", alarmsProcessingState);
    }

    public void incrementFailedAlarmCount() {
        alarmsFailedToProcess++;
        LOGGER.debug("failedAlarmCountByAPS {} ", alarmsFailedToProcess);
    }

    public void incrementDeletedShortLivedAlarmsCount() {
        deletedShortLivedAlarmsCount++;
        LOGGER.debug("deletedShortLivedAlarmsCount {} ", deletedShortLivedAlarmsCount);
    }

    public void incrementFailedFmxUpdateAlarmsCount() {
        failedFmxUpdateAlarmsCount++;
        LOGGER.debug("failedFmxUpdateAlarmsCount {} ", failedFmxUpdateAlarmsCount);
    }
    public void incrementNewlyProcessedAlarmCount(final ProcessedEventSeverity severity) {
        incrementAlarmAndSeverityCount(severity);
        newlyProcessedAlarms++;
        LOGGER.trace("createdAlarms {} ", newlyProcessedAlarms);
    }

    public void incrementCorrelatedProcessedAlarmCount(final ProcessedEventSeverity severity) {
        incrementAlarmAndSeverityCount(severity);
        correlatedAlarms++;
        LOGGER.trace("updatedAlarms {} ", correlatedAlarms);
    }

    public void incrementDiscardedAlarmCount(final ProcessedEventSeverity processedEventSeverity) {
        incrementAlarmAndSeverityCount(processedEventSeverity);
        discardedAlarms++;
        LOGGER.trace("discardedAlarms {} ", discardedAlarms);
    }

    private void incrementAlarmAndSeverityCount(final ProcessedEventSeverity severity) {
        incrementAlarmCount();
        switch (severity) {
            case INDETERMINATE:
                indeterminateAlarms++;
                break;
            case CRITICAL:
                criticalAlarms++;
                break;
            case MAJOR:
                majorAlarms++;
                break;
            case MINOR:
                minorAlarms++;
                break;
            case WARNING:
                warningAlarms++;
                break;
            case CLEARED:
                clearAlarms++;
                break;
            default:
                break;
        }
    }

    public void incrementAlarmRootCounters(final Map<String, Object> alarmAttributes) {
        if (alarmAttributes.get(ROOT) != null) {
            if (((String) alarmAttributes.get(ROOT)).equals(PRIMARY.name())) {
                primaryAlarms++;
            } else if (((String) alarmAttributes.get(ROOT)).equals(SECONDARY.name())) {
                secondaryAlarms++;
            } else if (((String) alarmAttributes.get(ROOT)).equals(NOT_APPLICABLE.name())) {
                notApplicableAlarms++;
            }
        } else {
            notApplicableAlarms++;
        }
    }
}

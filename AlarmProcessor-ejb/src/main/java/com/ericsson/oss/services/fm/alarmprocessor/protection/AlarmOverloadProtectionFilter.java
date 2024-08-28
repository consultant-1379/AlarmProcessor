/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.protection;

import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.ALARM;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.ERROR_MESSAGE;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.HEARTBEAT_ALARM;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.NON_SYNCHABLE_ALARM;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.REPEATED_ALARM;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.REPEATED_ERROR_MESSAGE;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.REPEATED_NON_SYNCHABLE;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.SYNCHRONIZATION_ALARM;
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity.CLEARED;
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity.CRITICAL;
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity.INDETERMINATE;
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity.MAJOR;
import static com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdCrossed.ON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.AOPInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

/**
 * Class is responsible for applying filter according the default alarm protection rule.
 */
@ApplicationScoped
public class AlarmOverloadProtectionFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmOverloadProtectionFilter.class);

    private static final List<ProcessedEventSeverity> SEVERITIES = new ArrayList<>(Arrays.asList(
            CRITICAL,
            MAJOR,
            INDETERMINATE,
            CLEARED
    ));

    private static final List<FMProcessedEventType> TYPES = new ArrayList<>(Arrays.asList(
            ALARM,
            REPEATED_ALARM,
            SYNCHRONIZATION_ALARM,
            HEARTBEAT_ALARM,
            NON_SYNCHABLE_ALARM,
            REPEATED_NON_SYNCHABLE,
            ERROR_MESSAGE,
            REPEATED_ERROR_MESSAGE
    ));

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private AlarmOverloadProtectionService alarmOverloadProtectionService;

    @Inject
    private AOPInstrumentedBean aopInstrumentedBean;

    /**
     * Method which checks if alarm overload protection is enabled and if filter is necessary.
     * @param {@link ProcessedAlarmEvent} processedAlarmEvent
     * @return boolean
     */
    public boolean applyAlarmOverloadProtectionFilter(final ProcessedAlarmEvent processedAlarmEvent) {
        /* 1) if alarm overload protection is disabled all alarms/alerts must be processed
           2) if alarm overload protection is not ON all alarms/alerts must be processed
           3) if alarm overload protection is ON alarms/alerts are processed according the following rules:
              alarms with severity CRITICAL, MAJOR, INDETERMINATE, CLEARED and recordType ALARM, SYNCHRONIZATION_ALARM, REPEATED_ALARM,
              HEARTBEAT_ALARM, NON_SYNCHABLE_ALARM, REPEATED_NON_SYNCHABLE must be kept, otherwise they will be discarded
              alerts with severity CRITICAL, MAJOR, INDETERMINATE, CLEARED and recordType ERROR_MESSAGE,
              REPEATED_ERROR_MESSAGE must be kept, otherwise they will be discarded */
        if (!configParametersListener.getAlarmOverloadProtectionOn()) {
            return true;
        }
        if (alarmOverloadProtectionService.getSafeMode() != ON) {
            return true;
        }
        return matchAlarmOverloadProtectionFilter(processedAlarmEvent);
    }

    /**
     * Method which checks if alarm match the default alarm protection rule.
     * @param {@link ProcessedAlarmEvent} processedAlarmEvent
     * @return boolean
     */
    private boolean matchAlarmOverloadProtectionFilter(final ProcessedAlarmEvent processedAlarmEvent) {
        final String fdn = processedAlarmEvent.getFdn();
        final ProcessedEventSeverity severity = processedAlarmEvent.getPresentSeverity();
        final FMProcessedEventType recordType = processedAlarmEvent.getRecordType();
        if (fdn.isEmpty()) {
            LOGGER.debug("ProcessedAlarmEvent contains fdn empty !");
        }
        if (!SEVERITIES.contains(severity)) {
            LOGGER.debug("DISCARDED PRESENT SEVERITY: {}", severity);
            if (isAlert(recordType)) {
                aopInstrumentedBean.increaseAlertCountDiscardedByAPS();
            } else {
                aopInstrumentedBean.increaseAlarmCountDiscardedByAPS();
            }
            if (!fdn.isEmpty()) {
                alarmOverloadProtectionService.updateStateToAlarmSuppressed(fdn);
            }
            return false;
        }
        if (!TYPES.contains(recordType)) {
            LOGGER.debug("DISCARDED RECORD TYPE: {}", recordType);
            aopInstrumentedBean.increaseAlarmCountDiscardedByAPS();
            if (!fdn.isEmpty()) {
                alarmOverloadProtectionService.updateStateToAlarmSuppressed(fdn);
            }
            return false;
        }
        LOGGER.debug("PRESENT SEVERITY: {}, RECORD TYPE: {}", severity, recordType);
        return true;
    }

    /**
     * Method which checks if record type refer to an alert.
     * @param {@link FMProcessedEventType} recordType
     * @return boolean
     */
    private boolean isAlert(final FMProcessedEventType recordType) {
        return ERROR_MESSAGE.equals(recordType) || REPEATED_ERROR_MESSAGE.equals(recordType);
    }
}

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

package com.ericsson.oss.services.fm.alarmprocessor.validators;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ORIGINAL_EVENTTIME_FROM_NODE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.INTERNAL_ALARM_FDN;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.NetworkElementMoReader;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

/**
 * Class for validating alarm.
 */
public class AlarmValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmValidator.class);

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private NetworkElementMoReader networkElementMoReader;

    /**
     * Validate alarm based on EventTime . Alarm is considered valid if EventTime is greater than the correlated Alarm present in database.
     */
    public boolean isAlarmValid(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        Date alarmEventTime = alarmRecord.getEventTime();
        Date correlatedAlarmEventTime = correlatedAlarm.getEventTime();
        final String alarmOriginalEventTime = alarmRecord.getAdditionalInformation().get(ORIGINAL_EVENTTIME_FROM_NODE);
        final String correlatedAlarmOriginalEventTime = correlatedAlarm.getAdditionalInformation().get(ORIGINAL_EVENTTIME_FROM_NODE);
        if (alarmOriginalEventTime != null && correlatedAlarmOriginalEventTime != null) {
            alarmEventTime = new Date(Long.parseLong(alarmOriginalEventTime));
            correlatedAlarmEventTime = new Date(Long.parseLong(correlatedAlarmOriginalEventTime));
        }
        // If clear is processed before hide, returning true as event time of received alarm will be before event time of correlated alarm in DB.
               if (ProcessedEventSeverity.CLEARED.equals(correlatedAlarm.getPresentSeverity())
                        && FMX_HIDE.equals(alarmRecord.getAdditionalInformation().get(HIDE_OPERATION))

                        ||(NOT_SET.equals(alarmRecord.getFmxGenerated())&&NOT_SET.equals(correlatedAlarm.getFmxGenerated())&& !alarmEventTime.equals(correlatedAlarmEventTime))

                || ((!alarmEventTime.before(correlatedAlarmEventTime))
                && ((correlatedAlarm.getCeaseTime() == null)
                || correlatedAlarm.getManualCease()
                || (!alarmEventTime.before(correlatedAlarm.getCeaseTime()))))) {
            return true;
        } else {
            systemRecorder.recordEvent("APS", EventLevel.DETAILED, "FM",
                    "Discarding alarm as its EventTime is less than the similar alarm already present in Database", alarmRecord.toString());
            apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
            return false;
        }
    }

    /**
     * Method which checks if the alarm should be processed further or not.
     *
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    public boolean isAlarmToBeHandled(final ProcessedAlarmEvent alarmRecord) {
        if (FMProcessedEventType.UNKNOWN_RECORD_TYPE.equals(alarmRecord.getRecordType())
                || FMProcessedEventType.SYNCHRONIZATION_IGNORED.equals(alarmRecord.getRecordType())) {
            LOGGER.warn("Alarm will not be processed further as there are no rules applicable to handle this alarm {}", alarmRecord);
            apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
            return false;
        }
        if (alarmRecord.getObjectOfReference() == null || alarmRecord.getObjectOfReference().isEmpty()) {
            systemRecorder.recordEvent("APS", EventLevel.DETAILED, "FM",
                    "Alarm is received with no OOR set.Alarm will not be processed further.", alarmRecord.toString());
            apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
            return false;
        }
        return true;
    }

    /**
     * Checks if a MO exists with given FDN in database.
     *
     * @param String
     *            fdn
     * @return true if MO exists or FDN is equals to ENM, false if MO does not exists.
     */
    public boolean isNetworkElementExists(final String fdn) {
        boolean networkElementExists = false;
        if (fdn != null) {
            if (INTERNAL_ALARM_FDN.equals(fdn)) {
                networkElementExists = true;
            } else {
                if (networkElementMoReader.read(fdn) != null) {
                    networkElementExists = true;
                }
            }
        }
        return networkElementExists;
    }

    /**
     * Method which checks if the alarm should be counted for alarm protection (only ALARM and ERROR_MESSAGE without FMX).
     *
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    public boolean isAlarmToBeCounted(final ProcessedAlarmEvent alarmRecord) {
        if(NOT_SET.equals(alarmRecord.getFmxGenerated())) {
            switch (alarmRecord.getRecordType()) {
                case ALARM:
                case REPEATED_ALARM:
                case ERROR_MESSAGE:
                case REPEATED_ERROR_MESSAGE:
                case NON_SYNCHABLE_ALARM:
                case REPEATED_NON_SYNCHABLE:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }
}

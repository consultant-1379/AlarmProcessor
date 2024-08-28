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

package com.ericsson.oss.services.fm.alarmprocessor.processors;

import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.populateUpdateAlarm;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.evaluateTrendIndication;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.INSERT_TIME;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_PROCESSED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;

/**
 * Class for replacing the correlated alarm present in db with the received alarm.
 */
public class ReplaceAlarmProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplaceAlarmProcessor.class);

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private ServiceStateModifier serviceStateModifier;

    @Inject
    private ConfigParametersListener configParametersListener;

    /**
     * Method for replacing alarm in database with received alarm whose record type is ALARM.
     *
     * @param correleatedAlarm
     *            {@link ProcessedAlarmEvent} correlatedAlarm
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    public void processNormalAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correleatedAlarm) {
        if (FMX_PROCESSED.equalsIgnoreCase(alarmRecord.getFmxGenerated())) {
            LOGGER.debug("Replacing Alarm with fmx processed Alarm {}", alarmRecord);
            processReplaceAlarm(alarmRecord, correleatedAlarm);
        } else {
            alarmRecord.setTrendIndication(evaluateTrendIndication(alarmRecord.getPresentSeverity(),
                    alarmRecord.getPreviousSeverity()));
            LOGGER.info("Duplicate Alarm !! Replacing existing alarm with the received alarm Event PoId {}.", alarmRecord.getEventPOId());
            LOGGER.debug("Duplicate Alarm !! Replacing existing alarm with the received alarm {}.", alarmRecord);
            processRepeatedAlarm(alarmRecord, correleatedAlarm);
        }
    }

    /**
     * Method called for repeated alarm in database with received alarm whose record type is Repeated_Alarm or Repeated_Non_Sync.
     *
     * @param correleatedAlarmRecord
     *            {@link ProcessedAlarmEvent} correlatedAlarm
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    public void processRepeatedAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        updateAlarmDetails(alarmRecord);
        final Map<String, Object> pOAttributes = alarmReader.readAllAttributes(alarmRecord.getCorrelatedPOId());
        final Map<String, Object> alarmAttributes = alarmAttributesPopulator.populateUpdateAlarm(alarmRecord, pOAttributes);
        alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, correlatedAlarm, alarmAttributes);
        openAlarmService.updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
        apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
    }

    /**
     * Method called for replacing alarm in database with received alarm.
     *
     * @param correlatedAlarm
     *            alarmRecord present in database.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    //TODO:Check whether additionalAttributes are to be retained or not.
    public void processReplaceAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        updateAlarmDetails(alarmRecord);
        //The last alarm operation should be NEW only when the alarm is first shown. So, Verifying if the correlated alarm contains hideOperation
        //in additional attributes which should be present for any alarms which are already shown/hidden by FMX. CorrelatedVisibility is verified
        //to avoid setting last alarm operation NEW for the alarms which are being hidden by FMX.
        if (!correlatedAlarm.getAdditionalInformation().containsKey(HIDE_OPERATION)
                && alarmRecord.getAdditionalInformation().containsKey(HIDE_OPERATION) && alarmRecord.getCorrelatedVisibility() != null
                && !alarmRecord.getCorrelatedVisibility()) {
            alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.NEW);
        }
        final Map<String, Object> pOAttributes = alarmReader.readAllAttributes(alarmRecord.getCorrelatedPOId());
        setInsertTime(alarmRecord, (Date) pOAttributes.get(INSERT_TIME));
        final Map<String, Object> alarmAttributes = populateUpdateAlarm(alarmRecord);
        alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, correlatedAlarm, alarmAttributes);
        openAlarmService.updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
        apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
    }

    /**
     * Update the InsertTime in ProcessedAlarmEvent.
     *
     * @param ProcessedAlarmEvent
     *            processedAlarmEvent
     * @param Date
     *            oldInsertTime
     */
    private void setInsertTime(final ProcessedAlarmEvent alarm, final Date oldInsertTime) {
        if (!configParametersListener.getUpdateInsertTime()) {
            alarm.setInsertTime(oldInsertTime);
        } else {
            final Date insertTime = new Date();
            alarm.setInsertTime(new Date(insertTime.getTime()));
        }
    }

    /**
     * Method for setting ProcessedAlarmEvent's lastUpdatedTime, LastAlarmOperation and updates service state and record type based on SP.
     *
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    private void updateAlarmDetails(final ProcessedAlarmEvent alarmRecord) {
        final Date lastUpdated = new Date();
        alarmRecord.setLastUpdatedTime(lastUpdated);
        alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.CHANGE);
        if (FMProcessedEventType.NON_SYNCHABLE_ALARM.equals(alarmRecord.getRecordType())
                || FMProcessedEventType.REPEATED_NON_SYNCHABLE.equals(alarmRecord.getRecordType())) {
            alarmRecord.setRecordType(FMProcessedEventType.REPEATED_NON_SYNCHABLE);
        } else if (!FMX_PROCESSED.equalsIgnoreCase(alarmRecord.getFmxGenerated())
                && !(FMProcessedEventType.HEARTBEAT_ALARM.equals(alarmRecord.getRecordType())
                        || FMProcessedEventType.OUT_OF_SYNC.equals(alarmRecord.getRecordType())
                        || FMProcessedEventType.NODE_SUSPENDED.equals(alarmRecord.getRecordType())
                        || FMProcessedEventType.TECHNICIAN_PRESENT.equals(alarmRecord.getRecordType())
                        || FMProcessedEventType.ALARM_SUPPRESSED_ALARM.equals(alarmRecord.getRecordType()))) {
            alarmRecord.setRecordType(FMProcessedEventType.REPEATED_ALARM);
        }
        if (!(FMProcessedEventType.TECHNICIAN_PRESENT.equals(alarmRecord.getRecordType()) || FMProcessedEventType.ALARM_SUPPRESSED_ALARM
                .equals(alarmRecord.getRecordType()))) {
            serviceStateModifier.updateFmFunctionBasedOnSpecificProblem(alarmRecord);
        }
    }
}
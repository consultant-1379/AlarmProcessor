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

import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.populateNewAlarm;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.COMMENT_TEXT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OSCILLATION_COUNT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.REPEAT_COUNT;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.EMPTY_STRING;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;

/**
 * Class for processing oscillation correlation when the correlated alarm in db is cleared.
 */
public class OscillationCorrelationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OscillationCorrelationProcessor.class);

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private ServiceStateModifier serviceStateModifier;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Inject
    private AlarmReader alarmReader;

    /**
     * Method handles oscillation correlation, 1. replaces correlated alarm in db with received alarm when oscillation correlation in ON, 2. inserts
     * alarm in db when oscillation correlation in OFF.
     *
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     */
    public void processAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        LOGGER.debug(
                "Oscillation Correlation is set to {}.Performing Oscilating_correlation for alarm {}",
                configParametersListener.getOscillatingCorrelation(), alarmRecord);
        // oscillation ON case
        if (configParametersListener.getOscillatingCorrelation()) {
            alarmRecord.setRecordType(getRecordType(alarmRecord.getRecordType()));
            alarmRecord.setAlarmId(correlatedAlarm.getAlarmId());
            alarmRecord.setAlarmNumber(correlatedAlarm.getAlarmNumber());
            alarmRecord.setPreviousSeverity(correlatedAlarm.getPresentSeverity());
            alarmRecord.setCorrelatedPOId(correlatedAlarm.getEventPOId());
            alarmRecord.setEventPOId(alarmRecord.getCorrelatedPOId());
            int oscillationCount = 0;
            int repeatCount = 0;
            String commentText = EMPTY_STRING;
            final Map<String, Object> correlatedAlarmAttributes = alarmReader.readAllAttributes(alarmRecord.getCorrelatedPOId());
            LOGGER.debug("PO attributes obtained: {}", correlatedAlarmAttributes);
            final Object oscillationCountObject = correlatedAlarmAttributes.get(OSCILLATION_COUNT);
            final Object repeatCountObject = correlatedAlarmAttributes.get(REPEAT_COUNT);
            final Object commentTextObject = correlatedAlarmAttributes.get(COMMENT_TEXT);
            if (oscillationCountObject != null) {
                oscillationCount = (int) oscillationCountObject;
            }
            if (repeatCountObject != null) {
                repeatCount = (int) repeatCountObject;
            }
            if (commentTextObject != null) {
                commentText = commentTextObject.toString();
            }
            final int incrementedRepeatCount = ++repeatCount;
            final int incrementedOscillationCount = ++oscillationCount;
            final Map<String, String> additionalInformation = alarmRecord.getAdditionalInformation();
            additionalInformation.put(REPEAT_COUNT, String.valueOf(incrementedRepeatCount));
            additionalInformation.put(OSCILLATION_COUNT, String.valueOf(incrementedOscillationCount));
            alarmRecord.setAdditionalInformation(additionalInformation);
            alarmRecord.setRepeatCount(Integer.valueOf(incrementedRepeatCount));
            alarmRecord.setOscillationCount(Integer.valueOf(incrementedOscillationCount));
            alarmRecord.setCommentText(commentText);
            final Date lastUpdated = new Date();
            alarmRecord.setLastUpdatedTime(lastUpdated);
            alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.CHANGE);
            serviceStateModifier.updateFmFunctionBasedOnSpecificProblem(alarmRecord);
            apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
            final Map<String, Object> alarmAttributes = alarmAttributesPopulator.populateUpdateAlarm(alarmRecord, correlatedAlarmAttributes);
            alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, correlatedAlarm, alarmAttributes);
            openAlarmService.updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
            LOGGER.debug("Oscillation Correlation Performed.Alarm now is {}", alarmRecord);
        } else {
            // oscillation OFF case
            serviceStateModifier.updateFmFunctionBasedOnSpecificProblem(alarmRecord);
            alarmRecord.setCorrelatedPOId(AlarmProcessorConstants.DEFAULT_EVENTPOID_VALUE);
            final Map<String, Object> alarmAtributes = populateNewAlarm(alarmRecord);
            alarmRecord.setEventPOId(openAlarmService.insertAlarmRecord(alarmAtributes));
            LOGGER.debug("Inserted Alarm in OpenAlarm Namespace of Database.{}", alarmRecord);
            apsInstrumentedBean.incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
            alarmRecord.setActionState(ProcessedLastAlarmOperation.NEW);
        }
    }

    private FMProcessedEventType getRecordType(final FMProcessedEventType recordType) {
        switch (recordType) {
            case ALARM:
            case SYNCHRONIZATION_ALARM:
                return FMProcessedEventType.REPEATED_ALARM;
            case HEARTBEAT_ALARM:
            case HB_FAILURE_NO_SYNCH:
                return FMProcessedEventType.OSCILLATORY_HB_ALARM;
            case NON_SYNCHABLE_ALARM:
                return FMProcessedEventType.REPEATED_NON_SYNCHABLE;
            default:
                return recordType;
        }
    }
}
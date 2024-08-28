/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.populateNewAlarm;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_PROCESSED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedUpdateAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;

/**
 * Class to handle Alarms with RecordType ERROR_MESSAGE and REPEATED_ERROR_MESSAGE.
 */
public class ErrorAlarmCommonHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorAlarmCommonHandler.class);

    @Inject
    private AlarmCorrelator alarmCorrelator;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private ServiceStateModifier serviceStateModifier;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Inject
    private CorrelatedUpdateAlarmProcessor correlatedUpdateAlarmProcessor;

    /**
     * Method to process alarms with RecordType ERROR_MESSAGE,REPEATED_ERROR_MESSAGE. Clear alarm for ERROR_MESSAGE,REPEATED_ERROR_MESSAGE will not be
     * processed as these are State less Events. Alarm Correlation is performed for the received event . If Correlated event is not found the event is
     * inserted into database else it is updated.
     *
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @return {@link AlarmProcessingResponse} alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        LOGGER.debug("Error Alarm received to ErrorAlarmCommonHandler : {}", alarmRecord);
        if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())) {
            LOGGER.debug("Clear request for an Error/Repeated Error Message will not be processed.Discarding it!! : {}", alarmRecord);
            apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
        } else {
            final ProcessedAlarmEvent correlatedAlarm = alarmCorrelator.getCorrelatedAlarm(alarmRecord);
            if (null != correlatedAlarm && correlatedAlarm.getEventPOId() > 0) {
                alarmRecord.setPreviousSeverity(correlatedAlarm.getPresentSeverity());
                alarmRecord.setCorrelatedPOId(correlatedAlarm.getEventPOId());
                if (!FMX_PROCESSED.equals(alarmRecord.getFmxGenerated())) {
                    alarmRecord.setRecordType(FMProcessedEventType.REPEATED_ERROR_MESSAGE);
                }
                alarmRecord.setAlarmState(correlatedAlarm.getAlarmState());
                alarmRecord.setEventPOId(correlatedAlarm.getEventPOId());
                final String hideOperation = alarmRecord.getAdditionalInformation().get(HIDE_OPERATION);
                // This case is for handling when FMX update is received on an event which is already in hidden state.
                if (hideOperation != null && FMX_HIDE.equals(hideOperation) && !alarmRecord.isVisibility()) {
                        LOGGER.debug("Removing the ERROR message as the hide request is received: {} ", alarmRecord);
                        final Map<String, Object> alarmAttributes = alarmAttributesPopulator.populateDeleteAlarm(alarmRecord, correlatedAlarm);
                        alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, correlatedAlarm, alarmAttributes);
                        openAlarmService.removeAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
                        alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
                        apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
                        return alarmProcessingResponse;
                }
                updateAlarmInDataBase(alarmRecord, correlatedAlarm);
                apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
                correlatedUpdateAlarmProcessor.sendFakeClearToUiAndNbi(alarmRecord, correlatedAlarm, alarmProcessingResponse);
            } else {
                alarmRecord.setAlarmState(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
                final Map<String, Object> alarmAttributes = populateNewAlarm(alarmRecord);
                alarmRecord.setEventPOId(openAlarmService.insertAlarmRecord(alarmAttributes));
                alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.NEW);
                apsInstrumentedBean.incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
            }
            alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
        }
        return alarmProcessingResponse;
    }

    /**
     * Method for replacing alarm in database with received alarm whose record type is SYNCHRONIZATION_ALARM, ERROR_MESSAGE, REPEATED_ERROR_MESSAGE.
     *
     * @param alarmRecord
     *            The new alarm record received.
     * @param correlatedAlarm
     *            The correlated alarm found in database.
     */
    private void updateAlarmInDataBase(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        setDataForReplacingAlarm(alarmRecord);
        if (!correlatedAlarm.getAdditionalInformation().containsKey(HIDE_OPERATION)
                && alarmRecord.getAdditionalInformation().containsKey(HIDE_OPERATION) && alarmRecord.getCorrelatedVisibility() != null
                && !alarmRecord.getCorrelatedVisibility()) {
            alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.NEW);
        }
        final Map<String, Object> correlatedPoIdAttributes = alarmReader.readAllAttributes(alarmRecord.getCorrelatedPOId());
        final Map<String, Object> alarmAttributes = alarmAttributesPopulator.populateUpdateAlarm(alarmRecord, correlatedPoIdAttributes);
        alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, correlatedAlarm, alarmAttributes);
        openAlarmService.updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
    }

    /**
     * Method for setting ProcessedAlarmEvent's lastUpdatedTime, LastAlarmOperation and updates service state and record type based on SP.
     *
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    private void setDataForReplacingAlarm(final ProcessedAlarmEvent alarmRecord) {
        final Date lastUpdated = new Date();
        alarmRecord.setLastUpdatedTime(lastUpdated);
        alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.CHANGE);
        serviceStateModifier.updateFmFunctionBasedOnSpecificProblem(alarmRecord);
    }
}
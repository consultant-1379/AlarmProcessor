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

package com.ericsson.oss.services.fm.alarmprocessor.eventhandlers;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_RECEIVED_TIME;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ORIGINAL_RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.SOURCE_TYPE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.INTERNAL_RETRYS_REACHED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.fm.capability.util.ModelCapabilities;
import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.SyncInitiator;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.UnProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability.DatabaseStatusProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder.ProcessingState;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * ProcessedAlarmEvent received from {@link AlarmPreProcessor} will be delegated to @{AlarmHandlerBean} for
 * further processing based on record type. Send processed alarm to NorthBound.
 */
public class AlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmHandler.class);

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private DatabaseStatusProcessor versantDbStatusHolder;

    @Inject
    private ActiveThreadsHolder activeThreadsHolder;

    @Inject
    private AlarmHandlerBean alarmHandlerBean;

    @Inject
    private UnProcessedAlarmSender unProcessedAlarmSender;

    @Inject
    private ProcessedAlarmSender processedAlarmSender;

    @Inject
    private ModelCapabilities modelCapabilities;

    @Inject
    private SyncInitiator syncInitiator;

    /**
     * Receives {@link ProcessedAlarmEvent} from {@link AlarmPreProcessor}.<br>
     * <p>
     * Received alarm is processed further based on record type and sent to NorthBound. Alarm processing is retried for 5 times if there is any
     * exception while processing it.
     */
    public void onEvent(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Alarm received to AlarmHandler {}", alarmRecord);
        String alarmReceivedTime = null;
        if (alarmRecord.getAdditionalInformation().containsKey(ALARM_RECEIVED_TIME)) {
            alarmReceivedTime = alarmRecord.getAdditionalInformation().get(ALARM_RECEIVED_TIME);
            alarmRecord.getAdditionalInformation().remove(ALARM_RECEIVED_TIME);
        }
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        int retrycount = 0;
        boolean retryFlag = true;
        int maxRetryCount = configParametersListener.getRetryLimitForAlarmProcessing();
        if (maxRetryCount <= 0) {
            // Default it to 5 in case of Improper value and Config Storage not accessible.
            maxRetryCount = 5;
        }
        while (retryFlag && retrycount < maxRetryCount && (versantDbStatusHolder.isDatabaseAvailable()
                && ProcessingState.RUNNING_NORMALLY.equals(activeThreadsHolder.getProcessingState()))) {
            alarmProcessingResponse = alarmHandlerBean.processAlarm(alarmRecord);
            retryFlag = alarmProcessingResponse.isRetryFlag();
            ++retrycount;
        }
        removeUnusedAttributeFromAdditionalInfo(alarmProcessingResponse);
        //Check retryFlag in scenario alarm is successfully process in the last retry attempt
        if ((retrycount == maxRetryCount && retryFlag) || !versantDbStatusHolder.isDatabaseAvailable()
                || !ProcessingState.RUNNING_NORMALLY.equals(activeThreadsHolder.getProcessingState())) {
            unProcessedAlarmSender.sendUnProcessedEvents(alarmRecord, INTERNAL_RETRYS_REACHED);
        } else {
            processedAlarmSender.sendAlarms(alarmProcessingResponse, alarmReceivedTime);
            checkIfAlarmSyncIsToBeInitiated(alarmRecord, alarmProcessingResponse);
        }
    }

    private void removeUnusedAttributeFromAdditionalInfo(final AlarmProcessingResponse alarmProcessingResponse) {
        for (final ProcessedAlarmEvent alarm : alarmProcessingResponse.getProcessedAlarms()) {
            if (alarm.getAdditionalInformation() != null) {
                alarm.getAdditionalInformation().remove(ORIGINAL_RECORD_TYPE);
            }
        }
    }

    private void checkIfAlarmSyncIsToBeInitiated(final ProcessedAlarmEvent alarmRecord, final AlarmProcessingResponse alarmProcessingResponse) {
        if (alarmProcessingResponse.isInitiateAlarmSync()) {
            boolean alarmSyncSupported = true;
            try {
                final String targetType = alarmRecord.getAdditionalInformation().get(SOURCE_TYPE);
                alarmSyncSupported = modelCapabilities.isAlarmSyncSupportedByNode(targetType);
            } catch (final Exception exception) {
                LOGGER.warn("Exception thrown while retrieving alarmSyncsupported for fdn:{} is:{}.Defaults to true",
                        alarmRecord.getFdn(), exception.getMessage());
                LOGGER.debug("Exception thrown while retrieving alarmSyncsupported for fdn:{} is:{}.Defaults to true",
                        alarmRecord, exception);
                alarmSyncSupported = true;
            }
            if (alarmSyncSupported) {
                syncInitiator.initiateAlarmSynchronization(alarmRecord.getFdn());
            } else {
                LOGGER.debug("Alarm Synchronization not supported by fdn:{} RecordType:{}", alarmRecord.getFdn(), alarmRecord.getRecordType());
            }
        }
    }
}

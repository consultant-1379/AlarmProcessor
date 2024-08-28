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

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.alarm.staging.AlarmStagingHandler;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

/**
 * Class handles alarms with record type REPEATED_ALARM.
 */
public class RepeatedAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepeatedAlarmHandler.class);

    @Inject
    private AlarmProcessor newAlarmProcessor;

    @Inject
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Inject
    private AlarmValidator alarmValidator;

    @Inject
    private AlarmCorrelator alarmCorrelator;

    @Inject
    private NormalAlarmHandler normalAlarmHandler;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private AlarmStagingHandler alarmStagingHandler;

    /**
     * Method handles alarms with record type {@link FMProcessedEventType#REPEATED_ALARM} when there is a correlated alarm and when there is no
     * correlated alarm in database.
     *
     * @param {@link ProcessedAlarmEvent} --alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Alarm received to RepeatedAlarmHandler : {}", alarmRecord);
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        // change record type to alarm when repeated alarm with severity cleared is received
        if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())) {
            alarmRecord.setRecordType(FMProcessedEventType.ALARM);
            alarmProcessingResponse = normalAlarmHandler.handleAlarm(alarmRecord);
            LOGGER.debug("Received Clear on Repeated Alarm for OOR {} ,Recordtype is changed to Alarm", alarmRecord.getObjectOfReference());
            return alarmProcessingResponse;
        }
        final ProcessedAlarmEvent correlatedAlarm = alarmCorrelator.getCorrelatedAlarm(alarmRecord);
        if (correlatedAlarm != null && correlatedAlarm.getEventPOId() > 0) {
            if (configParametersListener.getTransientAlarmStaging() && NOT_SET.equals(alarmRecord.getFmxGenerated())) {
                LOGGER.debug("Staging PIB parameter is enabled! Check and Stage the alarm if needed");
                if (alarmStagingHandler.checkAndStageAlarm(alarmRecord, correlatedAlarm)) {
                    // Transient alarm is staged!
                    // Will return here and the alarm will be re-processed once the stage timer expires.
                    return alarmProcessingResponse;
                }
            }
            if (alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)) {
                alarmProcessingResponse = correlatedAlarmProcessor.processRepeated(alarmRecord, correlatedAlarm);
            }
        } else {
            alarmProcessingResponse = newAlarmProcessor.processAlarm(alarmRecord);
        }
        return alarmProcessingResponse;
    }
}
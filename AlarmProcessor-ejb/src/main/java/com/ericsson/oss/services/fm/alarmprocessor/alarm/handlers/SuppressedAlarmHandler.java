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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

/**
 * Class handles alarms with record type ALARM_SUPPRESSED_ALARM.
 */
public class SuppressedAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuppressedAlarmHandler.class);

    @Inject
    private ServiceStateModifier serviceStateModifier;

    @Inject
    private AlarmProcessor newAlarmProcessor;

    @Inject
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Inject
    private AlarmCorrelator alarmCorrelator;

    @Inject
    private AlarmValidator alarmValidator;

    /**
     * Method handles alarms with record type ALARM_SUPPRESSED_ALARM, when there is a correlated alarm and when there is no correlated alarm in db.
     * Also updates service state of fmFunctionMO of fdn.
     * @param {@link ProcessedAlarmEvent}--alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Alarm received to SuppressedAlarmHandler : {}", alarmRecord);
        // AlarmSuppressed ServiceState is changed to true in FmFunctionMO for fdn.
        if (!(ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity()))) {
            // Alarm suppressed state is set to true in FmFunctionMO
            serviceStateModifier.updateAlarmSuppressedServiceState(alarmRecord.getFdn(), AlarmProcessorConstants.ALARM_SUPPRESSED_STATE);
        }
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        final ProcessedAlarmEvent correlatedAlarm = alarmCorrelator.getCorrelatedAlarm(alarmRecord);
        if (correlatedAlarm != null && correlatedAlarm.getEventPOId() > 0) {
            if (alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)) {
                alarmProcessingResponse = correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
            }
        } else {
            alarmProcessingResponse = newAlarmProcessor.processAlarm(alarmRecord);
        }
        return alarmProcessingResponse;
    }
}
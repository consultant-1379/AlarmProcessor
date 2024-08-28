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

import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.alarm.staging.AlarmStagingHandler;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class handles alarms with record type ALARM.
 */
public class NormalAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NormalAlarmHandler.class);

    @Inject
    private AlarmProcessor newAlarmProcessor;

    @Inject
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Inject
    private AlarmValidator alarmValidator;

    @Inject
    private AlarmCorrelator alarmCorrelator;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private AlarmStagingHandler alarmStagingHandler;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    /**
     * Method handles alarms with record type ALARM when there is a correlated alarm and when there no correlated alarm in database.
     *
     * @param {@link ProcessedAlarmEvent} --alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Alarm received to NormalAlarmHandler : {}", alarmRecord);
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        final ProcessedAlarmEvent correlatedAlarm = alarmCorrelator.getCorrelatedAlarm(alarmRecord);

        if (correlatedAlarm != null && correlatedAlarm.getEventPOId() > 0) {
            LOGGER.debug("Alarm received to NormalAlarmHandler : {} and Correlated {}", alarmRecord, correlatedAlarm);
            if (configParametersListener.getTransientAlarmStaging() && NOT_SET.equals(alarmRecord.getFmxGenerated())) {
                LOGGER.debug("Staging PIB parameter is enabled! Check and Stage the alarm if needed");
                if (alarmStagingHandler.checkAndStageAlarm(alarmRecord, correlatedAlarm)) {
                    // Transient alarm is staged!
                    // Will return here and the alarm will be re-processed once the stage timer expires.
                    return alarmProcessingResponse;
                }
            }
            if (alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm)) {
                alarmProcessingResponse = correlatedAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm);
            }
        } else {
            if (!FMX_HIDE.equals(alarmRecord.getAdditionalInformation().get(HIDE_OPERATION))) {
                alarmProcessingResponse = newAlarmProcessor.processAlarm(alarmRecord);
            } else {
                LOGGER.warn("Discarding this show/hide request as the corresponding AlarmRecord is not present in the Database anymore , Alarm EventPOId is {}.",
                        alarmRecord.getEventPOId());
                LOGGER.debug("Discarding this show/hide request as the corresponding AlarmRecord is not present in the Database anymore , Alarm {}.",
                        alarmRecord);
                apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
            }
        }
        return alarmProcessingResponse;
    }

}

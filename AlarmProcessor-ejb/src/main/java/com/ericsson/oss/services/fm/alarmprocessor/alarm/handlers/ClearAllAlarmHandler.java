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

import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.AUTOMATIC_SYNCHRONIZATION;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmSupervisionMoReader;
import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class to handle alarm with Record Type CLEARALL. Alarm Synchronization will be invoked for the concerned NetworkElement if automaticSyncronization
 * is turned on. Element.
 */
public class ClearAllAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClearAllAlarmHandler.class);

    @Inject
    private AlarmCorrelator alarmCorrelator;

    @Inject
    private FmSupervisionMoReader fmSupervisionMoReader;

    @Inject
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Inject
    private AlarmProcessor newAlarmProcessor;

    /**
     * Alarm with CLEARALL record type will be inserted in database with severity:MAJOR. Based on auto-sync boolean decision will be taken whether to
     * initiate sync on that node or not. If oscillation correlation is enabled record will replace with correlated record, otherwise it is inserted
     * as a new record. If auto-sync is disabled and correlated alarm is not under clear state, then repeat-count is incremented.
     * @param {@link ProcessedAlarmEvent}--alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        LOGGER.debug("Alarm received to ClearAllAlarmHandler: {}", alarmRecord);
        final ProcessedAlarmEvent correlatedAlarm = alarmCorrelator.getCorrelatedAlarm(alarmRecord);
        // when matching alarm exists in DB
        if (correlatedAlarm != null && correlatedAlarm.getEventPOId() > 0) {
            correlatedAlarmProcessor.processClearAllAlarm(alarmRecord, correlatedAlarm);
        } else {
            newAlarmProcessor.processClearAllAlarm(alarmRecord);
        }
        if (fmSupervisionMoReader.readSupervisionAndAutoSyncAttributes(alarmRecord.getFdn()).get(AUTOMATIC_SYNCHRONIZATION)) {
            LOGGER.debug("Auto sync is enabled for :{}.Alarm sync will be initiated.", alarmRecord.getFdn());
            alarmProcessingResponse.setInitiateAlarmSync(true);
        }
        alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
        return alarmProcessingResponse;
    }
}
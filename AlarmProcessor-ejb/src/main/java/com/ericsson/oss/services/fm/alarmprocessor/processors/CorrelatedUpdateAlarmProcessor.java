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

import javax.inject.Inject;

import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

/**
 * Class for processing alarms when correlated alarm is not cleared.
 */
public class CorrelatedUpdateAlarmProcessor {

    @Inject
    private ClearAlarmProcessor clearAlarmProcessor;

    @Inject
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Inject
    private ReplaceAlarmProcessor replaceAlarmProcessor;

    /**
     * Method processes FMProcessedEventType ALARM when correlated alarm is not cleared.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     * @return {@link AlarmProcessingResponse}
     */
    public AlarmProcessingResponse processNormalAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        updateCorrelatedAlarmAttrValues(alarmRecord, correlatedAlarm);
        if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())) {
            final ProcessedAlarmEvent clearAlarmRecord = clearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
            alarmProcessingResponse.getProcessedAlarms().add(clearAlarmRecord);
        } else {
            replaceAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm);
            // If there is repeated alarm received matching to FMX rule (NORMAL PROC), and the alarm exists DB with
            // visibility true(it means that its forwarded to UI/NBI)
            // Then Send fake clear to UI/NBI as repeated alarm is hidden and original alarm is still present in UI/NBI.
            // This will make sure that alarm is cleared from all the clients when its hidden by FMX operator in ENM.
            sendFakeClearToUiAndNbi(alarmRecord,correlatedAlarm,alarmProcessingResponse);
            alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
            clearAlarmsCacheManager.checkAndProcessForClearAlarm(alarmRecord, alarmProcessingResponse);
        }
        return alarmProcessingResponse;
    }

    /**
     * Method that processes alarms with record types REPEATED_ALARM and REPEATED_NON_SYNCHABLE when correlated alarm is not cleared.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     * @return {@link AlarmProcessingResponse}
     */
    public AlarmProcessingResponse processRepeatedAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        updateCorrelatedAlarmAttrValues(alarmRecord, correlatedAlarm);
        if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())) {
            final ProcessedAlarmEvent clearAlarmRecord = clearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
            alarmProcessingResponse.getProcessedAlarms().add(clearAlarmRecord);
            clearAlarmsCacheManager.checkAndProcessForClearAlarm(clearAlarmRecord, alarmProcessingResponse);
        } else {
            replaceAlarmProcessor.processRepeatedAlarm(alarmRecord, correlatedAlarm);
            // If there is repeated alarm received matching to FMX rule (NORMAL PROC), and the alarm exists DB with
            // visibility true(it means that its forwarded to UI/NBI)
            // Then Send fake clear to UI/NBI as repeated alarm is hidden and original alarm is still present in UI/NBI.
            // This will make sure that alarm is cleared from all the clients when its hidden by FMX operator in ENM.
            sendFakeClearToUiAndNbi(alarmRecord,correlatedAlarm,alarmProcessingResponse);
            alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
            clearAlarmsCacheManager.checkAndProcessForClearAlarm(alarmRecord, alarmProcessingResponse);
        }
        return alarmProcessingResponse;
    }

    /**
     * Method processes alarms with record types TECHNICIAN_PRESENT, ALARM_SUPPRESSED_ALARM, HEARTBEAT_ALARM, NON_SYNCHABLE_ALARM,
     * HB_FAILURE_NO_SYNCH,OUT_OF_SYNC and NODE_SUSPENDED when correlated alarm is not cleared.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     * @return {@link AlarmProcessingResponse}
     */
    public AlarmProcessingResponse processAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        updateCorrelatedAlarmAttrValues(alarmRecord, correlatedAlarm);
        if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())) {
            final ProcessedAlarmEvent clearAlarmRecord = clearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
            alarmProcessingResponse.getProcessedAlarms().add(clearAlarmRecord);
            clearAlarmsCacheManager.checkAndProcessForClearAlarm(clearAlarmRecord, alarmProcessingResponse);
        } else {
            replaceAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm);
            // If there is repeated alarm received matching to FMX rule (NORMAL PROC), and the alarm exists DB with
            // visibility true(it means that its forwarded to UI/NBI)
            // Then Send fake clear to UI/NBI as repeated alarm is hidden and original alarm is still present in UI/NBI.
            // This will make sure that alarm is cleared from all the clients when its hidden by FMX operator in ENM.
            sendFakeClearToUiAndNbi(alarmRecord,correlatedAlarm,alarmProcessingResponse);
            alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
            clearAlarmsCacheManager.checkAndProcessForClearAlarm(alarmRecord, null);
        }
        return alarmProcessingResponse;
    }

    /**
     * Method updates ProcessedAlarmEvent with correlatedAlarm's EventPOId, PresentSeverity, RepeatCount, OscillationCount.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     */
    private void updateCorrelatedAlarmAttrValues(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        alarmRecord.setCorrelatedPOId(correlatedAlarm.getEventPOId());
        alarmRecord.setPreviousSeverity(correlatedAlarm.getPresentSeverity());
        alarmRecord.setRepeatCount(correlatedAlarm.getRepeatCount());
        alarmRecord.setOscillationCount(correlatedAlarm.getOscillationCount());
        alarmRecord.setEventPOId(alarmRecord.getCorrelatedPOId());
    }

    public void sendFakeClearToUiAndNbi(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm , final AlarmProcessingResponse alarmProcessingResponse ) {
        if (correlatedAlarm.getVisibility() && !alarmRecord.getVisibility()) {
            alarmProcessingResponse.setSendFakeClearToUiAndNbi(true);
        }
    }
}
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

package com.ericsson.oss.services.fm.alarmprocessor.builders;

import com.ericsson.oss.services.fm.models.processedevent.ATRInputEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class for building {@link ATRInputEvent} from {@link ProcessedAlarmEvent}.
 */
public class AlarmTextRouteInputEventBuilder {

    /**
     * Method prepares {@link ATRInputEvent} from {@link ProcessedAlarmEvent} to be sent to ATRProcessedEventChannel.
     *
     * @param {@link ProcessedAlarmEvent} event FM processed alarm event received
     * @return {@link ATRInputEvent}
     */
    public ATRInputEvent build(final ProcessedAlarmEvent alarmRecord) {
        final ATRInputEvent atrInputEvent = new ATRInputEvent();
        atrInputEvent.setAckOperator(alarmRecord.getAckOperator());
        atrInputEvent.setAckTime(alarmRecord.getAckTime());
        atrInputEvent.setActionState(alarmRecord.getActionState());
        atrInputEvent.setAdditionalInformation(alarmRecord.getAdditionalInformation());
        atrInputEvent.setAlarmId(alarmRecord.getAlarmId());
        atrInputEvent.setAlarmNumber(alarmRecord.getAlarmNumber());
        atrInputEvent.setAlarmState(alarmRecord.getAlarmState());
        atrInputEvent.setAlarmingObject(alarmRecord.getAlarmingObject());
        atrInputEvent.setBackupObjectInstance(alarmRecord.getBackupObjectInstance());
        atrInputEvent.setBackupStatus(alarmRecord.getBackupStatus());
        atrInputEvent.setCeaseOperator(alarmRecord.getCeaseOperator());
        atrInputEvent.setCeaseTime(alarmRecord.getCeaseTime());
        atrInputEvent.setCommentText(alarmRecord.getCommentText());
        atrInputEvent.setCorrelatedPOId(alarmRecord.getCorrelatedPOId());
        atrInputEvent.setCorrelatedVisibility(alarmRecord.getCorrelatedVisibility());
        atrInputEvent.setEventPOId(alarmRecord.getEventPOId());
        atrInputEvent.setEventTime(alarmRecord.getEventTime());
        atrInputEvent.setEventType(alarmRecord.getEventType());
        atrInputEvent.setFdn(alarmRecord.getFdn());
        atrInputEvent.setFmxGenerated(alarmRecord.getFmxGenerated());
        atrInputEvent.setHistoryPOId(alarmRecord.getHistoryPOId());
        atrInputEvent.setInsertTime(alarmRecord.getInsertTime());
        atrInputEvent.setLastAlarmOperation(alarmRecord.getLastAlarmOperation());
        atrInputEvent.setLastUpdatedTime(alarmRecord.getLastUpdatedTime());
        atrInputEvent.setManagedObject(alarmRecord.getManagedObject());
        atrInputEvent.setObjectOfReference(alarmRecord.getObjectOfReference());
        atrInputEvent.setOscillationCount(alarmRecord.getOscillationCount());
        atrInputEvent.setPresentSeverity(alarmRecord.getPresentSeverity());
        atrInputEvent.setPreviousSeverity(alarmRecord.getPreviousSeverity());
        atrInputEvent.setProbableCause(alarmRecord.getProbableCause());
        atrInputEvent.setProblemDetail(alarmRecord.getProblemDetail());
        atrInputEvent.setProblemText(alarmRecord.getProblemText());
        atrInputEvent.setProcessingType(alarmRecord.getProcessingType());
        atrInputEvent.setProposedRepairAction(alarmRecord.getProposedRepairAction());
        atrInputEvent.setRecordType(alarmRecord.getRecordType());
        atrInputEvent.setRepeatCount(alarmRecord.getRepeatCount());
        atrInputEvent.setSpecificProblem(alarmRecord.getSpecificProblem());
        atrInputEvent.setSyncState(alarmRecord.getSyncState());
        atrInputEvent.setTrendIndication(alarmRecord.getTrendIndication());
        atrInputEvent.setTimeZone(alarmRecord.getTimeZone());
        atrInputEvent.setVisibility(alarmRecord.isVisibility());
        return atrInputEvent;
    }
}
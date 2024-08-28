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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARMSUPPRESSED_SP;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.TECHNICIANPRESENT_SP;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.populateHiddenAlarm;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.INSERT_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.common.builder.ProcessedAlarmEventBuilder;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;
import com.ericsson.oss.services.models.alarm.AlarmRecordType;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmsCountOnNodesMapManager;

/**
 * Class processes alarms with severity CLEAR.
 */
public class ClearAlarmProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClearAlarmProcessor.class);

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Inject
    AlarmsCountOnNodesMapManager alarmsCountOnNodesMapManager;

    /**
     * Method processes clear alarm. Changes FmFunctionMo ALARM_SUPPRESSED_ALARM/TECHNICIAN_PRESENT values to false. Clear on hide - alarms are
     * removed from db else updates alarm in db.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     */

    public ProcessedAlarmEvent processAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        // To indicate a clear Alarm
        alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.CLEAR);
        alarmRecord.setCeaseTime(alarmRecord.getEventTime());
        // since it is clear alarm, need to update fmFunctionMO to false for the below attr
        if (AlarmRecordType.ALARM_SUPPRESSED_ALARM.name().equalsIgnoreCase(correlatedAlarm.getRecordType().name())
                || ALARMSUPPRESSED_SP.equals(correlatedAlarm.getSpecificProblem())) {
            fmFunctionMoService.update(correlatedAlarm.getFdn(), AlarmProcessorConstants.ALARM_SUPPRESSED_STATE, false);
        } else if (AlarmRecordType.TECHNICIAN_PRESENT.name().equalsIgnoreCase(correlatedAlarm.getRecordType().name())
                || TECHNICIANPRESENT_SP.equals(correlatedAlarm.getSpecificProblem())) {
            fmFunctionMoService.update(correlatedAlarm.getFdn(), AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE, false);
        }
        if (!correlatedAlarm.getVisibility()) {
            return updateAndDeleteHiddenClearAlarm(alarmRecord, correlatedAlarm);
        } else {
            return updateAlarmForClear(alarmRecord, correlatedAlarm);
        }
    }

    public ProcessedAlarmEvent updateAndDeleteHiddenClearAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        final List<String> outputAttributes = new ArrayList<>();
        outputAttributes.add(ALARM_STATE);
        outputAttributes.add(PRESENT_SEVERITY);
        outputAttributes.add(INSERT_TIME);
        outputAttributes.add(ACK_OPERATOR);
        outputAttributes.add(ACK_TIME);
        final Map<String, Object> attributesFromDataBase = alarmReader.readAttributes(alarmRecord.getCorrelatedPOId(), outputAttributes);
        // Clear is received from the Node on an FMX Hidden Alarm. So deleting it from OpenAlarm DB.
        final Map<String, Object> hiddenAlarmAttributes = populateHiddenAlarm(alarmRecord, attributesFromDataBase);
        alarmAttributesPopulator.updateLastDeliveredTime(alarmRecord, correlatedAlarm, hiddenAlarmAttributes);
        openAlarmService.removeAlarm(alarmRecord.getCorrelatedPOId(), hiddenAlarmAttributes);
        LOGGER.debug("Hidden Alarm : {} removed from Database.", alarmRecord);
        apsInstrumentedBean.incrementDeletedShortLivedAlarmsCount();
        apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(alarmRecord.getPresentSeverity());
        return alarmRecord;
    }

    /**
     * Method updates an already existing Alarm PersistenceObject when a clear is received.
     * @param {@link ProcessedAlarmEvent} clearAlarm
     * @param {@link ProcessedAlarmEvent} alarm
     */
    private ProcessedAlarmEvent updateAlarmForClear(final ProcessedAlarmEvent clearAlarm, final ProcessedAlarmEvent correlatedAlarm) {
        LOGGER.debug("Correlated poid is :{} ", correlatedAlarm);
        final String originalProcessingType = clearAlarm.getProcessingType();
        ProcessedAlarmEvent updatedClearAlarm = clearAlarm;
        final List<String> originalDiscriminatorList = clearAlarm.getDiscriminatorList();
        if (correlatedAlarm.getAlarmState().equals(ProcessedEventState.ACTIVE_ACKNOWLEDGED)) {
            clearAlarm.setAlarmState(ProcessedEventState.CLEARED_ACKNOWLEDGED);
            clearAlarm.setAckOperator(correlatedAlarm.getAckOperator());
            clearAlarm.setAckTime(correlatedAlarm.getAckTime());
            clearAlarm.setCorrelatedPOId(correlatedAlarm.getEventPOId());
            final Map<String, Object> alarmAttributes = alarmAttributesPopulator.populateDeleteAlarm(clearAlarm, correlatedAlarm);
            updatedClearAlarm = buildAndPopulateClearAlarm(correlatedAlarm, alarmAttributes);
            openAlarmService.removeAlarm(clearAlarm.getCorrelatedPOId(), alarmAttributes);
            LOGGER.debug("Alarm with this PO Id: {} removed from the list.", clearAlarm.getCorrelatedPOId());
        } else if (correlatedAlarm.getAlarmState().equals(ProcessedEventState.ACTIVE_UNACKNOWLEDGED)) {
            final Map<String, Object> clearAlarmAttributes = alarmAttributesPopulator.populateClearAlarm(clearAlarm);
            updatedClearAlarm = buildAndPopulateClearAlarm(correlatedAlarm, clearAlarmAttributes);
            openAlarmService.updateAlarm(clearAlarm.getCorrelatedPOId(), clearAlarmAttributes);
            final Map<String, String> additionalInformationOfAlarms = clearAlarm.getAdditionalInformation();
            if ((clearAlarm.getFdn() != null) && (additionalInformationOfAlarms.get("sourceType") != null)) {
                final String nodeName = (clearAlarm.getFdn()).split("=")[1];
                final String nodeType = additionalInformationOfAlarms.get("sourceType");
                alarmsCountOnNodesMapManager.incrementAlarmsCountRequest(nodeName, nodeType);
            }
            LOGGER.debug("Alarm with this PO Id: {} received Clear.", clearAlarm.getCorrelatedPOId());
        }
        apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(clearAlarm.getPresentSeverity());
        updatedClearAlarm.setRecordType(correlatedAlarm.getRecordType());
        updatedClearAlarm.setProcessingType(originalProcessingType);
        updatedClearAlarm.setDiscriminatorList(originalDiscriminatorList);
        return updatedClearAlarm;
    }

    private ProcessedAlarmEvent buildAndPopulateClearAlarm(final ProcessedAlarmEvent correlatedAlarm, final Map<String, Object> alarmAttributes) {
        final ProcessedAlarmEvent clearAlarm = ProcessedAlarmEventBuilder.buildProcessedAlarm(alarmAttributes);
        clearAlarm.setEventPOId(clearAlarm.getCorrelatedPOId());
        alarmAttributesPopulator.updateLastDeliveredTime(clearAlarm, correlatedAlarm, alarmAttributes);
        return clearAlarm;
    }
}

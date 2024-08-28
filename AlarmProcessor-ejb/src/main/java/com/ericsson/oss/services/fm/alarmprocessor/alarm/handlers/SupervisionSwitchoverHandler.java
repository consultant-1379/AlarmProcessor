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

import static com.ericsson.oss.mediation.translator.model.Constants.SEV_CRITICAL;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.APS_SERVICE_ID;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.LAST_DELIVERED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATED_EVENT_PO_ID;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.EVENT_PO_ID;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_ALARM_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_UPDATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_DETAIL;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PSEUDO_PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PSEUDO_PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.UNDER_SCORE_DELIMITER;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.alarmprocessor.util.CurrentServiceStateUpdator;
import com.ericsson.oss.services.fm.common.builder.ProcessedAlarmEventBuilder;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;
import com.ericsson.oss.services.fm.util.PseudoSeverities;

/**
 * The class is responsible to handle SUPERVISION_SWITCHOVER notification coming from mediation during fail over. If the node has any
 * HEARTBEAT/NODE_SUSPENDED/OUT_OF_SYNC alarm after receiving SUPERVISION_SWITCHOVER notification in APS, then the
 * HEARTBEAT/NODE_SUSPENDED/OUT_OF_SYNC alarm present is cleared, and currentServiceState is set to IN_SERVICE.
 */
public class SupervisionSwitchoverHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionSwitchoverHandler.class);

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private CurrentServiceStateUpdator currentServiceStateUpdater;

    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("Alarm received to SupervisionSwitchoverHandler : {}", alarmRecord);
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();

        final List<String> recordTypeValues = new ArrayList<>();
        recordTypeValues.add(FMProcessedEventType.NODE_SUSPENDED.toString());
        recordTypeValues.add(FMProcessedEventType.HEARTBEAT_ALARM.toString());
        recordTypeValues.add(FMProcessedEventType.OUT_OF_SYNC.toString());

        final Map<String, Object> singleValuedAttributes = new HashMap<>();
        singleValuedAttributes.put(FDN, alarmRecord.getFdn());
        singleValuedAttributes.put(PRESENT_SEVERITY, SEV_CRITICAL);

        final Map<String, List<String>> multiValuedAttributes = new HashMap<>();
        multiValuedAttributes.put(RECORD_TYPE, recordTypeValues);

        final Iterator<PersistenceObject> poIterator = openAlarmService.getOpenAlarmPO(singleValuedAttributes, multiValuedAttributes);
        LOGGER.info("Heartbeat Clear Notification received to SupervisionSwitchoverHandler : {}",alarmRecord);

        // CeaseTime of HB alarm when the Supervision SwitchOver happened. Assigning this ceaseTime in Mediation itself
        // while generating fmEvent for Supervision SwitchOver and only for CPP nodes, so as to clear HB first and raise 
        // new HB alarm after switch over as part of TORF-704795

        String ceaseTimeOfHB = alarmRecord.getAdditionalInformation().get(CEASE_TIME);
        LOGGER.info("ceaseTimeOfHB received from SupervisionSwitchover is {} ", ceaseTimeOfHB);

        while (poIterator.hasNext()) {
            final PersistenceObject openAlarm = poIterator.next();
            final Map<String, Object> allOpenAlarmAttributes = openAlarm.getAllAttributes();
            final Object eventPoId = openAlarm.getPoId();
            ProcessedAlarmEvent clearAlarm = new ProcessedAlarmEvent();

            if (!(boolean) allOpenAlarmAttributes.get(VISIBILITY)) {
                // Clear has to be performed on a FMX Hidden Alarm. So deleting it from OpenAlarm DB.
                // Setting the AlarmState to CLEARED_ACKNOWLEDGED as the alarm is being deleted from DB, so should be at NMS.
                allOpenAlarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_ACKNOWLEDGED.name());
                allOpenAlarmAttributes.put(PROBLEM_DETAIL, AlarmProcessorConstants.DELETE_ALARM_PROBLEM_DETAIL);
                setCommonAttributes(allOpenAlarmAttributes, ceaseTimeOfHB);
                if(alarmRecord.getSyncState() == true) {
                    openAlarmService.updateAlarm((long) eventPoId, allOpenAlarmAttributes);
                }
                else {
                     openAlarmService.removeAlarm((long) eventPoId, allOpenAlarmAttributes);
                     allOpenAlarmAttributes.put(CORRELATED_EVENT_PO_ID, eventPoId);
                     allOpenAlarmAttributes.put(EVENT_PO_ID, eventPoId);
                     clearAlarm = ProcessedAlarmEventBuilder.buildProcessedAlarm(allOpenAlarmAttributes);
                     LOGGER.debug("Hidden Alarm : {} removed from DB.", clearAlarm);
                }
            } else {
                if (ProcessedEventState.ACTIVE_ACKNOWLEDGED.name().equals(allOpenAlarmAttributes.get(ALARM_STATE))) {
                    // Setting the AlarmState to CLEARED_ACKNOWLEDGED as the alarm is being deleted from DB, so should be at NMS.
                    allOpenAlarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_ACKNOWLEDGED.name());
                    setCommonAttributes(allOpenAlarmAttributes, ceaseTimeOfHB);
                    openAlarmService.removeAlarm((long) eventPoId, allOpenAlarmAttributes);
                    allOpenAlarmAttributes.put(CORRELATED_EVENT_PO_ID, eventPoId);
                    allOpenAlarmAttributes.put(EVENT_PO_ID, eventPoId);
                    clearAlarm = ProcessedAlarmEventBuilder.buildProcessedAlarm(allOpenAlarmAttributes);
                    LOGGER.debug("{} alarm : {} removed from DB as it was acknowledged already..", clearAlarm.getRecordType(), clearAlarm);
                } else if (ProcessedEventState.ACTIVE_UNACKNOWLEDGED.name().equals(allOpenAlarmAttributes.get(ALARM_STATE))) {
                    LOGGER.info("HB Alarm is in active_unack");
                    allOpenAlarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_UNACKNOWLEDGED.name());
                    setCommonAttributes(allOpenAlarmAttributes, ceaseTimeOfHB);
                    openAlarmService.updateAlarm((long) eventPoId, allOpenAlarmAttributes);
                    allOpenAlarmAttributes.put(CORRELATED_EVENT_PO_ID, eventPoId);
                    allOpenAlarmAttributes.put(EVENT_PO_ID, eventPoId);
                    clearAlarm = ProcessedAlarmEventBuilder.buildProcessedAlarm(allOpenAlarmAttributes);
                }
            }
            alarmProcessingResponse.getProcessedAlarms().add(clearAlarm);
            clearAlarmsCacheManager.checkAndProcessForClearAlarm(clearAlarm, alarmProcessingResponse);

            apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(ProcessedEventSeverity.CLEARED);
            if (null != clearAlarm.getFdn()) {
                currentServiceStateUpdater.updateForSupervisionSwitchOver(clearAlarm.getFdn());
            }
        }
        return alarmProcessingResponse;
    }

    private void setCommonAttributes(final Map<String, Object> allOpenAlarmAttributes, String ceaseTimeOfHB) {
        allOpenAlarmAttributes.put(PREVIOUS_SEVERITY, allOpenAlarmAttributes.get(PRESENT_SEVERITY));
        allOpenAlarmAttributes.put(PRESENT_SEVERITY, ProcessedEventSeverity.CLEARED.name());
        long currentTime;
        if (ceaseTimeOfHB == null) {
            currentTime = System.currentTimeMillis();
        } else {
            currentTime = Long.parseLong(ceaseTimeOfHB);
        }
        allOpenAlarmAttributes.put(CEASE_TIME, new Date(currentTime));
        allOpenAlarmAttributes.put(LAST_UPDATED, new Date(currentTime));
        allOpenAlarmAttributes.put(LAST_ALARM_OPERATION, ProcessedLastAlarmOperation.CLEAR.name());
        allOpenAlarmAttributes.put(CEASE_OPERATOR, APS_SERVICE_ID);
        final String pseudoPresentSeverity = allOpenAlarmAttributes.get(PREVIOUS_SEVERITY) + UNDER_SCORE_DELIMITER
                + ProcessedEventSeverity.CLEARED.name();
        allOpenAlarmAttributes.put(PSEUDO_PRESENT_SEVERITY, PseudoSeverities.PSEUDO_SEVERITIES.get(pseudoPresentSeverity));
        allOpenAlarmAttributes.put(PSEUDO_PREVIOUS_SEVERITY,
                PseudoSeverities.PSEUDO_SEVERITIES.get(allOpenAlarmAttributes.get(PREVIOUS_SEVERITY)));

        Long diffTimeInMilliSeconds = currentTime;
        if (allOpenAlarmAttributes.get(LAST_DELIVERED) != null) {
            diffTimeInMilliSeconds = Math.abs(currentTime - (long) allOpenAlarmAttributes.get(LAST_DELIVERED));
        }
        long lastDeliveredTime;
        if (diffTimeInMilliSeconds < configParametersListener.getAlarmThresholdInterval()) {
            lastDeliveredTime = currentTime + configParametersListener.getClearAlarmDelayToQueue();
        } else {
            lastDeliveredTime = currentTime;
        }
        allOpenAlarmAttributes.put(LAST_DELIVERED, lastDeliveredTime);
    }
}

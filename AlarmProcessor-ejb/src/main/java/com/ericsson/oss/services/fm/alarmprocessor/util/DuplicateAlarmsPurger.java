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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATED_EVENT_PO_ID;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_ALARM_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_UPDATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBLEM_DETAIL;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PSEUDO_PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PSEUDO_PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_RECEIVED_TIME;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.common.builder.ProcessedAlarmEventBuilder;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;
import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmHandlerBean;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ProcessedAlarmSender;

/**
 * The class is responsible to purge the duplicate alarms present in the database with same correlation ID.
 */
public class DuplicateAlarmsPurger {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateAlarmsPurger.class);

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Inject
    private ProcessedAlarmSender processedAlarmSender;

    @Inject
    private AlarmHandlerBean alarmHandlerBean;

    /**
     * The method deletes all the duplicate alarms from the DB. Changes added as part of TORF-219031. Deleted duplicate clear alarms are
     * forwarded to all the APS subscribers.
     *
     * @param correlatedAlarms
     *            List of alarms to be removed from DB.
     */
    public void removeDuplicateAlarmsFromDatabase(final List<ProcessedAlarmEvent> correlatedAlarms) {
        LOGGER.debug("Event received to DuplicateAlarmsPurger.");
        for (final ProcessedAlarmEvent duplicateCorrelatedAlarm : correlatedAlarms) {
            if (!duplicateCorrelatedAlarm.getPresentSeverity().equals(ProcessedEventSeverity.CLEARED)) {
                final Long eventPoId = duplicateCorrelatedAlarm.getEventPOId();
                final Map<String, Object> alarmAttributes = alarmReader.readAllAttributes(eventPoId);

                alarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_ACKNOWLEDGED.name());
                alarmAttributes.put(ACK_OPERATOR, AlarmProcessorConstants.APS_SERVICE_ID);
                alarmAttributes.put(ACK_TIME, new Date());
                alarmAttributes.put(CEASE_TIME, new Date());
                alarmAttributes.put(CEASE_OPERATOR, AlarmProcessorConstants.APS_SERVICE_ID);
                alarmAttributes.put(CORRELATED_EVENT_PO_ID, eventPoId);
                alarmAttributes.put(PROBLEM_DETAIL, AlarmProcessorConstants.DELETE_DUPLICATE_ALARM_PROBLEM_DETAIL);
                alarmAttributes.put(PREVIOUS_SEVERITY, duplicateCorrelatedAlarm.getPresentSeverity().name());
                alarmAttributes.put(PRESENT_SEVERITY, ProcessedEventSeverity.CLEARED.name());
                alarmAttributes.put(LAST_UPDATED, new Date());
                alarmAttributes.put(LAST_ALARM_OPERATION, ProcessedLastAlarmOperation.CLEAR.name());

                final ProcessedAlarmEvent alarmToDelete = ProcessedAlarmEventBuilder.buildProcessedAlarm(alarmAttributes);

                alarmToDelete.setEventPOId(eventPoId);
                alarmAttributes.put(PSEUDO_PRESENT_SEVERITY, alarmToDelete.getPseudoPresentSeverity());
                alarmAttributes.put(PSEUDO_PREVIOUS_SEVERITY, alarmToDelete.getPseudoPreviousSeverity());

                alarmAttributesPopulator.updateLastDeliveredTime(alarmToDelete, alarmToDelete, alarmAttributes);

                openAlarmService.removeAlarm(alarmToDelete.getEventPOId(), alarmAttributes);
                LOGGER.warn("Alarm with PO Id: {} is removed from DB as its a duplicate alarm.", alarmToDelete.getCorrelatedPOId());

                String alarmReceivedTime = null;
                if (alarmToDelete.getAdditionalInformation().containsKey(ALARM_RECEIVED_TIME)) {
                    alarmReceivedTime = alarmToDelete.getAdditionalInformation().get(ALARM_RECEIVED_TIME);
                }

                processedAlarmSender.sendDuplicateAlarms(alarmToDelete, alarmReceivedTime, false, false);
                LOGGER.warn("duplicate alarm: {} is cleared and sent to NBI.", alarmToDelete.getCorrelatedPOId());
            }
        }
    }
}

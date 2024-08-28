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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.LAST_DELIVERED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.NUMBER_OF_CORRELATED_ALARMS_TO_PROCESS;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ORIGINAL_EVENTTIME_FROM_NODE;

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
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.common.builder.ProcessedAlarmEventBuilder;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class for correlating alarms and error events.
 */
public class AlarmCorrelator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmCorrelator.class);

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private FmxAttributesWriter fmxAttributesSetter;

    @Inject
    private DuplicateAlarmsPurger duplicateAlarmsPurger;

    /**
     * Method to get Correlated Alarm. It calls {@link #getCorrelatedAlarmFromDataBase(ProcessedAlarmEvent)} with ProcessedAlarmEvent object with
     * fields set used for Correlation.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @return {@link ProcessedAlarmEvent} correlatedAlarm
     */
    public ProcessedAlarmEvent getCorrelatedAlarm(final ProcessedAlarmEvent alarmRecord) {
        final ProcessedAlarmEvent correlatedKey = buildCorrelatedKey(alarmRecord);
        final ProcessedAlarmEvent correlatedAlarm = getCorrelatedAlarmFromDataBase(correlatedKey);
        fmxAttributesSetter.updateVisibilityForNodeClearAlarm(alarmRecord, correlatedAlarm);
        return correlatedAlarm;
    }

    /**
     * Returns true if the alarm is correlated based on AlarmNumber,SpecificProblem,ProbableCause,EventType and Event time is latest than the Event
     * time of alarm present in Cache.
     * @param {@link ProcessedAlarmEvent} alarm
     * @param {@link ProcessedAlarmEvent} originalAlarm
     * @return true true if correlated or else false
     */
    public boolean correlateAlarm(final ProcessedAlarmEvent clearAlarm, final ProcessedAlarmEvent originalAlarm) {
        boolean isCorrelated = false;
        Date clearAlarmEventTime = clearAlarm.getEventTime();
        Date originalAlarmEventTime = originalAlarm.getEventTime();
        final String clearAlarmOriginalEventTime = clearAlarm.getAdditionalInformation().get(ORIGINAL_EVENTTIME_FROM_NODE);
        final String originalAlarmOriginalEventTime = originalAlarm.getAdditionalInformation().get(ORIGINAL_EVENTTIME_FROM_NODE);
        if (clearAlarmOriginalEventTime != null && originalAlarmOriginalEventTime != null) {
            clearAlarmEventTime = new Date(Long.parseLong(clearAlarmOriginalEventTime));
            originalAlarmEventTime = new Date(Long.parseLong(originalAlarmOriginalEventTime));
        }
        if (clearAlarm.getAlarmNumber() > 0) {
            if (clearAlarm.getAlarmNumber().equals(originalAlarm.getAlarmNumber()) && !clearAlarmEventTime.before(originalAlarmEventTime)) {
                isCorrelated = true;
            }
        } else {
            if (clearAlarm.getSpecificProblem().equalsIgnoreCase(originalAlarm.getSpecificProblem())
                    && clearAlarm.getProbableCause().equalsIgnoreCase(originalAlarm.getProbableCause())
                    && clearAlarm.getEventType().equalsIgnoreCase(originalAlarm.getEventType())
                    && !clearAlarmEventTime.before(originalAlarmEventTime)) {
                isCorrelated = true;
            }
        }
        LOGGER.debug("Result of alarmCorrelation in ClearAlarmsCache is {}", isCorrelated);

        return isCorrelated;
    }

    /**
     * Method that returns correlated Alarm from database. Alarm Correlation is based on ObjectOfReference,AlarmNumber if AlarmNumber is present in
     * the alarm . In case of absence of alarm Number in the alarm , ObjectOfReference,SpecificProblem,ProbableCause and EventType attributes are used
     * for alarm Correlation.
     * @param {@link ProcessedAlarmEvent} alarm for which Correlation should be performed.
     * @return {@link ProcessedAlarmEvent} correlatedAlarm
     */
    private ProcessedAlarmEvent getCorrelatedAlarmFromDataBase(final ProcessedAlarmEvent alarm) {
        ProcessedAlarmEvent correlatedAlarm = new ProcessedAlarmEvent();
        Iterator<PersistenceObject> correlatedAlarmIterator = null;
        if (alarm.getAlarmNumber() > 0) {
            correlatedAlarmIterator = alarmReader.correlateWithAlarmNumber(alarm);
        } else {
            correlatedAlarmIterator = alarmReader.correlateWithSpPcEt(alarm);
        }
        int counter = 0;
        final List<ProcessedAlarmEvent> correlatedAlarms = new ArrayList<ProcessedAlarmEvent>();
        if (correlatedAlarmIterator != null) {
            // The check to verify number of alarms is to avoid iterating over all the correlated alarms(including cleared) in DB which
            // would cause acquiring the locks.
            while (counter <= NUMBER_OF_CORRELATED_ALARMS_TO_PROCESS && correlatedAlarmIterator.hasNext()) {
                final PersistenceObject po = correlatedAlarmIterator.next();
                Map<String, Object> poAttributes = new HashMap<String, Object>();
                poAttributes = po.getAllAttributes();
                correlatedAlarm = ProcessedAlarmEventBuilder.buildProcessedAlarm(poAttributes);
                correlatedAlarm.setEventPOId(po.getPoId());
                final Long lastDeliveredTime = (Long) poAttributes.get(LAST_DELIVERED);
                correlatedAlarm.getAdditionalInformation().put(LAST_DELIVERED, String.valueOf(lastDeliveredTime));
                correlatedAlarms.add(correlatedAlarm);
                LOGGER.debug("CorrelatedAlarm Found {}", correlatedAlarm);
                counter++;
            }
            if (!correlatedAlarms.isEmpty()) {
                correlatedAlarm = correlatedAlarms.remove(0);
                if (!correlatedAlarms.isEmpty()) {
                    // Check if duplicate records are there and clear all except one.
                    duplicateAlarmsPurger.removeDuplicateAlarmsFromDatabase(correlatedAlarms);
                }
            }
        }
        return correlatedAlarm;
    }

    /**
     * Method that builds {@link ProcessedAlarmEvent} object with fields set used for Correlation.
     * @param {@link ProcessedAlarmEvent} alarm
     * @return {@link ProcessedAlarmEvent}
     */
    private ProcessedAlarmEvent buildCorrelatedKey(final ProcessedAlarmEvent alarm) {
        final ProcessedAlarmEvent correlatedKey = new ProcessedAlarmEvent();
        correlatedKey.setObjectOfReference(alarm.getObjectOfReference());
        correlatedKey.setSpecificProblem(alarm.getSpecificProblem());
        correlatedKey.setProbableCause(alarm.getProbableCause());
        correlatedKey.setEventType(alarm.getEventType());
        correlatedKey.setRecordType(alarm.getRecordType());
        correlatedKey.setAlarmNumber(alarm.getAlarmNumber());
        return correlatedKey;
    }
}
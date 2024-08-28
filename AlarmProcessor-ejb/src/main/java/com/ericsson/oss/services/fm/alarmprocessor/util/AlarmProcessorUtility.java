/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CLEARALARMSCACHE_KEY_DELIMITER;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.COMMA_DELIMITER;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.EMPTY_STRING;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.EQUAL_TO_DELIMITER;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FMFUNCTION_SUFFIX;

import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventTrendIndication;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.NodesSearchCriteriaData;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.NodesSearchCriteriaData.MatchCondition;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.NodesSearchCriteriaData.SearchScopeType;

/**
 * Util class for alarm processor.
 */
public final class AlarmProcessorUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmProcessorUtility.class);

    private AlarmProcessorUtility() {
    }

    /**
     * Method to retrieve manageObject subtoken from a given String.
     */
    public static String getManagedObject(final String managedObjectReference) {
        String result = EMPTY_STRING;
        try {
            final int lastIndex = managedObjectReference.lastIndexOf(COMMA_DELIMITER) + 1;
            final String lastAttribute = managedObjectReference.substring(lastIndex, managedObjectReference.length());
            result = lastAttribute.split(EQUAL_TO_DELIMITER)[0];
        } catch (final Exception exception) {
            LOGGER.warn("Exception in getManagedObject for {} is  {}", managedObjectReference, exception.getMessage());
            LOGGER.debug("Exception in getManagedObject for {} is  {}", managedObjectReference, exception);
        }
        return result;
    }

    /**
     * Method creates node search criteria.
     *
     * @param {@link String} fdn
     * @return NodesSearchCriteriaData
     */
    public static NodesSearchCriteriaData createNodeSearchCriteria(final String fdn) {
        final NodesSearchCriteriaData nodesSearchCriteriaData = new NodesSearchCriteriaData();
        nodesSearchCriteriaData.setNodeFdn(fdn.concat(FMFUNCTION_SUFFIX));
        nodesSearchCriteriaData.setMatchCondition(MatchCondition.EQUALS);
        nodesSearchCriteriaData.setSearchScopeType(SearchScopeType.FDN);
        return nodesSearchCriteriaData;
    }

    public static String getAlarmingObject(final String managedObjectReference) {
        final String[] oorSplit = managedObjectReference.split(COMMA_DELIMITER);
        return oorSplit[oorSplit.length - 1];
    }

    /**
     * Method evaluates trend indication based on new severity and existing severity for the alarm.
     *
     * @param ProcessedEventSeverity
     *            newSeverity
     * @param ProcessedEventSeverity
     *            oldSeverity
     * @return ProcessedEventTrendIndication
     */
    public static ProcessedEventTrendIndication evaluateTrendIndication(final ProcessedEventSeverity newSeverity,
                                                                        final ProcessedEventSeverity oldSeverity) {
        int newSeverityValue;
        int oldSeverityValue;
        ProcessedEventTrendIndication trendIndication = ProcessedEventTrendIndication.UNDEFINED;
        if (!(ProcessedEventSeverity.UNDEFINED.equals(newSeverity) || ProcessedEventSeverity.UNDEFINED.equals(oldSeverity)
                || ProcessedEventSeverity.INDETERMINATE.equals(newSeverity) || ProcessedEventSeverity.INDETERMINATE.equals(oldSeverity))) {
            newSeverityValue = getEnumValue(newSeverity);
            oldSeverityValue = getEnumValue(oldSeverity);
            if (newSeverityValue > oldSeverityValue) {
                trendIndication = ProcessedEventTrendIndication.MORE_SEVERE;
            } else {
                if (newSeverityValue < oldSeverityValue) {
                    trendIndication = ProcessedEventTrendIndication.LESS_SEVERE;
                } else {
                    trendIndication = ProcessedEventTrendIndication.NO_CHANGE;
                }
            }
        }
        return trendIndication;
    }

    /**
     * Builds String with alarm attributes to form a unique identifier. ObjectOfReference,AlarmNumber separated by DELIMITER("@@@") forms the
     * identifier if AlarmNumber is greater than 0 in the received alarm. ObjectOfReference,SpecificProblem,ProbableCause,EventType each separated by
     * DELIMITER("@@@") forms the identifier if AlarmNumber is not set in the received alarm.
     *
     * @param ProcessedAlarmEvent
     *            processedAlarmEvent
     * @return String
     */
    public static String getKeyFromAlarm(final ProcessedAlarmEvent processedAlarmEvent) {
        final StringBuilder stringBuilder = new StringBuilder();
        if (processedAlarmEvent.getAlarmNumber() > 0) {
            return stringBuilder.append(processedAlarmEvent.getObjectOfReference()).append(CLEARALARMSCACHE_KEY_DELIMITER)
                    .append(processedAlarmEvent.getAlarmNumber()).toString();
        } else {
            return stringBuilder.append(processedAlarmEvent.getObjectOfReference()).append(CLEARALARMSCACHE_KEY_DELIMITER)
                    .append(processedAlarmEvent.getSpecificProblem()).append(CLEARALARMSCACHE_KEY_DELIMITER)
                    .append(processedAlarmEvent.getProbableCause()).append(CLEARALARMSCACHE_KEY_DELIMITER).append(processedAlarmEvent.getEventType())
                    .toString();
        }
    }

    /**
     * Deserializes the received content to list of EventNotification notifications.
     *
     * @param payLoad
     *            payload
     *
     * @return List of EventNotification
     */
    public static List<EventNotification> getEventNotifications(final byte[] payLoad) {
        List<EventNotification> eventNotifications = new ArrayList<>();
        try(final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payLoad);
                ObjectInput objectInput = new ObjectInputStream(byteArrayInputStream)) {
            final Object object = objectInput.readObject();
            eventNotifications = (List<EventNotification>) object;
        } catch (final Exception exception) {
            LOGGER.error("Exception in conversion of Object to Serialized form {} ", exception.getMessage());
            LOGGER.debug("Exception in conversion of Object to Serialized form {} ", exception);
        }
        return eventNotifications;
        }
    

    public static Serializable extractMessagePayload(final Message message) {
        if (message == null) {
            return null;
        }
        try {
            if (message instanceof ObjectMessage) {
                return ((ObjectMessage) message).getObject();
            }
        } catch (final JMSException exception) {
            LOGGER.error("Exception :: {} while extracting MessagePayload from the fmalarmqueue. ", exception.getMessage());
            LOGGER.debug("Exception :: {} while extracting MessagePayload from the fmalarmqueue :: {}", exception, message);
        }
        return null;
    }

    /**
     * Method converts severity from {@link ProcessedEventSeverity} to integer value.
     *
     * @param {@link ProcessedEventSeverity} severity
     * @return int
     */
    private static int getEnumValue(final ProcessedEventSeverity severity) {
        int num = 0;
        if (ProcessedEventSeverity.CLEARED.equals(severity)) {
            num = 0;
        }
        if (ProcessedEventSeverity.WARNING.equals(severity)) {
            num = 1;
        }
        if (ProcessedEventSeverity.MINOR.equals(severity)) {
            num = 2;
        }
        if (ProcessedEventSeverity.MAJOR.equals(severity)) {
            num = 3;
        }
        if (ProcessedEventSeverity.CRITICAL.equals(severity)) {
            num = 4;
        }
        return num;
    }
}
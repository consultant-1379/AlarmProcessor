/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.eventsender;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_RECEIVED_TIME;
import static com.ericsson.oss.services.fm.common.util.AlarmAttributeDataPopulate.populateLimitedAlarmAttributes;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FLAG_ONE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FM_CORE_OUT_QUEUE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FM_NB_QUEUE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FM_SNMP_NB_TOPIC;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.LAST_DELIVERED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ROUTE_TO_NMS;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.DELIVERY_ATTEMPT;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARMS_EVENTS_CHANNEL_URI;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;

import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.EventConfigurationBuilder;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.ericsson.oss.services.fm.alarmprocessor.builders.AlarmTextRouteInputEventBuilder;
import com.ericsson.oss.services.fm.alarmprocessor.builders.MetaDataInformationBuilder;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.models.processedevent.ATRInputEvent;
import com.ericsson.oss.services.fm.models.processedevent.AlarmMetadataInformation;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;

/**
 * Responsible to send Processed Alarm to Destination. Sends the processed alarm to {@link FMCoreOutQueue} for NorthBound and UI.
 * <p>
 * Sends the processed alarm to ATRProcessedEventChannel, AlarmMetaDataChannel for Auto Acknowledgment and building MetaDataInformation.
 */
@Stateless
public class ModeledEventSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModeledEventSender.class);

    @Inject
    @Modeled
    private EventSender<ProcessedAlarmEvent> modEventSender;

    @Inject
    @Modeled
    private EventSender<EventNotificationBatch> eventSender;

    @Inject
    @Modeled
    private EventSender<ATRInputEvent> atrModeledEventSender;

    @Inject
    @Modeled
    private EventSender<AlarmMetadataInformation> metaDataInformationEvent;

    @Inject
    private AlarmTextRouteInputEventBuilder atrInputEventBuilder;

    @Inject
    private MetaDataInformationBuilder metaDataInformationBuilder;

    @Inject
    private ConfigParametersListener configParametersListener;

    /**
     * Send {@link ProcessedAlarmEvent} to FMNorthBoundQueue.
     *
     * @param processedAlarmEvent
     *            The processed alarm event to be sent.
     * @param isClearListAlarm
     *            boolean value indicating if the alarm received is CLEAR_LIST alarm.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean sendEventToCorbaNbi(final ProcessedAlarmEvent processedAlarmEvent, final boolean isClearListAlarm,
                                       final String alarmReceivedTime) {
        boolean eventSent = false;
        if (!isClearListAlarm) {
            final Boolean correlatedVisibility = processedAlarmEvent.getCorrelatedVisibility();
            final Boolean visibility = processedAlarmEvent.isVisibility();
            if (visibility != null && visibility) {
                sendEventToFmNorthBoundQueue(processedAlarmEvent, alarmReceivedTime);
                LOGGER.debug("Alarm is successfully sent to the NorthBoundQueue. ProcessedAlarmEvent {} ", processedAlarmEvent);
                eventSent = true;
            } else if (correlatedVisibility != null && correlatedVisibility
                    && FMX_HIDE.equals(processedAlarmEvent.getAdditionalInformation().get(HIDE_OPERATION))) {
                LOGGER.debug("Not Sending the Fake Clear to NBI! It will be sent after sending the original to FMX and History.");
                eventSent = true;
            }
        }
        return eventSent;
    }

    /**
     * Send {@link ProcessedAlarmEvent} to CoreOutQueue.
     *
     * @param processedAlarmEvent
     *            The processed alarm event to be sent.
     * @param alarmReceivedTime
     *            The string containing the alarm received time.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendEventToCoreOutQueue(final ProcessedAlarmEvent processedAlarmEvent, final String alarmReceivedTime) {
        if (null != alarmReceivedTime && !alarmReceivedTime.isEmpty()) {
            final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
            eventConfigurationBuilder.addEventProperty(ALARM_RECEIVED_TIME, alarmReceivedTime);
            modEventSender.send(processedAlarmEvent, FM_CORE_OUT_QUEUE, eventConfigurationBuilder.build());
        } else {
            modEventSender.send(processedAlarmEvent, FM_CORE_OUT_QUEUE);
        }
        LOGGER.debug("Alarm is successfully sent to CoreOutQueue. ProcessedAlarmEvent {} ", processedAlarmEvent);
    }

    /**
     * Send {@link ProcessedAlarmEvent} to FMSnmpNorthBoundTopic.
     *
     * @param processedAlarmEvent
     *            The processed alarm event to be sent.
     * @param alarmReceivedTime
     *            The string containing the alarm received time.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendEventToSnmpNbi(final ProcessedAlarmEvent processedAlarmEvent, final String alarmReceivedTime) {
        final Boolean visibility = processedAlarmEvent.isVisibility();
        if (visibility != null && visibility) {
            if (null != alarmReceivedTime && !alarmReceivedTime.isEmpty()) {
                final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
                eventConfigurationBuilder.addEventProperty(ALARM_RECEIVED_TIME, alarmReceivedTime);
                modEventSender.send(processedAlarmEvent, FM_SNMP_NB_TOPIC, eventConfigurationBuilder.build());
            } else {
                modEventSender.send(processedAlarmEvent, FM_SNMP_NB_TOPIC);
            }
            LOGGER.debug("Alarm is successfully sent to FMSnmpNorthBoundTopic. ProcessedAlarmEvent {} ", processedAlarmEvent);
        }
    }

    /**
     * Send {@link ProcessedAlarmEvent} alarm meta data to AlarmMetaDataQueue.
     *
     * @param processedAlarmEvent
     *            The processed alarm event to be sent.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendAlarmMetaData(final ProcessedAlarmEvent processedAlarmEvent) {
        final Boolean visibility = processedAlarmEvent.isVisibility();
        if (visibility != null && visibility) {
            // Sending AlarmMetadataInformation to AlarmMetaDataQueue.
            sendAlarmMetaDataInformation(processedAlarmEvent);
        }
    }

    /**
     * Send {@link ProcessedAlarmEvent} attribute input to AlarmRoutingServiceQueue.
     *
     * @param processedAlarmEvent
     *            The processed alarm event to be sent.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendAtrInput(final ProcessedAlarmEvent processedAlarmEvent) {
        final Boolean visibility = processedAlarmEvent.isVisibility();
        if (visibility != null && visibility) {
            // Sending ATRInputEvent to AlarmRoutingServiceQueue.
            sendAtrInputEvent(processedAlarmEvent);
        }
    }

    /**
     * Send {@link ProcessedAlarmEvent} fake clear alarm to corresponding destination.
     *
     * @param processedAlarmEvent
     *            The processed alarm event to be sent.
     * @param alarmReceivedTime
     *            The string containing the alarm received time.
     * @param sendFakeClearToNbi
     *            boolean indicates whether a fake clear is to be sent to NorthBound or not.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendFakeClear(final ProcessedAlarmEvent processedAlarmEvent, final String alarmReceivedTime, final boolean sendFakeClearToUiAndNbi) {
        final Boolean visibility = processedAlarmEvent.isVisibility();
        final Boolean correlatedVisibility = processedAlarmEvent.getCorrelatedVisibility();
        if (visibility != null && visibility) {
            return;
        }

        if (sendFakeClearToUiAndNbi || correlatedVisibility != null && correlatedVisibility
                && FMX_HIDE.equals(processedAlarmEvent.getAdditionalInformation().get(HIDE_OPERATION))) {
            // Sending dummy clear alarm to hide the alarm from UI.
            buildAndSendFakeClear(processedAlarmEvent, alarmReceivedTime);
        }
    }

    /**
     * Send {@link ProcessedAlarmEvent} fake clear alarm to Corba NBI.
     *
     * @param processedAlarmEvent
     *            The processed alarm event to be sent.
     * @param alarmReceivedTime
     *            The string containing the alarm received time.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendFakeClearToBeSentToCorbaNbi(final ProcessedAlarmEvent processedAlarmEvent, final String alarmReceivedTime) {
        buildAndSendFakeClearToBeSentToCorbaNbi(processedAlarmEvent, alarmReceivedTime);
    }

    public void sendEventNotificationBatchBackToQueue(final EventNotificationBatch alarmsToBeResentToQueue, final int deliveryAttempt) {
        LOGGER.warn("Exception while processing an alarm!! Resending it to ClusteredFMMediationChannel to reprocess it another time.");
        final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
        eventConfigurationBuilder.delayToDeliver(configParametersListener.getRedeliveryDelay(), TimeUnit.MILLISECONDS);
        eventConfigurationBuilder.addEventProperty(DELIVERY_ATTEMPT, deliveryAttempt + 1);
        eventSender.send(alarmsToBeResentToQueue, ALARMS_EVENTS_CHANNEL_URI, eventConfigurationBuilder.build());
        LOGGER.debug("RESENT the EventNotificationBatch");
    }

    private void sendEventToFmNorthBoundQueue(final ProcessedAlarmEvent processedAlarmEvent, final String alarmReceivedTime) {
        final Integer timeToDeliver = getAlarmDelayToDeliver(processedAlarmEvent);
        boolean isEventCreatedTimeAvailable = true;
        if (null == alarmReceivedTime || alarmReceivedTime.isEmpty()) {
            isEventCreatedTimeAvailable = false;
        }
        final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
        if (isEventCreatedTimeAvailable) {
            eventConfigurationBuilder.addEventProperty(ALARM_RECEIVED_TIME, alarmReceivedTime);
        }
        if (timeToDeliver != null) {
            try {
                eventConfigurationBuilder.addEventProperty(LAST_DELIVERED, processedAlarmEvent.getAdditionalInformation().get(LAST_DELIVERED));
                eventConfigurationBuilder.delayToDeliver(timeToDeliver, TimeUnit.MILLISECONDS);
            } catch (final Exception exception) {
                LOGGER.error("Exception occured while adding timeToDeliver:{} to alarm:{}", timeToDeliver, populateLimitedAlarmAttributes(processedAlarmEvent));
                LOGGER.debug("Exception occured while adding timeToDeliver:{} to alarm:{}", timeToDeliver, processedAlarmEvent);
            }
        }
        processedAlarmEvent.getAdditionalInformation().remove(LAST_DELIVERED);
        if (isEventCreatedTimeAvailable || timeToDeliver != null) {
            modEventSender.send(processedAlarmEvent, FM_NB_QUEUE, eventConfigurationBuilder.build());
        } else {
            modEventSender.send(processedAlarmEvent, FM_NB_QUEUE);
        }
    }

    private Integer getAlarmDelayToDeliver(final ProcessedAlarmEvent processedAlarmEvent) {
        Integer timeToDeliver = null;
        if (processedAlarmEvent.getLastUpdatedTime() != null && processedAlarmEvent.getAdditionalInformation().get(LAST_DELIVERED) != null) {
            try {
                final Long diffTimeInMilliSeconds = Math.abs(Long.parseLong(processedAlarmEvent.getAdditionalInformation().get(LAST_DELIVERED))
                        - processedAlarmEvent.getLastUpdatedTime().getTime());
                if (diffTimeInMilliSeconds != 0) {
                    timeToDeliver = diffTimeInMilliSeconds.intValue();
                }
            } catch (final NumberFormatException numberFormatException) {
                LOGGER.error("Exception occurred for Last Delivered time returning timeToDeliver as Null : ", numberFormatException);
            }
        }
        LOGGER.debug("Delay time to Deliver is {},  with Last Delivered (millisec) is {}", timeToDeliver,
                processedAlarmEvent.getAdditionalInformation().get(LAST_DELIVERED));
        return timeToDeliver;
    }

    /**
     * Method builds a fake clear alarm to hide (Remove/Close) the original alarm when Hide request comes on a Show alarm.
     *
     * @param {@link
     *            ProcessedAlarmEvent} event
     */
    private void buildAndSendFakeClear(final ProcessedAlarmEvent event, final String alarmReceivedTime) {
        LOGGER.debug("buildAndSendFakeClear for {}", event);
        setAlarmAttributesForFakeClear(event);
        event.setVisibility(true);
        sendEventToFmNorthBoundQueue(event, alarmReceivedTime);
        modEventSender.send(event, FM_CORE_OUT_QUEUE);
        modEventSender.send(event, FM_SNMP_NB_TOPIC);
    }

    /**
     * Builds and send Fake Clear for an alarm that was sent earlier to Corba North Bound. RouteToNMS attribute is sent to 1 as this fake clear needs
     * to reach Corba North Bound.
     *
     * @param processedAlarmEvent
     *            alarm to be sent.
     * @param alarmReceivedTime
     *            receivedTime of the alarm in ENM.
     */
    private void buildAndSendFakeClearToBeSentToCorbaNbi(final ProcessedAlarmEvent processedAlarmEvent, final String alarmReceivedTime) {
        setAlarmAttributesForFakeClear(processedAlarmEvent);
        //Ensure to set RouteToNMS=1 as this needs to be reached to CorbaNBI.
        processedAlarmEvent.getAdditionalInformation().put(ROUTE_TO_NMS, FLAG_ONE);
        LOGGER.debug("About to send fake clear to be sent to CorbaNBI:{} with:{}", processedAlarmEvent, alarmReceivedTime);
        sendEventToFmNorthBoundQueue(processedAlarmEvent, alarmReceivedTime);
    }

    private void setAlarmAttributesForFakeClear(final ProcessedAlarmEvent event) {
        if (FMProcessedEventType.ERROR_MESSAGE.equals(event.getRecordType())
                || FMProcessedEventType.REPEATED_ERROR_MESSAGE.equals(event.getRecordType())) {
            event.setAlarmState(ProcessedEventState.ACTIVE_ACKNOWLEDGED);
            event.setLastAlarmOperation(ProcessedLastAlarmOperation.ACKSTATE_CHANGE);
            event.setAckOperator(FMX_HIDE_OPERATOR);
        } else {
            event.setAlarmState(ProcessedEventState.CLEARED_ACKNOWLEDGED);
            event.setLastAlarmOperation(ProcessedLastAlarmOperation.CLEAR);
            event.setPresentSeverity(ProcessedEventSeverity.CLEARED);
            event.setCeaseOperator(FMX_HIDE_OPERATOR);
            //CeaseTime is updated to lastUpdatedTime.It could also be current time.
            event.setCeaseTime(event.getLastUpdatedTime());
        }
    }

    /**
     * Sends {@link AlarmMetadataInformation} to its registered destination AlarmMetaDataChannel.
     *
     * @param ProcessedAlarmEvent
     *            processedAlarmEvent
     */
    private void sendAlarmMetaDataInformation(final ProcessedAlarmEvent processedAlarmEvent) {
        final AlarmMetadataInformation metaDataInformation = metaDataInformationBuilder.build(processedAlarmEvent);
        metaDataInformationEvent.send(metaDataInformation);
        LOGGER.debug("Sent AlarmMetadataInformation to registered destination. Event: {}", metaDataInformation);
    }

    /**
     * Sends {@link ATRInputEvent} to its registered destination ATRProcessedEventChannel.
     *
     * @param ProcessedAlarmEvent
     *            processedAlarmEvent
     */
    private void sendAtrInputEvent(final ProcessedAlarmEvent processedAlarmEvent) {
        final ATRInputEvent atrInputEvent = atrInputEventBuilder.build(processedAlarmEvent);
        atrModeledEventSender.send(atrInputEvent);
        LOGGER.debug("Sent ATRInputEvent to registered ATRProcessedEventChannel for ATR processing. Event: {}", atrInputEvent);
    }
}

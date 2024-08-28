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

package com.ericsson.oss.services.fm.alarmprocessor.listeners;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.DELIVERY_ATTEMPT;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.REDELIVERY_ATTEMPTS_REACHED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.DATABASE_SCHEMA;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.extractMessagePayload;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.getEventNotifications;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.eventbus.modeled.core.SerializableModeledEventData;
import com.ericsson.oss.itpf.sdk.eventbus.model.ModeledEvent;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmPreProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ModeledEventSender;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.UnProcessedAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.BufferedData;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
/**
 * Receives modeled event {@link EventNotificationBatch} from fmalarmqueue and does further processing.
 */
@ApplicationScoped
public class FmAlarmsListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FmAlarmsListener.class);

    @Inject
    private AlarmPreProcessor alarmPreProcessor;

    @Inject
    private ActiveThreadsHolder threadsHolder;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private UnProcessedAlarmHandler unProcessedAlarmHandler;

    @Inject
    private ModeledEventSender modeledEventSender;

    @Override
    public void onMessage(final Message message) {
        long threadId = 0;
        EventNotificationBatch eventNotificationBatch = null;
        List<EventNotification> eventNotifications = null;
        ModeledEvent<EventNotificationBatch> modeledEvent = null;
        try {
            final SerializableModeledEventData serializableModeledEventData = (SerializableModeledEventData) extractMessagePayload(message);
            if (serializableModeledEventData != null) {
                modeledEvent = serializableModeledEventData.toEvent(EventNotificationBatch.class);
                eventNotificationBatch = modeledEvent.extract();
                threadId = Thread.currentThread().getId();
                eventNotifications = getEventNotifications(eventNotificationBatch.getSerializedData());
                avoidSyncEventsAdditionToBuffer(threadId, eventNotifications);
                if (eventNotifications != null) {
                    LOGGER.trace("Events before sending to alarm pre processor: {}", eventNotifications);
                    alarmPreProcessor.onEvent(eventNotifications);
                } else {
                    LOGGER.error("Conversion of SerializedData {} resulted in null", eventNotificationBatch);
                }
            } else {
                LOGGER.warn("Received null event notification batch");
            }
        } catch (final Exception exception) {
            if (exception.getMessage() != null && exception.getMessage().contains(
                    "Cannot begin transaction because there is at least one active transaction modifying the database schema")) {
                final int sentToNorthBoundCount =
                        unProcessedAlarmHandler.prepareAndSendUnprocessedAlarms(eventNotifications, false, threadId, DATABASE_SCHEMA);
                LOGGER.warn(
                        "Sent {} unprocessed alarms to Northbound as database schema upgrade ongoing and the exception is :: {}",
                        sentToNorthBoundCount, exception.getMessage());
                LOGGER.debug(
                        "Sent {} unprocessed alarms to Northbound as database schema upgrade ongoing and the exception is :: ",
                        sentToNorthBoundCount, exception);
            } else {
                LOGGER.info("Exception caught while processing  eventNotifications :: {}  ", eventNotifications, exception);
               if(exception.getClass().getName().contains("ComponentIsStoppedException")){
                LOGGER.error("Ejb component is unavailable/stopped  to proceed with the message, may be jboss is starting/stopping {}", exception.getMessage());
                }
                else if (modeledEvent != null) {
                    reDeliveryEventNotifications(modeledEvent, eventNotificationBatch, threadId);
                } else {
                    LOGGER.warn("Received null modeledEvent");
                }

            }
        } finally {
            threadsHolder.remove(threadId);
            LOGGER.debug("ThreadId removed from the buffer is {}", threadId);
        }
    }

    private void reDeliveryEventNotifications(final ModeledEvent<EventNotificationBatch> modeledEvent,
            final EventNotificationBatch eventNotificationBatch, final Long problematicThreadId) {
        final Map<String, Object> eventProperties = modeledEvent.getModeledProperties();
        final Object deliveryAttempt = eventProperties.get(DELIVERY_ATTEMPT);
        List<EventNotification> eventNotifications = null;
        try {
        if (deliveryAttempt == null || Integer.parseInt(deliveryAttempt.toString()) < configParametersListener.getRedeliveryAttempts()) {
            modeledEventSender.sendEventNotificationBatchBackToQueue(eventNotificationBatch,
                    deliveryAttempt == null ? 0 : Integer.parseInt(deliveryAttempt.toString()));
        } else {
            LOGGER.warn("Delivery attempts have crossed the defined threshold. Sending alarm as unprocessed alarm");
             eventNotifications = getEventNotifications(eventNotificationBatch.getSerializedData());

                final int sentToNorthBoundCount =
                        unProcessedAlarmHandler.prepareAndSendUnprocessedAlarms(eventNotifications, false, problematicThreadId,
                                REDELIVERY_ATTEMPTS_REACHED);
                LOGGER.warn(
                        "Sent {} unprocessed alarms to Northbound as delivery attempts have crossed the defined threshold.",
                        sentToNorthBoundCount);
            }
        }
        catch (final Exception exception) {
                LOGGER.error("Exception caught while processing unprocessed alarm ", exception);
                LOGGER.debug("Exception caught :: {} while processing unprocessed alarm :: {}  ", exception, eventNotifications);
            }
        }

    private void avoidSyncEventsAdditionToBuffer(final long threadId, final List<EventNotification> eventNotifications){
        if (configParametersListener.isCircuitBreakerEnabled()) {
            final EventNotification firstAlarm = eventNotifications.get(0);
            LOGGER.debug("First Alarm in the received EventNotifications is {}", firstAlarm);
            if (firstAlarm != null && !firstAlarm.getRecordType().equals(FMProcessedEventType.SYNCHRONIZATION_STARTED.toString())) {
                threadsHolder.put(threadId, new BufferedData(System.currentTimeMillis(), false, eventNotifications));
                LOGGER.debug("ThreadId added to the buffer is {}", threadId);
            } else {
                LOGGER.debug("Not adding the {} Sync alarms to the buffer", eventNotifications.size());
            }
        }
    }

}

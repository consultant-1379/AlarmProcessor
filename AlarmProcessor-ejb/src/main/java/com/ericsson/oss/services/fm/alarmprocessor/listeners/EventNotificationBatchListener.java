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

package com.ericsson.oss.services.fm.alarmprocessor.listeners;

import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.extractMessagePayload;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.getEventNotifications;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.SECONDARY_CONSUMER;

import java.util.List;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.eventbus.modeled.core.SerializableModeledEventData;
import com.ericsson.oss.itpf.sdk.eventbus.model.ModeledEvent;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.UnProcessedAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder.ProcessingState;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingRuntimeException;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;

/**
 * This is a {@link MessageListener }, which listens to fmalarmqueue.
 */
public class EventNotificationBatchListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventNotificationBatchListener.class);

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private UnProcessedAlarmHandler unProcessedAlarmHandler;

    @Inject
    private ActiveThreadsHolder activeThreadsHolder;

    @Override
    public void onMessage(final Message message) {
        if (!ProcessingState.HARD_THRESHOLD_CROSSED.equals(activeThreadsHolder.getProcessingState())) {
            activeThreadsHolder.stopConsumers();
        }
        EventNotificationBatch eventNotificationBatch = null;
        List<EventNotification> eventNotifications = null;
        ModeledEvent<EventNotificationBatch> modeledEvent = null;
        EventNotification firstAlarm = null;
        try {
            final SerializableModeledEventData serializableModeledEventData = (SerializableModeledEventData) extractMessagePayload(message);
            if (serializableModeledEventData != null) {
                modeledEvent = serializableModeledEventData.toEvent(EventNotificationBatch.class);
                eventNotificationBatch = modeledEvent.extract();
                eventNotifications = getEventNotifications(eventNotificationBatch.getSerializedData());
                if (eventNotifications != null) {
                    LOGGER.trace("Events before sending to alarm pre processor: {}", eventNotifications);
                    firstAlarm = eventNotifications.get(0);
                    LOGGER.debug("First Alarm in the received EventNotifications is {}", firstAlarm);
                    if (firstAlarm != null && !firstAlarm.getRecordType().equals(FMProcessedEventType.SYNCHRONIZATION_STARTED.toString())) {
                        final int sentToNorthBoundCount =
                                unProcessedAlarmHandler.prepareAndSendUnprocessedAlarms(eventNotifications, true, null, SECONDARY_CONSUMER);
                        apsInstrumentedBean.incrementAlarmsProcessedBySecondaryConsumers(sentToNorthBoundCount);
                        LOGGER.warn("HARD_THRESHOLD mode is ON. {} events are sent to NorthBound directly from secondary consumers",
                                sentToNorthBoundCount);
                    } else {
                        LOGGER.debug("Not sending the {} Sync alarms as Unprocessed from Secondary consumers", eventNotifications.size());
                    }
                } else {
                    LOGGER.error("Conversion of SerializedData {} resulted in null", eventNotificationBatch);
                }
            } else {
                LOGGER.warn("Received null event notification batch");
            }
        } catch (final Exception exception) {
            LOGGER.error("EventNotificationBatchListener:Exception caught :: {} while processing  eventNotifications ", exception.getMessage());
            LOGGER.debug("EventNotificationBatchListener:Exception caught :: {} while processing  eventNotifications :: {}  ", exception,
                    eventNotifications);
            throw new AlarmProcessingRuntimeException(exception.getMessage());
        }
    }

}

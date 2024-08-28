/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.eventsender;

import static com.ericsson.oss.services.fm.alarmprocessor.converters.EventnotificationToProcessedAlarmEventConverter.convert;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.SOURCE_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.common.targettype.handler.TargetTypeHandler;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class responsible for sending unprocessed alarms to NorthBound.
 */
@ApplicationScoped
public class UnProcessedAlarmHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnProcessedAlarmHandler.class);

    @Inject
    private UnProcessedAlarmSender unProcessedAlarmSender;

    @Inject
    private AlarmValidator alarmValidator;

    @Inject
    private ActiveThreadsHolder threadsHolder;

    @Inject
    private TargetTypeHandler targetTypeHandler;

    public int prepareAndSendUnprocessedAlarms(final List<EventNotification> events, final boolean fromSecondaryConsumers,
            final Long problematicThreadId, final String consumerType) {
        int sentCount = 0;
        for (final EventNotification eventNotification : events) {
            final ProcessedAlarmEvent alarmRecord = convertEventNotificationToProcessedAlarmEvent(eventNotification);
            if (alarmValidator.isAlarmToBeHandled(alarmRecord)) {
                if (fromSecondaryConsumers) {
                    unProcessedAlarmSender.sendUnProcessedEvents(alarmRecord, consumerType);
                    sentCount++;
                } else {
                    if (threadsHolder.get(problematicThreadId) != null) {
                        unProcessedAlarmSender.sendUnProcessedEvents(alarmRecord, consumerType);
                        sentCount++;
                    } else {
                        break;
                    }
                }
            }
        }
        return sentCount;
    }

    /**
     * Method which sets the source type in received event notification and converts it to alarm record.
     * @param {@link EventNotification} eventNotification
     */
    private ProcessedAlarmEvent convertEventNotificationToProcessedAlarmEvent(final EventNotification eventNotification) {
        final String fdn = eventNotification.getAdditionalAttribute(FDN);
        eventNotification.addAdditionalAttribute(SOURCE_TYPE, eventNotification.getSourceType());
        try {
            final String targetType = targetTypeHandler.get(fdn);
            if (targetType != null && !targetType.isEmpty()) {
                LOGGER.debug("The target type for fdn : {} is : {}", fdn, targetType);
                eventNotification.setSourceType(targetType);
                // Adding in additional info as well in case when source type is not proper in the event from FMX.
                eventNotification.addAdditionalAttribute(SOURCE_TYPE, targetType);
                }
        }catch(Exception e) {
            LOGGER.warn("Exception caught: {} while getting target type for the FDN, proceding to send the unprocessed alarm with source type as target type.", e.getMessage());
        }
        return convert(eventNotification);
    }

}

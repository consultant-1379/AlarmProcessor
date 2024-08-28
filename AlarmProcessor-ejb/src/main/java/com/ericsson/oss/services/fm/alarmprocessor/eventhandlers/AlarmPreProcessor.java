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

package com.ericsson.oss.services.fm.alarmprocessor.eventhandlers;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.RATE_DETECTION_SESSION_NAME;
import static com.ericsson.oss.services.fm.alarmprocessor.converters.EventnotificationToProcessedAlarmEventConverter.convert;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.SOURCE_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_PROCESSED;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.HB_FAILURE_NO_SYNCH;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.HEARTBEAT_ALARM;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.NODE_SUSPENDED;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.enrichment.AlarmEnricher;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.protection.AlarmOverloadProtectionFilter;
import com.ericsson.oss.services.fm.alarmprocessor.util.FmxAttributesWriter;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.common.targettype.handler.TargetTypeHandler;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.ratedetectionengine.api.RateDetectionService;

/**
 * This class converts the EventNotificationBatch received from AlarmQueueMsgReceiverMDB to {@link ProcessedAlarmEvent}. <br>
 * <p>
 * The converted {@link ProcessedAlarmEvent} will be sent downstream for further processing.
 */
public class AlarmPreProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmPreProcessor.class);

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private AlarmHandler alarmHandler;

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private FmxAttributesWriter fmxAttributesWriter;

    @Inject
    private AlarmValidator alarmValidator;

    @EServiceRef
    private AlarmEnricher alarmEnricher;

    @EServiceRef
    private RateDetectionService rateDetectionService;

    @Inject
    private TargetTypeHandler targetTypeHandler;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private AlarmOverloadProtectionFilter alarmOverloadProtectionFilter;

    /**
     * Method called when this handler is invoked.
     * @param inputEvent
     *            - Parameter type is not changed as it is required once EPS becomes transactional and is changed back to flows.
     */
    public void onEvent(final Object inputEvent) {
        final List<EventNotification> eventNotifications = (List<EventNotification>) inputEvent;
        LOGGER.debug("Event received to APS {}", eventNotifications);
        prepareAlarmRecord(eventNotifications);
    }
    /**
     * Prepare {@link ProcessedAlarmEvent} from {@link EventNotification} object and pass to next handlers for processing.AlarmRecord is also
     * enhanced with FMX attributes if any FMX rules are matched .
     * @param List
     *            eventNotifications
     */
    private void prepareAlarmRecord(final List<EventNotification> eventNotifications) {
        final Iterator<EventNotification> iterator = eventNotifications.iterator();
        Long currentIntervalValue = 0L;
        final boolean alarmOverloadProtectionOn = configParametersListener.getAlarmOverloadProtectionOn();
        while (iterator.hasNext()) {
            // Instrumentation is moved here to count each of the SyncAlarm rather counting the Batch as one alarm.
            apsInstrumentedBean.increaseAlarmCountReceivedByAPS();
            EventNotification eventNotification = iterator.next();
            setSourceType(eventNotification);

            final ProcessedAlarmEvent alarmRecord = convert(eventNotification);
            if (alarmEnricher != null) {
                eventNotification = alarmEnricher.enrichNotification(eventNotification);
            }
            fmxAttributesWriter.setEnrichmentValues(alarmRecord, eventNotification);
            updateVisibilityForFmxProcessed(alarmRecord);
            if (alarmValidator.isAlarmToBeHandled(alarmRecord)) {
                if (alarmValidator.isAlarmToBeCounted(alarmRecord)) {
                    currentIntervalValue++;
                }
                if (alarmOverloadProtectionFilter.applyAlarmOverloadProtectionFilter(alarmRecord)) {
                    alarmHandler.onEvent(alarmRecord);
                }
            }
        }
        if (alarmOverloadProtectionOn && currentIntervalValue > 0) {
            currentIntervalValue = rateDetectionService.increment(RATE_DETECTION_SESSION_NAME, currentIntervalValue);
            LOGGER.debug("Current Interval alarm counter on APS {}", currentIntervalValue);
        }
    }

    /**
     * Method which sets the source type in received event notification.
     * @param {@link EventNotification} eventNotification
     */
    private void setSourceType(final EventNotification eventNotification) {
        final String fdn = eventNotification.getAdditionalAttribute(FDN);
        eventNotification.addAdditionalAttribute(SOURCE_TYPE, eventNotification.getSourceType());
        final String targetType = targetTypeHandler.get(fdn);
        if (targetType != null && !targetType.isEmpty()) {
            eventNotification.setSourceType(targetType);
            // Adding in additional info as well in case when source type is not proper in the event from FMX.
            eventNotification.addAdditionalAttribute(SOURCE_TYPE, targetType);
        }
    }

    /**
     * Method which gets correlated alarm's visibility from database and sets the same in received alarm.
     * @param {@link ProcessedAlarmEvent} alarmEvent
     */
    private void changeDisplaystatus(final ProcessedAlarmEvent alarmEvent) {
        if (alarmEvent.getCorrelatedPOId() > 0) {
            // Find PO by CorrelatedId and update the status
            final Object visibility = alarmReader.readAttribute(alarmEvent.getCorrelatedPOId(), VISIBILITY);
            if (visibility != null) {
                alarmEvent.setVisibility((boolean) visibility);
            }
        }
    }

    /**
     * visibility of FMX processed alarm of record type heartbeat_alarm, Hb_failure_No_Sync, node_suspended is updated to its correlated alarm's
     * visibility.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    private void updateVisibilityForFmxProcessed(final ProcessedAlarmEvent alarmRecord) {
        if ((HEARTBEAT_ALARM.equals(alarmRecord.getRecordType())
                || HB_FAILURE_NO_SYNCH.equals(alarmRecord.getRecordType())
                || NODE_SUSPENDED.equals(alarmRecord.getRecordType())) && FMX_PROCESSED.equals(alarmRecord.getFmxGenerated())) {
            // To Be Added OUT_OF_SYNC alarm?
            changeDisplaystatus(alarmRecord);
        }
    }
}

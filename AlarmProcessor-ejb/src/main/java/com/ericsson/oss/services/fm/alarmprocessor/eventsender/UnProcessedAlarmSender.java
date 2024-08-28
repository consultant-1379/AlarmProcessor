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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.LAST_DELIVERED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FM_SNMP_NB_TOPIC;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.fm.capability.util.ModelCapabilities;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability.FmDatabaseAvailabilityCacheManager;
import com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability.FmDatabaseAvailabilityConfigurationListener;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.common.targettype.handler.TargetTypeHandler;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity.CLEARED;

/**
 * Responsible to send UnProcessed Alarm to Destination. Sends the unprocessed alarm to
 * {@link com.ericsson.oss.services.fm.models.channel.FMNorthBoundQueue} for NorthBound and {@link FMAlarmOutTopic} for UI.
 * <p>
 * FDN of NetworkElements whose Alarms could not be processed will be cached for initiation of alarm synchronization.
 */
@Stateless
public class UnProcessedAlarmSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnProcessedAlarmSender.class);
    private static final String FM_NB_QUEUE = "//global/FMNorthBoundQueue";

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private FmDatabaseAvailabilityConfigurationListener fmDataBaseAvailabilityConfigurationListener;

    @Inject
    private FmDatabaseAvailabilityCacheManager fmDataBaseAvailabilityCacheManager;

    @Inject
    @Modeled
    private EventSender<ProcessedAlarmEvent> modeledEventSender;

    @Inject
    private ModelCapabilities modelCapabilities;

    @Inject
    private TargetTypeHandler targetTypeHandler;

    /**
     * Send unprocessed Alarm to NorthBound and update FmAvailabilityCacheDefinition with fdn of NetworkElement to perform alarm synchronization 
     * later.
     *
     * @param ProcessedAlarmEvent
     *            alarmRecord
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendUnProcessedEvents(final ProcessedAlarmEvent alarmRecord , final String consumerType) {
        apsInstrumentedBean.incrementFailedAlarmCount();
        final boolean isAlarmSentToNbi = sendEvent(alarmRecord);
        if (isAlarmSentToNbi) {
            systemRecorder.recordEvent("APS- " + consumerType, EventLevel.DETAILED, "Alarm  ", alarmRecord.toString(),
                    "Max retries to process an alarm reached and unprocessed alarm is sent NorthBound");
        }
        if(isValidRecordTypeToBeSynced(alarmRecord)) {
            fmDataBaseAvailabilityCacheManager.addFdn(alarmRecord.getFdn());
        }
    }

    /**
     * Sends unprocessed alarm record {@link ProcessedAlarmEvent} to registered destination FmProcessedEventChannel if the configuration parameter
     * <b>handleDBFailure</b> is true.
     *
     * @param Object
     *            inputEvent
     * @return boolean
     */
    private boolean sendEvent(final Object inputEvent) {
        boolean isEventSent = false;
        final boolean handleDataBaseFailure = fmDataBaseAvailabilityConfigurationListener.getHandleDbFailure();
        if (handleDataBaseFailure) {
            final ProcessedAlarmEvent event = (ProcessedAlarmEvent) inputEvent;
            event.getAdditionalInformation().remove(LAST_DELIVERED);
            if (isValidRecordTypeToBeSent(event.getRecordType())) {
                //To send Unprocessed alarm to FMNorthBoundQueue (CORBA NBI)
                modeledEventSender.send(event, FM_NB_QUEUE);
                //To send Unprocessed alarm to FMSnmpNorthBoundTopic (SNMP NBI)
                modeledEventSender.send(event, FM_SNMP_NB_TOPIC);
                //To send the Unprocessed alarm to UI and other subscribers.
                modeledEventSender.send(event);
                LOGGER.debug("Sent event : {} to registered destination.", event);
                isEventSent = true;
            }
        }
        return isEventSent;
    }

    /**
     * Alarms with certain recordType are need not be sent to NorthBound in case of database failure.
     */
    private boolean isValidRecordTypeToBeSent(final FMProcessedEventType recordType) {
        switch (recordType) {
            case SYNCHRONIZATION_STARTED:
            case SYNCHRONIZATION_ENDED:
            case CLEAR_LIST:
            case SUPERVISION_SWITCHOVER:
            case UNDEFINED:
                return false;
            default:
                return true;
        }
    }

    /**
     * Alarms with certain recordType are need not be stored in the sync cache in case on database failure
     * because these entries aren't present on active alarm list.
     */
    private boolean isValidRecordTypeToBeSynced(final ProcessedAlarmEvent alarmRecord) {
        final FMProcessedEventType recordType = alarmRecord.getRecordType();
        switch (recordType) {
            case ERROR_MESSAGE:
            case REPEATED_ERROR_MESSAGE:
                return isErrorMessageSyncSupportedByNode(alarmRecord);
            case NON_SYNCHABLE_ALARM:
            case REPEATED_NON_SYNCHABLE:
                return false;
            case HEARTBEAT_ALARM:
            case NODE_SUSPENDED:
                return CLEARED.equals(alarmRecord.getPresentSeverity());
            default:
                return true;
        }
    }

    /**
     * In case on ERROR_MESSAGE or REPEATED_ERROR_MESSAGE return if the targetType support error message synchronization
     */
    private boolean isErrorMessageSyncSupportedByNode(final ProcessedAlarmEvent alarmRecord) {
        final String targetType = targetTypeHandler.get(alarmRecord.getFdn());
        return modelCapabilities.isErrorMessageSyncSupportedByNode(targetType);
    }
}
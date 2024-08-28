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

package com.ericsson.oss.services.fm.alarmprocessor.eventsender;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.LAST_DELIVERED;
import static com.ericsson.oss.services.fm.common.util.AlarmAttributeDataPopulate.populateLimitedAlarmAttributes;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATEDVISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_CREATED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.fm.common.models.RecordType;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.ServiceProxyProviderBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class sends processed alarms to the required destinations.
 */
public class ProcessedAlarmSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessedAlarmSender.class);

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private ModeledEventSender modeledEventSender;

    /**
     * Sends processed alarm to all appropriate destinations.
     *
     * @param {@link
     *            AlarmProcessingResponse} alarmResponse containing processed alarms to be sent.
     * @param {@link
     *            String} alarmReceivedTime represents the time stamp of alarm received to ENM.
     */
    public void sendAlarms(final AlarmProcessingResponse alarmProcessingResponse, final String alarmReceivedTime) {
        LOGGER.debug("Event received to ProcessedAlarmSender, sendAlarms {}", alarmProcessingResponse);
        final List<ProcessedAlarmEvent> processedAlarms = alarmProcessingResponse.getProcessedAlarms();
        if (processedAlarms == null || processedAlarms.isEmpty()) {
            return;
        }

        final Iterator<ProcessedAlarmEvent> iterator = processedAlarms.iterator();
        while (iterator.hasNext()) {
            final ProcessedAlarmEvent processedAlarmEvent = iterator.next();
            sendAlarm(processedAlarmEvent, alarmReceivedTime, alarmProcessingResponse.isSendFakeClearToNbi(),
                    alarmProcessingResponse.isSendFakeClearToUiAndNbi());
        }
    }

    /**
     * Alarm is first sent to FmNorthBoundQueue,then to FMX and at last to FmCoreOutQueue,AlarmMetaDataInformation and ATRInput channels. If sending
     * to NBI (hi priority service) fails will be propagate the exception causing the parent transaction to roll-back and re-manage the alarm. If
     * other send fails (low priority) no other parent action is required to prevent messages overflow on the net.
     *
     * @param processedAlarmEvent
     *            alarm to be sent to North Bound,UI.
     * @param alarmReceivedTime
     *            indicates alarm received time to ENM.
     * @param sendFakeClearToUiAndNbi
     *            boolean indicates whether a fake clear is to be sent to NMS and UI clients or not.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void sendAlarm(final ProcessedAlarmEvent processedAlarmEvent, final String alarmReceivedTime, final boolean sendFakeClearToNbi, final boolean sendFakeClearToUiAndNbi) {
        LOGGER.debug("Event received to ProcessedAlarmSender, sendAlarm {}", processedAlarmEvent);
        final boolean isClearListAlarm = processedAlarmEvent.getRecordType().name().equals(RecordType.CLEAR_LIST.name());
        final boolean isEventSentToNbi = modeledEventSender.sendEventToCorbaNbi(processedAlarmEvent, isClearListAlarm, alarmReceivedTime);
        final String processingType = processedAlarmEvent.getProcessingType();
        // Capturing the "lastDelivered" filed in the case of alarm in NormalProc case which means alarm wont be sent to FMX.
        // And if any Error in sending alarm to FMX( alarmSender.sendAlarm(processedAlarmEvent);) .Assuming that FMX is down ,
        // So alarm should sent to NBI and other UI's with visibility true and retain the calculation of alarm delay to NBI.
        String lastDelivered = null;
        if (!isEventSentToNbi) {
            lastDelivered = processedAlarmEvent.getAdditionalInformation().get(LAST_DELIVERED);
            processedAlarmEvent.getAdditionalInformation().remove(LAST_DELIVERED);
        }
        try {
            // Send alarms to FMX if processingType is either NORMAL_PROC/POST_PROC,
            // FmxGenerated alarms or ClearList alarms
            if (!NOT_SET.equals(processingType) || FMX_CREATED.equals(processedAlarmEvent.getFmxGenerated()) || isClearListAlarm) {
                serviceProxyProviderBean.getAlarmSender().sendAlarm(processedAlarmEvent);
            }
        } catch (final Exception exception) {
            LOGGER.error("Exception {} caught while forwarding FMX matched alarm {} to FmxAdaptorServiceQueue.", exception.getMessage(),populateLimitedAlarmAttributes(processedAlarmEvent));
            LOGGER.debug("Exception {} caught while forwarding FMX matched alarm {} to FmxAdaptorServiceQueue.", exception,processedAlarmEvent);
            updateVisibility(processedAlarmEvent);
            if (!isEventSentToNbi) {
                processedAlarmEvent.getAdditionalInformation().put(LAST_DELIVERED, lastDelivered);
                modeledEventSender.sendEventToCorbaNbi(processedAlarmEvent, isClearListAlarm, alarmReceivedTime);
            }
        }

        // Send to multiple destinations if clear list alarm flag is not set.
        if (isClearListAlarm) {
            systemRecorder.recordEvent("APS", EventLevel.DETAILED, "AlarmEvent", processedAlarmEvent.toString(),
                    "Not sent to multiple destinations as ClearListAlarm flag is set.");
            return;
        }

        try {
            modeledEventSender.sendEventToSnmpNbi(processedAlarmEvent, alarmReceivedTime);
        } catch (final Exception exception) {
            LOGGER.error("Exception {} caught while forwarding alarm {} to FMSnmpNorthBoundTopic.", exception.getMessage(), processedAlarmEvent);
            LOGGER.debug("Exception {} caught while forwarding alarm to FMSnmpNorthBoundTopic.", exception);
        }

        try {
            modeledEventSender.sendEventToCoreOutQueue(processedAlarmEvent, alarmReceivedTime);
        } catch (final Exception exception) {
            LOGGER.error("Exception {} caught while forwarding alarm {} to CoreOutQueue.", exception.getMessage(), processedAlarmEvent);
            LOGGER.debug("Exception {} caught while forwarding alarm to CoreOutQueue.", exception);
        }
        try {
            modeledEventSender.sendAlarmMetaData(processedAlarmEvent);
        } catch (final Exception exception) {
            LOGGER.error("Exception {} caught while forwarding alarm {} to AlarmMetaData.", exception.getMessage(), processedAlarmEvent);
            LOGGER.debug("Exception {} caught while forwarding alarm to AlarmMetaData.", exception);
        }
        try {
            modeledEventSender.sendAtrInput(processedAlarmEvent);
        } catch (final Exception exception) {
            LOGGER.error("Exception {} caught while forwarding alarm {} to AtrInput.", exception.getMessage(), processedAlarmEvent);
            LOGGER.debug("Exception {} caught while forwarding alarm to AtrInput.", exception);
        }
        try {
            modeledEventSender.sendFakeClear(processedAlarmEvent, alarmReceivedTime, sendFakeClearToUiAndNbi);
        } catch (final Exception exception) {
            LOGGER.error("Exception {} caught while forwarding alarm {} to FakeClear.", exception.getMessage(), processedAlarmEvent);
            LOGGER.debug("Exception {} caught while forwarding alarm to FakeClear.", exception);
        }
        if (sendFakeClearToNbi) {
            try {
                modeledEventSender.sendFakeClearToBeSentToCorbaNbi(processedAlarmEvent, alarmReceivedTime);
            } catch (final Exception exception) {
                LOGGER.error("Exception {} caught while forwarding alarm {} to CorbaNbi.", exception.getMessage(), processedAlarmEvent);
                LOGGER.debug("Exception {} caught while forwarding alarm to CorbaNbi.", exception);
            }
        }
    }

    /**
     * Updates visibility,correlatedVisibility of an alarm to true upon exception while sending {@link ProcessedAlarmEvent} to FMX. In such case alarm
     * visibility state needs to be updated to true and sent to NorthBound. To maintain consistency {@link ProcessedAlarmEvent} object is updated with
     * visibility attributes only if DPS operation succeeds. Note : There is one specific case here where DPS could throw an exception. Clear on a
     * Hidden Alarm will result in deletion of alarm from database and if sending this clear is failed to sent to FMX. An attempt to update Visibility
     * attribute will be made but DPS will throw an exception as the update operation is being made on an object marked for deletion. This exception
     * can be ignored silently as the alarm is going to be deleted anyhow. TODO: Check for the possibility to use alarmState attribute here. NOTE: It
     * involves XA transaction here as database is updated in a txn where message is sent to queue.
     */
    private void updateVisibility(final ProcessedAlarmEvent processedAlarmEvent) {
        try {
            final Map<String, Object> alarmAttributes = new HashMap<>();
            alarmAttributes.put(VISIBILITY, true);
            alarmAttributes.put(CORRELATEDVISIBILITY, true);
            openAlarmService.updateAlarm(processedAlarmEvent.getEventPOId(), alarmAttributes);
            processedAlarmEvent.setVisibility(true);
            processedAlarmEvent.setCorrelatedVisibility(true);
        } catch (final Exception exception) {
            LOGGER.warn("Exception in updateVisibility for ProcessedAlarmEvent {} is {}", populateLimitedAlarmAttributes(processedAlarmEvent), exception.getMessage());
            LOGGER.debug("Exception in updateVisibility for ProcessedAlarmEvent {} is {}", processedAlarmEvent, exception);
        }
    }

    /**
     * Sends processed duplicate alarm to all appropriate destinations.
     *
     * @param {@link
     *            AlarmProcessingResponse} alarmResponse containing processed alarms to be sent.
     * @param {@link
     *            String} alarmReceivedTime represents the time stamp of alarm received to ENM.
     * @param sendFakeClearToNbi
     *            boolean indicates whether a fake clear is to be sent to NMS or not.
     * @param sendFakeClearToUiAndNbi
     *            boolean indicates whether a fake clear is to be sent to NMS and UI clients or not.
     */
    public void sendDuplicateAlarms(final ProcessedAlarmEvent processedAlarmEvent, final String alarmReceivedTime, final boolean sendFakeClearToNbi, final boolean sendFakeClearToUiAndNbi) {
        LOGGER.debug("Event received to duplicate alarms ProcessedAlarmSender, sendAlarm {}", processedAlarmEvent);
        sendAlarm(processedAlarmEvent, alarmReceivedTime, sendFakeClearToNbi, sendFakeClearToUiAndNbi);
    }
}
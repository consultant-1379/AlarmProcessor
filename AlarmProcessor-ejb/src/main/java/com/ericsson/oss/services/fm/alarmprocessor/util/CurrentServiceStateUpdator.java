/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_TO_BE_PROCESSED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.APS;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CATEGORY;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CLEARED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CURRENT_SERVICE_STATE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FAILURE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FMALARM_SUPERVISION_MO_SUFFIX;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.HEARTBEATUPDATEREQUEST;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.INITIATE_SYNC;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.SOURCE_TYPE;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.ALARM_SUPERVISION_STATE;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.AUTOMATIC_SYNCHRONIZATION;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.fm.capability.util.ModelCapabilities;
import com.ericsson.oss.fm.common.models.AlarmSeverity;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.core.events.MediationClientType;
import com.ericsson.oss.mediation.core.events.OperationType;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.AxeBladeClusterHbClearTimerInvoker;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmSupervisionMoReader;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.service.model.FmMediationHeartBeatRequest;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

/**
 * Class updates current service state of fmFunctionMO when
 * HeartBeat/node_suspended/out_of_sync is received.
 */
public class CurrentServiceStateUpdator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentServiceStateUpdator.class);

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    @Inject
    private FmSupervisionMoReader fmSupervisionMoReader;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    private ModelCapabilities modelCapabilities;

    @Inject
    private AlarmReader alarmReader;

    @Inject
    private AxeBladeClusterHbClearTimerInvoker axeBladeClusterHbClearTimerInvoker;

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> mediationTaskSender;

    /**
     * Method updates current service state of fmFunctionMO when HeartBeatalarm
     * is received.
     */
    public Map<String, Boolean> updateForHeartBeatAlarm(final ProcessedAlarmEvent alarmRecord) {
        final Map<String, Boolean> response = new HashMap<>();
        final String fdn = alarmRecord.getFdn();
        final ProcessedEventSeverity presentSeverity = alarmRecord.getPresentSeverity();
        final String currentState = (String) fmFunctionMoService.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE);
        if (currentState == null) {
            LOGGER.error("currentServiceState value is null for fdn {}.Hence currentServiceState will not be updated and alarm will be discarded",
                    fdn);
            LOGGER.debug("currentServiceState value is null for fdn {}.Hence currentServiceState will not be updated and alarm {} will be discarded",
                    fdn, alarmRecord);
            apsInstrumentedBean.incrementDiscardedAlarmCount(presentSeverity);
            response.put(ALARM_TO_BE_PROCESSED, false);
            return response;
        }
        if (FmSyncStatus100.SYNC_ONGOING.name().equals(currentState)) {
            // Setting sync state to true as heartbeat alarm cannot be cleared in sync.
            fmFunctionMoService.updateCurrentServiceState(fdn, FmSyncStatus100.HEART_BEAT_FAILURE.name());
            systemRecorder.recordEvent(APS, EventLevel.DETAILED, "Ongoing Sync is Discarded and currentServiceState",
                    "is changed to HEART_BEAT_FAILURE for NetworkElement", fdn);
        } else if (FmSyncStatus100.HEART_BEAT_FAILURE.name().equals(currentState)) {
            if (ProcessedEventSeverity.CLEARED.equals(presentSeverity)) {
                updateHeartBeatStateToInService(alarmRecord, response);
            } else {
                // HB failure message already exists.Replace existing with the latest alarm.
                LOGGER.info("HeartBeat Alarm is already raised for fdn {}.Replacing existing HeartBeat alarm with the latest alarm", fdn);
            }
        } else if (!ProcessedEventSeverity.CLEARED.equals(presentSeverity)) {
            fmFunctionMoService.updateCurrentServiceState(fdn, FmSyncStatus100.HEART_BEAT_FAILURE.name());
            sendingMTRWithOOR(alarmRecord, fdn, FAILURE);
            systemRecorder.recordEvent(APS, EventLevel.DETAILED, CURRENT_SERVICE_STATE, "is changed to HEART_BEAT_FAILURE for NetworkElement", fdn);
        }
        response.put(ALARM_TO_BE_PROCESSED, true);
        return response;
    }

    /**
     * Method updates current service state of fmFunctionMO when nodesuspended
     * alarm is received.
     */
    public Map<String, Boolean> updateForNodeSuspendedAlarm(final ProcessedAlarmEvent alarmRecord) {
        final String fdn = alarmRecord.getFdn();
        final ProcessedEventSeverity presentSeverity = alarmRecord.getPresentSeverity();
        final Map<String, Boolean> response = new HashMap<>();
        final String currentState = (String) fmFunctionMoService.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE);
        if (currentState == null) {
            LOGGER.error("currentServiceState value is null for fdn {}.Hence currentServiceState will not be updated and alarm will be discarded",
                    fdn);
            LOGGER.debug("currentServiceState value is null for fdn {}.Hence currentServiceState will not be updated and alarm {} will be discarded",
                    fdn,alarmRecord);
            apsInstrumentedBean.incrementDiscardedAlarmCount(presentSeverity);
            response.put(ALARM_TO_BE_PROCESSED, false);
            return response;
        }
        if (FmSyncStatus100.NODE_SUSPENDED.name().equals(currentState)) {
            if (ProcessedEventSeverity.CLEARED.equals(presentSeverity)) {
                updateNodeSuspendedStateToInService(alarmRecord, response);
            } else {
                if (FmSyncStatus100.HEART_BEAT_FAILURE.name().equals(currentState)) {
                    LOGGER.info(
                            "currentServiceState of NetworkElement {} is HEART_BEAT_FAILURE.So is not updated to NODE_SUSPENDED, alarm is processed",
                            fdn);
                }
                if (FmSyncStatus100.NODE_SUSPENDED.name().equals(currentState)) {
                    LOGGER.info("Node Suspended alarm is already raised for {}.Replace existing NodeSuspended alarm with latest alarm.", fdn);
                } else {
                    fmFunctionMoService.updateCurrentServiceState(fdn, FmSyncStatus100.NODE_SUSPENDED.name());
                    systemRecorder.recordEvent(APS, EventLevel.DETAILED, CURRENT_SERVICE_STATE, "is changed to NODE_SUSPENDED for fdn", fdn);
                }
            }
        } else if (!ProcessedEventSeverity.CLEARED.equals(presentSeverity)) {
            fmFunctionMoService.updateCurrentServiceState(fdn, FmSyncStatus100.NODE_SUSPENDED.name());
        }
        response.put(ALARM_TO_BE_PROCESSED, true);
        return response;
    }

    /**
     * Method updates current service state of fmFunctionMO when OutOfSync alarm
     * is received.
     */
    public Map<String, Boolean> updateForOutOfSyncAlarm(final ProcessedAlarmEvent alarmRecord) {
        final String fdn = alarmRecord.getFdn();
        final ProcessedEventSeverity presentSeverity = alarmRecord.getPresentSeverity();
        final Map<String, Boolean> response = new HashMap<>();
        final String targetType = alarmRecord.getAdditionalInformation().get(SOURCE_TYPE);
        if (!ProcessedEventSeverity.CLEARED.equals(presentSeverity)) {
            alarmRecord.setPresentSeverity(ProcessedEventSeverity.WARNING);
        }
        boolean alarmSyncSupported = true;
        try {
            alarmSyncSupported = modelCapabilities.isAlarmSyncSupportedByNode(targetType);
        } catch (final Exception exception) {
            LOGGER.error("Exception thrown while retrieving alarm sync suported: {}, defaulting to true", exception.getMessage());
            LOGGER.debug("Exception thrown while retrieving alarm sync suported: , defaulting to true", exception);
            alarmSyncSupported = true;
        }
        if (!alarmSyncSupported) {
            LOGGER.info("Alarm OutOfSync setting NOT SUPPORTED by fdn:{}", fdn);
            apsInstrumentedBean.incrementDiscardedAlarmCount(presentSeverity);
            response.put(ALARM_TO_BE_PROCESSED, false);
            return response;
        }
        final String currentState = (String) fmFunctionMoService.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE);
        if (currentState == null) {
            LOGGER.error(
                    "currentServiceState value is null for fdn {}.As a result currentServiceState will not be updated and alarm will be discarded",
                    fdn);
            LOGGER.debug(
                    "currentServiceState value is null for fdn {}.As a result currentServiceState will not be updated and alarm {} will be discarded",
                    fdn,alarmRecord);
            apsInstrumentedBean.incrementDiscardedAlarmCount(presentSeverity);
            response.put(ALARM_TO_BE_PROCESSED, false);
            return response;
        }
        if (FmSyncStatus100.OUT_OF_SYNC.name().equals(currentState)) {
            if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())) {
                fmFunctionMoService.updateCurrentServiceState(fdn, FmSyncStatus100.IN_SERVICE.name());
                systemRecorder.recordEvent("APS", EventLevel.DETAILED, "currentServiceState", "is changed to IN_SERVICE from OUT_OF_SYNC for fdn",
                        fdn);
            } else {
                LOGGER.info("OUT_OF_SYNC has been already raised for fdn {}.Replacing existing Out_Of_Sync alarm with the latest alarm ", fdn);
            }
            response.put(ALARM_TO_BE_PROCESSED, true);
            return response;
        } else if (FmSyncStatus100.IN_SERVICE.name().equals(currentState)) {
            fmFunctionMoService.updateCurrentServiceState(fdn, FmSyncStatus100.OUT_OF_SYNC.name());
            systemRecorder.recordEvent(APS, EventLevel.DETAILED, CURRENT_SERVICE_STATE,
                    "is changed to OUT_OF_SYNC from IN_SERVICE for NetworkElement {} ", fdn);
            if (fmSupervisionMoReader.readSupervisionAndAutoSyncAttributes(fdn).get(AUTOMATIC_SYNCHRONIZATION)) {
                response.put(INITIATE_SYNC, true);
            }
            response.put(ALARM_TO_BE_PROCESSED, true);
            return response;
        } else {
            LOGGER.warn("OUT_OF_SYNC Not processed due to service state for NetworkElement {} ", fdn);
        }
        response.put(ALARM_TO_BE_PROCESSED, false);
        return response;
    }

    /**
     * Method updates currentServiceState value to IN_SERVICE if active
     * attribute is true SUPERVISION_SWITCHOVER notification is received from
     * mediation during failover.
     *
     * @param fdn
     *            node FDN.
     */
    public void updateForSupervisionSwitchOver(final String fdn) {
        final String currentState = (String) fmFunctionMoService.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE);
        fmFunctionMoService.updateCurrentServiceState(fdn, FmSyncStatus100.IN_SERVICE.name());
        if (FmSyncStatus100.HEART_BEAT_FAILURE.name().equals(currentState)) {
            systemRecorder.recordEvent(APS, EventLevel.DETAILED, CURRENT_SERVICE_STATE, "is changed to IN_SERVICE from HEART_BEAT_FAILURE for fdn",
                    fdn);
        } else if (FmSyncStatus100.OUT_OF_SYNC.name().equals(currentState)) {
            systemRecorder.recordEvent(APS, EventLevel.DETAILED, CURRENT_SERVICE_STATE, "is changed to IN_SERVICE from OUT_OF_SYNC for fdn", fdn);
        } else if (FmSyncStatus100.NODE_SUSPENDED.name().equals(currentState)) {
            systemRecorder.recordEvent(APS, EventLevel.DETAILED, CURRENT_SERVICE_STATE, "is changed to IN_SERVICE from NODE_SUSPENDED for fdn", fdn);
        }
    }

    public boolean checkCsstateUpdateRequired(final ProcessedAlarmEvent alarmRecord) {
        final String currentState = (String) fmFunctionMoService.read(alarmRecord.getFdn(), FM_SUPERVISEDOBJECT_SERVICE_STATE);
        if (FmSyncStatus100.HEART_BEAT_FAILURE.name().equals(currentState)) {
            if (!AlarmSeverity.CLEARED.name().equals(alarmRecord.getPresentSeverity().name())) {
                LOGGER.debug("CurrentServiceState is HB and incoming alarm is not CLEARED {}", alarmRecord);
                return true;
            } else {
                final Long matchedHbAlarmCount = alarmReader.getMatchedHbAlarms(alarmRecord);
                LOGGER.info("matched active HB alarms on the node {} is {}", alarmRecord.getFdn(), matchedHbAlarmCount);
                if (matchedHbAlarmCount != 0) {
                    LOGGER.info("invoking AxeBladeClusterHbClearTimerInvoker for {}", alarmRecord.getObjectOfReference());
                    axeBladeClusterHbClearTimerInvoker.changeCurrentServiceStateByTimer(alarmRecord);
                    return true;
                }
            }
        }
        LOGGER.info("checkCsStateUpdateRequired false {} oor {}", alarmRecord.getFdn(), alarmRecord.getObjectOfReference());
        return false;
    }

    /**
     * Method that retreives capability value to differentiate whether
     * multiLevel handling is required or not.
     *
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent} --
     * @return boolean
     */
    public boolean getMultilevelCapabilityValue(final ProcessedAlarmEvent alarmRecord) {
    	LOGGER.info("getMultilevelCapabilityValue source type: {}", alarmRecord.getAdditionalInformation().get(SOURCE_TYPE));
        return modelCapabilities.isMultiLevelCssHandlingRequired(CATEGORY, alarmRecord.getAdditionalInformation().get(SOURCE_TYPE));
    }

    public void sendHearbeatStateRequest(final String state, final String fdn) {
    	sendHearbeatStateRequest(state, fdn, null);
    }
    public void sendHearbeatStateRequest(final String state, final String fdn, String oor) {
    	LOGGER.info("sending sendHearbeatStateRequest with oor");
        final FmMediationHeartBeatRequest fmMediationHeartBeatRequest = new FmMediationHeartBeatRequest();
        final long currentTimeMsec = System.currentTimeMillis();
        fmMediationHeartBeatRequest.setJobId(HEARTBEATUPDATEREQUEST.concat(Long.toString(currentTimeMsec)));
        fmMediationHeartBeatRequest.setProtocolInfo(OperationType.FM.toString());
        fmMediationHeartBeatRequest.setNodeAddress(fdn.concat(FMALARM_SUPERVISION_MO_SUFFIX));
        if (state.equals(FAILURE)) {
            fmMediationHeartBeatRequest.setState(FAILURE);
        } else {
            fmMediationHeartBeatRequest.setState(CLEARED);
        }
        fmMediationHeartBeatRequest.setClientType(MediationClientType.EVENT_BASED.name());
        if(oor != null) {
        	fmMediationHeartBeatRequest.setOor(oor);
        	LOGGER.info("oor {} current",oor);
        }
        try {
            mediationTaskSender.send(fmMediationHeartBeatRequest);
        } catch (final Exception exception) {
            LOGGER.error("Exception while sending hertbeat state request for fdn {} is {} ", fdn, exception);
        }
    }

    public void sendingMTRWithOOR(ProcessedAlarmEvent alarmRecord, String fdn,String failureOrClered) {
    	if(getMultilevelCapabilityValue (alarmRecord)) {
	        final String oor = alarmRecord.getObjectOfReference();
	        LOGGER.debug("sending MTR with oor {}",oor);
            sendHearbeatStateRequest(failureOrClered, fdn,oor);    
        } else {
            sendHearbeatStateRequest(failureOrClered, fdn);
        }
    }
    /**
     * Method updates current service state of to Heart_Beat_Failure.
     */
    private void updateHeartBeatStateToInService(final ProcessedAlarmEvent alarmRecord, final Map<String, Boolean> response) {
        final String fdn = alarmRecord.getFdn();
        final Map<String, Boolean> supervisionAndAutoSyncAttributes = fmSupervisionMoReader.readSupervisionAndAutoSyncAttributes(fdn);
        if (supervisionAndAutoSyncAttributes.get(ALARM_SUPERVISION_STATE)) {
            fmFunctionMoService.updateCurrentServiceState(fdn, FmSyncStatus100.IN_SERVICE.name());
            systemRecorder.recordEvent(APS, EventLevel.DETAILED, CURRENT_SERVICE_STATE, "is changed to IN_SERVICE from HEART_BEAT_FAILURE for fdn",
                    fdn);
            sendHearbeatStateRequest(CLEARED, fdn);
            final boolean autoSync = supervisionAndAutoSyncAttributes.get(AUTOMATIC_SYNCHRONIZATION);
            if (autoSync && !FMProcessedEventType.HB_FAILURE_NO_SYNCH.equals(alarmRecord.getRecordType())) {
                response.put(INITIATE_SYNC, true);
            }
        } else {
            LOGGER.info("Received HeartBeat Clear alarm when alarm supervision is disabled for fdn {}", fdn);
        }
    }

    /**
     * Method updates current service state of to Node_Suspended.
     */
    private void updateNodeSuspendedStateToInService(final ProcessedAlarmEvent alarmRecord, final Map<String, Boolean> response) {
        final String fdn = alarmRecord.getFdn();
        final Map<String, Boolean> supervisionAndAutoSyncAttributes = fmSupervisionMoReader.readSupervisionAndAutoSyncAttributes(fdn);
        if (supervisionAndAutoSyncAttributes.get(ALARM_SUPERVISION_STATE)) {
            fmFunctionMoService.updateCurrentServiceState(fdn, FmSyncStatus100.IN_SERVICE.name());
            systemRecorder.recordEvent(APS, EventLevel.DETAILED, CURRENT_SERVICE_STATE, "is changed to IN_SERVICE from NODE_SUSPENDED for fdn", fdn);
            if (supervisionAndAutoSyncAttributes.get(AUTOMATIC_SYNCHRONIZATION)) {
                response.put(INITIATE_SYNC, true);
            }
        } else {
            LOGGER.info("Received NodeSuspended Clear alarm when supervision is disabled for fdn {}", fdn);
        }
    }
}

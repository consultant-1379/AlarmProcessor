/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.eventhandlers;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.OSSPREFIX_NOT_SET;
import static com.ericsson.oss.services.fm.common.util.AlarmAttributeDataPopulate.populateLimitedAlarmAttributes;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.ClearAllAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.ClearListAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.ErrorAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.HeartBeatAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.HeartBeatFailureNoSyncHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.NodeSuspendAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.NonSyncAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.NormalAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.OutOfSyncAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.RepeatedAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.RepeatedErrorAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.RepeatedNonSyncAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.SupervisionSwitchoverHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.SuppressedAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.SyncAbortAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.SyncAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.SyncEndAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.SyncStartAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.TechnicianPresentAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.UpdateAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.OssPrefixHolder;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Bean responsible to redirect alarm to the appropriate handler based on Record Type attribute of the alarm .It provides transaction context required
 * for accessing DPS in alarm processing.
 * <p>
 * Alarm will not be processed if objectOfReference attribute is empty or null. Alarm will be discarded if the corresponding NetworkElement is not
 * present in Database.
 */
@Stateless
public class AlarmHandlerBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmHandlerBean.class);

    @Resource
    private EJBContext context;

    @Inject
    private OssPrefixHolder ossPrefixHolder;

    @Inject
    private TechnicianPresentAlarmHandler technicianPresentAlarmHandler;

    @Inject
    private SyncStartAlarmHandler syncStartAlarmHandler;

    @Inject
    private SyncEndAlarmHandler syncEndAlarmHandler;

    @Inject
    private SyncAlarmHandler syncAlarmHandler;

    @Inject
    private SyncAbortAlarmHandler syncAbortAlarmHandler;

    @Inject
    private SuppressedAlarmHandler suppressedAlarmHandler;

    @Inject
    private RepeatedNonSyncAlarmHandler repeatedNonSyncAlarmHandler;

    @Inject
    private RepeatedErrorAlarmHandler repeatedErrorAlarmHandler;

    @Inject
    private RepeatedAlarmHandler repeatedAlarmHandler;

    @Inject
    private OutOfSyncAlarmHandler outOfSyncAlarmHandler;

    @Inject
    private NormalAlarmHandler normalAlarmHandler;

    @Inject
    private NonSyncAlarmHandler nonSyncAlarmHandler;

    @Inject
    private NodeSuspendAlarmHandler nodeSuspendAlarmHandler;

    @Inject
    private HeartBeatFailureNoSyncHandler heartBeatFailureNoSyncHandler;

    @Inject
    private HeartBeatAlarmHandler heartBeatAlarmHandler;

    @Inject
    private ErrorAlarmHandler errorAlarmHandler;

    @Inject
    private ClearListAlarmHandler clearListAlarmHandler;

    @Inject
    private UpdateAlarmHandler updateAlarmHandler;

    @Inject
    private ClearAllAlarmHandler clearAllAlarmHandler;

    @Inject
    private SupervisionSwitchoverHandler supervisionSwitchoverHandler;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private AlarmValidator alarmValidator;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    /**
     * Performs the core logic of alarm processing : CRUD operations in the database, Sends processed alarm to its destination, Initiates alarm
     * synchronization if required. All these operations are performed in a single transaction to achieve data consistency . If these are performed in
     * separate transactions there could be a chance for some inconsistency in alarm information. For Eg: Assume DPS operations and sending events to
     * JMS is performed in separate transactions. DPS transaction committed and there was some exception with JMS and Runtime Exception will be thrown
     * and message will be held in the queue itself. Later when the message is re-delivered after, alarm will be already in database and it will now
     * be considered as a repeated alarm and sent to NorthBound . It is not a repeated alarm from Node perspective but treated as repeated alarm by
     * OSS.
     * @param ProcessedAlarmEvent
     *            alarmRecord
     * @return AlarmProcessingResponse
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public AlarmProcessingResponse processAlarm(final ProcessedAlarmEvent alarmRecord) {
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        try {
            final boolean networkElementExists = alarmValidator.isNetworkElementExists(alarmRecord.getFdn());
            if (networkElementExists || FMProcessedEventType.CLEAR_LIST.equals(alarmRecord.getRecordType())) {
                checkOssPrefixIsNotSet(alarmRecord);
                alarmProcessingResponse = handleAlarmBasedOnRecordType(alarmRecord);
            } else {
                systemRecorder.recordEvent("APS", EventLevel.DETAILED, "FM",
                        "Discarding alarm as corresponding ManagedObject does not exists in Database with fdn ", alarmRecord.getFdn());
                LOGGER.debug("Discarding alarm : {} as corresponding ManagedObject does not exists in Database ",alarmRecord);
                apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
            }
        } catch (final NullPointerException exception) {
            LOGGER.error("NullPointerException occured while processing alarmRecord {} and exception details:{}",
                    populateLimitedAlarmAttributes(alarmRecord), exception.getMessage());
            context.setRollbackOnly();
            alarmProcessingResponse.getProcessedAlarms().clear();
            alarmProcessingResponse.setRetryFlag(true);
        } catch (final Exception exception) {
            LOGGER.error("Exception in processing alarmRecord : {} is : {}", populateLimitedAlarmAttributes(alarmRecord), exception.getMessage());
            LOGGER.debug("Exception in processing alarmRecord : {} is : {}", alarmRecord, exception);
            context.setRollbackOnly();
            alarmProcessingResponse.getProcessedAlarms().clear();
            alarmProcessingResponse.setRetryFlag(true);
        }
        return alarmProcessingResponse;
    }

    /**
     * Checks additionalAttributes for a specific entry to decide whether ObjectOfReference needs to be changed to include OssPrefix. <br>
     * <p>
     * This case will be triggered only when Mediation is unaware of whether OssPrefix is set or not.
     * @param ProcessedAlarmEvent
     *            alarmRecord
     */
    private void checkOssPrefixIsNotSet(final ProcessedAlarmEvent alarmRecord) {
        if (alarmRecord.getAdditionalInformation().containsKey(OSSPREFIX_NOT_SET)) {
            // This implies OSSPrefix is not set.
            final String ossPrefix = ossPrefixHolder.getOssPrefix(alarmRecord.getFdn());
                if (ossPrefix!=null &&!alarmRecord.getObjectOfReference().contains(ossPrefix)) {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(ossPrefix);
                    stringBuilder.append(",");
                    stringBuilder.append(alarmRecord.getObjectOfReference());
                    alarmRecord.setObjectOfReference(stringBuilder.toString());
                }
            // Remove it as it is not required anymore.
            alarmRecord.getAdditionalInformation().remove(OSSPREFIX_NOT_SET);
            LOGGER.debug("objectOfReference is changed for {}", alarmRecord);
        }
    }

    /**
     * Invoke corresponding handler based on recordType of the alarm.
     * @param {@link
     *            ProcessedAlarmEvent} alarmRecord
     * @return {@link AlarmProcessingResponse} toBeSent
     */
    private AlarmProcessingResponse handleAlarmBasedOnRecordType(final ProcessedAlarmEvent alarmRecord) {
        AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        switch (alarmRecord.getRecordType()) {
            case ALARM:
                alarmProcessingResponse = normalAlarmHandler.handleAlarm(alarmRecord);
                break;
            case TECHNICIAN_PRESENT:
                alarmProcessingResponse = technicianPresentAlarmHandler.handleAlarm(alarmRecord);
                break;
            case SYNCHRONIZATION_STARTED:
                alarmProcessingResponse = syncStartAlarmHandler.handleAlarm(alarmRecord);
                break;
            case SYNCHRONIZATION_ENDED:
                alarmProcessingResponse = syncEndAlarmHandler.handleAlarm(alarmRecord);
                break;
            case SYNCHRONIZATION_ALARM:
                alarmProcessingResponse = syncAlarmHandler.handleAlarm(alarmRecord);
                break;
            case SYNCHRONIZATION_ABORTED:
                alarmProcessingResponse = syncAbortAlarmHandler.handleAlarm(alarmRecord);
                break;
            case ALARM_SUPPRESSED_ALARM:
                alarmProcessingResponse = suppressedAlarmHandler.handleAlarm(alarmRecord);
                break;
            case REPEATED_NON_SYNCHABLE:
                alarmProcessingResponse = repeatedNonSyncAlarmHandler.handleAlarm(alarmRecord);
                break;
            case REPEATED_ALARM:
                alarmProcessingResponse = repeatedAlarmHandler.handleAlarm(alarmRecord);
                break;
            case NON_SYNCHABLE_ALARM:
                alarmProcessingResponse = nonSyncAlarmHandler.handleAlarm(alarmRecord);
                break;
            case HEARTBEAT_ALARM:
                alarmProcessingResponse = heartBeatAlarmHandler.handleAlarm(alarmRecord);
                break;
            case HB_FAILURE_NO_SYNCH:
                alarmProcessingResponse = heartBeatFailureNoSyncHandler.handleAlarm(alarmRecord);
                break;
            case NODE_SUSPENDED:
                alarmProcessingResponse = nodeSuspendAlarmHandler.handleAlarm(alarmRecord);
                break;
            case OUT_OF_SYNC:
                alarmProcessingResponse = outOfSyncAlarmHandler.handleAlarm(alarmRecord);
                break;
            case ERROR_MESSAGE:
                alarmProcessingResponse = errorAlarmHandler.handleAlarm(alarmRecord);
                break;
            case REPEATED_ERROR_MESSAGE:
                alarmProcessingResponse = repeatedErrorAlarmHandler.handleAlarm(alarmRecord);
                break;
            case CLEAR_LIST:
                alarmProcessingResponse = clearListAlarmHandler.handleAlarm(alarmRecord);
                break;
            case CLEARALL:
                alarmProcessingResponse = clearAllAlarmHandler.handleAlarm(alarmRecord);
                break;
            case UPDATE:
                alarmProcessingResponse = updateAlarmHandler.handleAlarm(alarmRecord);
                break;
            case SUPERVISION_SWITCHOVER:
                alarmProcessingResponse = supervisionSwitchoverHandler.handleAlarm(alarmRecord);
                break;
            default:
                break;
        }
        return alarmProcessingResponse;
    }
}

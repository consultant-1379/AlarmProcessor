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

package com.ericsson.oss.services.fm.alarmprocessor.builders;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.DATE_FORMAT;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.DISCARD_SYNC_PROBABALE_CAUSE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.EventTypeConstants.COMMUNICATIONS_ALARM;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.COMMA_DELIMITER;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.util.NodeRef;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

/**
 * Class builds the Sync Abort Alarm notifications.
 */
public class SyncAbortNotificationBuilder {

    /**
     * Method builds the Sync Abort notifications.
     * @param {@link NodeRef} nodeRef
     * @return list of {@link EventNotification}s
     */
    public List<EventNotification> build(final NodeRef nodeRef) {
        final List<EventNotification> syncAbortnotifications = new ArrayList<EventNotification>();
        syncAbortnotifications.add(buildAbortSyncNotification(nodeRef));
        return syncAbortnotifications;
    }

    private EventNotification buildAbortSyncNotification(final NodeRef nodeRef) {
        final EventNotification syncAbortNotification = new EventNotification();
        String nodeFdn = nodeRef.getNodeFdn();
        nodeFdn = nodeFdn.substring(0, nodeFdn.indexOf(COMMA_DELIMITER));
        syncAbortNotification.addAdditionalAttribute(FDN, nodeFdn);
        syncAbortNotification.setManagedObjectInstance(nodeFdn);
        syncAbortNotification.setPerceivedSeverity(ProcessedEventSeverity.INDETERMINATE.name());
        syncAbortNotification.setEventType(COMMUNICATIONS_ALARM);
        syncAbortNotification.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ABORTED.name());
        syncAbortNotification.setProbableCause(DISCARD_SYNC_PROBABALE_CAUSE);
        syncAbortNotification.setSpecificProblem(AlarmProcessorConstants.SP_SYNCABORTED);
        syncAbortNotification.setEventTime(new SimpleDateFormat(DATE_FORMAT).format(new Date()));
        return syncAbortNotification;
    }
}
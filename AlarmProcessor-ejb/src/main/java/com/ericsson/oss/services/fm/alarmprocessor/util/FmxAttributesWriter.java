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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_PROCESSED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NORMAL_PROC;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.POST_PROC;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

/**
 * Class which updates ProcessedAlarmEvent with values related to FMX.
 */
public class FmxAttributesWriter {

    /**
     * Method which sets DiscriminatorList, ProcessingType, Visibility, FmxGenerated, CorrelatedVisibility in ProcessedAlarmEvent after enrichment.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link EventNotification} eventNotification
     */
    public void setEnrichmentValues(final ProcessedAlarmEvent alarmRecord, final EventNotification eventNotification) {
        alarmRecord.setDiscriminatorList(eventNotification.getDiscriminatorList());
        alarmRecord.setProcessingType(eventNotification.getProcessingType());
        alarmRecord.setVisibility(eventNotification.isVisibility());
        alarmRecord.setFmxGenerated(eventNotification.getFmxGenerated());
        alarmRecord.setCorrelatedVisibility(!(eventNotification.isVisibility()));
    }

    /**
     * Update the Visibility and CorrelatedVisibility for FMX Matching CLEAR alarm(Node Clear).
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @param {@link ProcessedAlarmEvent} correlatedAlarm
     */
    public void updateVisibilityForNodeClearAlarm(final ProcessedAlarmEvent alarmRecord, final ProcessedAlarmEvent correlatedAlarm) {
        if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())
                && (NORMAL_PROC.equals(alarmRecord.getProcessingType()) || POST_PROC.equals(alarmRecord.getProcessingType()))
                && !(FMX_PROCESSED.equals(alarmRecord.getFmxGenerated()))) {
            alarmRecord.setVisibility(correlatedAlarm.getVisibility());
            alarmRecord.setCorrelatedVisibility(correlatedAlarm.getCorrelatedVisibility());
        }
    }
}

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

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import javax.inject.Inject;

import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class handles alarms with record type HB_FAILURE_NO_SYNCH.
 */
public class HeartBeatFailureNoSyncHandler {

    @Inject
    private HeartBeatCommonHandler heartBeatCommonHandler;

    /**
     * Method handles alarms with record type HB_FAILURE_NO_SYNCH when there is a correlated alarm and when there is no correlated alarm in db.
     * @param {@link ProcessedAlarmEvent} --alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        return heartBeatCommonHandler.handleAlarm(alarmRecord);
    }
}
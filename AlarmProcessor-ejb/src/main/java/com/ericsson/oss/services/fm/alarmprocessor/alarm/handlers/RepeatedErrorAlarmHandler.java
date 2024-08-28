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
 * Class handles Alarms with RecordType REPEATED_ERROR_MESSAGE.
 */
public class RepeatedErrorAlarmHandler {

    @Inject
    private ErrorAlarmCommonHandler errorAlarmCommonHandler;

    /**
     * Method handles alarms with RecordType REPEATED_ERROR_MESSAGE . Clear Alarm for REPEATED_ERROR_MESSAGE will not be processed as these are State
     * less Events. Alarm Correlation is performed for the received event . If Correlated event is not found the event is inserted into database else
     * it is updated.
     * @param {@link ProcessedAlarmEvent}--alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        return errorAlarmCommonHandler.handleAlarm(alarmRecord);
    }
}
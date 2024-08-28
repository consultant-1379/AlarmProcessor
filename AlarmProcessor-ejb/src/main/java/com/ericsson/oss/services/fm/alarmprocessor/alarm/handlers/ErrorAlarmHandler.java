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

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import javax.inject.Inject;

import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class to handle Alarms with RecordType ERROR_MESSAGE.
 */
public class ErrorAlarmHandler {

    @Inject
    private ErrorAlarmCommonHandler errorAlarmCommonHandler;

    /**
     * Method to handle alarms with RecordType ERROR_MESSAGE. Clear Alarm for ERROR_MESSAGE will not be processed as these are State less Events.
     * Alarm Correlation is performed for the received event . If Correlated event is not found the event is inserted into database else it is
     * updated.
     * @param {@link ProcessedAlarmEvent}--alarmRecord
     * @return {@link AlarmProcessingResponse}--alarmProcessingResponse
     */
    public AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord) {
        return errorAlarmCommonHandler.handleAlarm(alarmRecord);
    }
}
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

import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * An interface which is implemented by all alarm handlers. TO DO Use this interface later when issue with Qualifiers is resolved.
 */
public interface AlarmHandler {

    /**
     * A contract method for handling alarms which is implemented by all alarm handlers.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @return {@link AlarmProcessingResponse}
     */
    AlarmProcessingResponse handleAlarm(final ProcessedAlarmEvent alarmRecord);
}
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

package com.ericsson.oss.services.fm.alarmprocessor.qualifiers;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Class for alarm Record Type @Qualifier.
 */
@Retention(RUNTIME)
@Target({ TYPE, FIELD })
@Qualifier
public @interface AlarmHandlerQualifier {

    /**
     * Class that deals with alarms record types.
     */
    enum RecordTypes {
        UNDEFINED, ALARM, ERROR_MESSAGE, NON_SYNCHABLE_ALARM, REPEATED_ALARM, SYNCHRONIZATION_ALARM, HEARTBEAT_ALARM, SYNCHRONIZATION_STARTED,
        SYNCHRONIZATION_ENDED, SYNCHRONIZATION_ABORTED, SYNCHRONIZATION_IGNORED, CLEAR_LIST, REPEATED_ERROR_MESSAGE, REPEATED_NON_SYNCHABLE, UPDATE,
        NODE_SUSPENDED, HB_FAILURE_NO_SYNCH, SYNC_NETWORK, TECHNICIAN_PRESENT, ALARM_SUPPRESSED_ALARM, OSCILLATORY_HB_ALARM, UNKNOWN_RECORD_TYPE,
        CLEARALL, OUT_OF_SYNC, NO_SYNCHABLE_ALARM
    }

    RecordTypes value();
}

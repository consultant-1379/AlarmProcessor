/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.orphanclear;

/**
 * Pending Clear Alarm Exception class.
 */
public class PendingClearAlarmException extends Exception {
    private static final long serialVersionUID = -7843889350374467803L;

    /**
     * PendingClearAlarmException.
     * @param string the exception string.
     */
    public PendingClearAlarmException(final String string) {
        super(string);
    }
}

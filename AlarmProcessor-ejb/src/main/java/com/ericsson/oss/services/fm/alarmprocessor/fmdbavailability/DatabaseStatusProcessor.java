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

package com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

/**
 * Singleton which holds the status of Database failure status.
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class DatabaseStatusProcessor {

    @Inject
    private SystemRecorder systemRecorder;

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseStatusProcessor.class);

    private boolean isDatabaseAvailable = true;

    public synchronized boolean isDatabaseAvailable() {
        LOGGER.info("isDatabaseAvailable value is: {} ", isDatabaseAvailable);
        return isDatabaseAvailable;
    }

    public void updateDatabaseStatusToFailed() {
        synchronized (this) {
            isDatabaseAvailable = false;
            LOGGER.warn("Updated the database status to Failed and current value of isDatabaseAvailable is: {} ", isDatabaseAvailable);
        }
    }

    public void updateDatabaseStatusToAvailable() {
        synchronized (this) {
            isDatabaseAvailable = true;
            systemRecorder.recordEvent("APS", EventLevel.DETAILED, "FM", "Updated the database status to available. ", String.valueOf(true));
        }
    }

}
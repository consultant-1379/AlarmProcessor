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

package com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.availability.DpsAvailabilityCallback;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

/**
 * Callback service to handle updates in DPS service availability.
 */
@Singleton
public class DpsAvailabilityCallBackImpl implements DpsAvailabilityCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DpsAvailabilityCallBackImpl.class);
    private final String callbackName = "DPS_Callback_From_APS";

    @Inject
    private DatabaseStatusProcessor databaseStatusProcessor;

    @Inject
    private SystemRecorder systemRecorder;

    public DpsAvailabilityCallBackImpl() {
    }

    @Override
    public String getCallbackName() {
        return callbackName;
    }

    @Override
    public void onServiceAvailable() {
        systemRecorder.recordEvent("APS", EventLevel.DETAILED, "FM", "Invoked CALLED_ON_SERVICE_AVAILABLE as the DB is Available again...! ",
                String.valueOf(true));
        databaseStatusProcessor.updateDatabaseStatusToAvailable();
    }

    @Override
    public void onServiceUnavailable() {
        LOGGER.warn("Invoked CALLED_ON_SERVICE_UNAVAILABLE as the DB is not available...!");
        databaseStatusProcessor.updateDatabaseStatusToFailed();
    }
}

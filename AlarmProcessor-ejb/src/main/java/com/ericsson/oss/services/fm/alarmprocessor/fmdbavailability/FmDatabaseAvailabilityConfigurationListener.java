/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.modeling.annotation.constraints.NotNull;
import com.ericsson.oss.itpf.sdk.config.annotation.ConfigurationChangeNotification;
import com.ericsson.oss.itpf.sdk.config.annotation.Configured;

/**
 * Class listens for changes in Configurable parameters like handleDBFailure.
 */
@ApplicationScoped
public class FmDatabaseAvailabilityConfigurationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FmDatabaseAvailabilityConfigurationListener.class);

    @Inject
    @NotNull
    @Configured(propertyName = "handleDBFailure")
    private Boolean handleDataBaseFailure;

    public void listenForForwardAlarmsChanges(@Observes @ConfigurationChangeNotification(propertyName = "handleDBFailure") final Boolean newValue) {
        LOGGER.warn(
                "Boolean used to check whether to send raw alarms to NBI in database unavailable case is modified, new value of forwardAlarms is: {}",
                newValue);
        handleDataBaseFailure = newValue;
    }

    public Boolean getHandleDbFailure() {
        return handleDataBaseFailure;
    }

}

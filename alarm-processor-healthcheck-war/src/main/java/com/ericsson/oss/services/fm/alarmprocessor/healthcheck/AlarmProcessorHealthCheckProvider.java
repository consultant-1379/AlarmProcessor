/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.healthcheck;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.microhealthcheck.DefaultHealthCheckProvider;
import com.ericsson.oss.itpf.sdk.microhealthcheck.DefaultHealthCheckResponseBuilder;
import com.ericsson.oss.itpf.sdk.microhealthcheck.HealthCheckResponse;
import com.ericsson.oss.services.fm.alarmprocessor.cluster.AlarmProcessorHealthCheck;

@Path(DefaultHealthCheckProvider.HEALTHCHECK_URI)
@ApplicationScoped
public class AlarmProcessorHealthCheckProvider extends DefaultHealthCheckProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmProcessorHealthCheckProvider.class);

    @Inject AlarmProcessorHealthCheck alarmProcessorHealthCheck;

    @Override
    public HealthCheckResponse getAppHealthStatus() {
        LOGGER.trace("HC invoked for Alarm Processor");
        //Determine the healthiness of the services here
        final Boolean hCheck = alarmProcessorHealthCheck.checkHealthState();
        HealthCheckResponse healthReport = null;
        LOGGER.trace("HC result is: {} for Alarm Processor", hCheck);
        if(hCheck) {
            healthReport = new DefaultHealthCheckResponseBuilder()
                    .appName("alarm-processor")
                    .up()
                    .build();
        } else {
            healthReport = new DefaultHealthCheckResponseBuilder()
                    .appName("alarm-processor")
                    .down()
                    .withData("status", "cluster joining")
                    .build();
        }
        return healthReport;
    }
}

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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static javax.ws.rs.core.Response.Status.OK;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.APS_SERVICE_ID;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FAILURE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.HOST_KEY;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.INTERNAL_ALARM_SERVICE_LABEL;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.URL_FOR_INTERNAL_ALARM_SERVICE;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.ADDITIONAL_TEXT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.EVENT_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OBJECT_OF_REFERENCE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBABLE_CAUSE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.SPECIFIC_PROBLEM;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.internalalarm.api.InternalAlarmRequest;

/**
 * This class generates FM internal alarm
 */
@ApplicationScoped
public class InternalAlarmGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalAlarmGenerator.class);

    @Inject
    protected SystemRecorder systemRecorder;

    /**
     * This method raise an FM internal alarm.
     *
     * @param alarmDetails
     *            Map containing alarm details need to be generated
     */
    public void raiseInternalAlarm(final Map<String, Object> alarmDetails) {
        try {
            final InternalAlarmRequest internalAlarmRequest = generateRequestObject(alarmDetails);
            final ClientRequest request = new ClientRequest(URL_FOR_INTERNAL_ALARM_SERVICE);
            request.header(HOST_KEY, INTERNAL_ALARM_SERVICE_LABEL);
            request.accept(MediaType.APPLICATION_JSON);
            final String jsonRequest = getJsonString(internalAlarmRequest);
            request.body(MediaType.APPLICATION_JSON, jsonRequest);
            LOGGER.debug("json request which is sent to Alarmendpoint {}", jsonRequest);
            LOGGER.info("Raising an internal alarm for {} ", (String) alarmDetails.get(APS_SERVICE_ID));
            final ClientResponse<String> response = request.post(String.class);
            manageResponse(response, (String) alarmDetails.get(APS_SERVICE_ID));
        } catch (final Exception exception) {
            systemRecorder.recordError(FAILURE, ErrorSeverity.NOTICE, (String) alarmDetails.get(APS_SERVICE_ID),
                                       (String) alarmDetails.get(PROBABLE_CAUSE), (String) alarmDetails.get(SPECIFIC_PROBLEM));
            LOGGER.error("Failed to raise internal alarm {}", exception.getMessage());
        }
    }

    private InternalAlarmRequest generateRequestObject(final Map<String, Object> alarmDetails) {
        final InternalAlarmRequest internalAlarmRequest = new InternalAlarmRequest();
        internalAlarmRequest.setEventType((String) alarmDetails.get(EVENT_TYPE));
        internalAlarmRequest.setProbableCause((String) alarmDetails.get(PROBABLE_CAUSE));
        internalAlarmRequest.setSpecificProblem((String) alarmDetails.get(SPECIFIC_PROBLEM));
        internalAlarmRequest.setPerceivedSeverity((String) alarmDetails.get(PRESENT_SEVERITY));
        internalAlarmRequest.setRecordType((String) alarmDetails.get(RECORD_TYPE));
        internalAlarmRequest.setManagedObjectInstance((String) alarmDetails.get(OBJECT_OF_REFERENCE));
        final Map<String, String> additionalAttributes = new HashMap<>();
        additionalAttributes.put(FDN, (String) alarmDetails.get(FDN));
        if (alarmDetails.get(ADDITIONAL_TEXT) != null) {
            additionalAttributes.put(ADDITIONAL_TEXT, (String) alarmDetails.get(ADDITIONAL_TEXT));
        }
        internalAlarmRequest.setAdditionalAttributes(additionalAttributes);
        return internalAlarmRequest;
    }

    private String getJsonString(final InternalAlarmRequest internalAlarmRequest) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        String jsonRequest = null;
        jsonRequest = mapper.writeValueAsString(internalAlarmRequest);
        return jsonRequest;
    }

    private void manageResponse(final ClientResponse<String> resp, final String details) {
        if (resp.getStatus() == OK.getStatusCode()) {
            LOGGER.info("Alarm request processed successfully for {} ", details);
        } else {
            LOGGER.info("Alarm request failed for {} ", details);
        }
    }
}

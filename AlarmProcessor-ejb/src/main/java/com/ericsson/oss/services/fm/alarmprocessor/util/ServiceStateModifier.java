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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Class updates attribute value to true in FMFunctionMO in database against the given attribute.
 */
public class ServiceStateModifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceStateModifier.class);

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * Method updates record type and service state of fdn based on SP.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     */
    public void updateFmFunctionBasedOnSpecificProblem(final ProcessedAlarmEvent alarmRecord) {
        if (AlarmProcessorConstants.ALARMSUPPRESSED_SP.equals(alarmRecord.getSpecificProblem())) {
            alarmRecord.setRecordType(FMProcessedEventType.ALARM_SUPPRESSED_ALARM);
            updateAlarmSuppressedServiceState(alarmRecord.getFdn(), AlarmProcessorConstants.ALARM_SUPPRESSED_STATE);
        } else if (AlarmProcessorConstants.TECHNICIANPRESENT_SP.equals(alarmRecord.getSpecificProblem())) {
            alarmRecord.setRecordType(FMProcessedEventType.TECHNICIAN_PRESENT);
            updateTechnicianPresentServiceState(alarmRecord.getFdn(), AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE);
        }
    }

    /**
     * Updates TECHNICIAN_PRESENT_STATE of fdn to true.
     * @param String
     *            fdn
     * @param String
     *            attribute
     */
    public void updateTechnicianPresentServiceState(final String fdn, final String attribute) {
        updateFmFunctionServiceState(fdn, attribute);
    }

    /**
     * Updates ALARM_SUPPRESSED_STATE of fdn to true.
     * @param String
     *            fdn
     * @param String
     *            attribute
     */
    public void updateAlarmSuppressedServiceState(final String fdn, final String attribute) {
        updateFmFunctionServiceState(fdn, attribute);
    }

    /**
     * Method reads the attribute value from db and updates it to true against the attribute sent.
     * @param String
     *            fdn
     * @param String
     *            attribute
     */
    private void updateFmFunctionServiceState(final String fdn, final String attribute) {
        final Object attributeValue = fmFunctionMoService.read(fdn, attribute);
        LOGGER.debug("Value for the attribute {} received is : {} ", attribute, attributeValue);
        if (attributeValue != null && !(boolean) attributeValue) {
            fmFunctionMoService.update(fdn, attribute, true);
            systemRecorder.recordEvent("APS", EventLevel.DETAILED, "AlarmSuppressedState/TechnicianPresentState ", "is changed to true for fdn", fdn);
        }
    }
}
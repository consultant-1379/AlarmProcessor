/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FLAG_ONE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FLAG_ZERO;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ROUTE_TO_NMS;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;

import java.util.Map;

import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Indicates whether Fake Clear is to be sent to North Bound based on alarm attributes like RouteToNMS and visibility.
 *
 */
public final class FakeClearToNorthBoundUtility {

    private FakeClearToNorthBoundUtility() {
    }

    /**
     * Checks attributes like visibility,RouteToNMS in given alarm and existing alarm attributes in database to decide
     * whether fake clear is to be sent NorthBound or not.
     * @param alarmRecord
     *            Alarm received as part of FMX update.
     * @param existingAlarmAttributes
     *            Alarm attributes of correlated alarm present in the database.
     * @return boolean true if fake clear is to be sent to North Bound.
     */
    public static boolean checkIfFakeClearIsToBeSentToNorthBound(final ProcessedAlarmEvent alarmRecord,
            final Map<String, Object> existingAlarmAttributes) {
        boolean sendFakeClearToNorthBound = false;
        String routeToNmsFromAlarm = null;
        final Map<String, String> additionalAttributesFromAlarm = alarmRecord.getAdditionalInformation();
        if (additionalAttributesFromAlarm.containsKey(ROUTE_TO_NMS)) {
            routeToNmsFromAlarm = additionalAttributesFromAlarm.get(ROUTE_TO_NMS);
        }
        final boolean visibilityFromExistingAlarm = (boolean) existingAlarmAttributes.get(VISIBILITY);
        final String additionalAttributesStringFromExistingAlarm = (String) existingAlarmAttributes.get(ADDITIONAL_INFORMATION);
        final ProcessedAlarmEvent processedAlarmEvent = new ProcessedAlarmEvent();
        processedAlarmEvent.setAdditionalInformationToMap(additionalAttributesStringFromExistingAlarm);
        final Map<String, String> additionalAttributesFromExistingAlarm = processedAlarmEvent.getAdditionalInformation();
        final String routeToNmsFromExistingAlarm = additionalAttributesFromExistingAlarm.get(ROUTE_TO_NMS);

        if (visibilityFromExistingAlarm && FLAG_ZERO.equalsIgnoreCase(routeToNmsFromExistingAlarm)
                && FLAG_ZERO.equalsIgnoreCase(routeToNmsFromAlarm)) {
            //This specific case is to avoid sending fake clear for repeated updates from FMX.
            //visibility made true with RouteToNMS==0 initially and then again visibility false with RouteToNMS=0.
            sendFakeClearToNorthBound = false;
        } else if (visibilityFromExistingAlarm && FLAG_ONE.equalsIgnoreCase(routeToNmsFromExistingAlarm)
                && FLAG_ZERO.equalsIgnoreCase(routeToNmsFromAlarm)) {
            //Fake Clear to be sent upon changes for RouteToNMS attribute.
            sendFakeClearToNorthBound = true;
        } else if (visibilityFromExistingAlarm && FLAG_ZERO.equalsIgnoreCase(routeToNmsFromAlarm)) {
            //Fake Clear to be sent if visibility of alarm in database is true and incoming alarm has RouteToNMS=0.
            sendFakeClearToNorthBound = true;
        }
        return sendFakeClearToNorthBound;
    }

}

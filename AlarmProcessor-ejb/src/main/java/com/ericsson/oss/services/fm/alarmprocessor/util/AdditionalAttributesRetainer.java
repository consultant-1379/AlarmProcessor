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

import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

/**
 * Retains the attributes in additionalInformation attribute of an alarm if not present in incoming alarm.
 *
 */
public final class AdditionalAttributesRetainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalAttributesRetainer.class);

    private AdditionalAttributesRetainer() {
    }

    public static void retainAdditionalAttributesPresentInDatabase(final Map<String, String> additionalAttributesFromAlarm,
            final String additionalInfoFromAlarmInDatabase) {
        final ProcessedAlarmEvent alarmEvent = new ProcessedAlarmEvent();
        alarmEvent.setAdditionalInformationToMap(additionalInfoFromAlarmInDatabase);
        final Map<String, String> additionalAttributesFromAlarmInDatabase = alarmEvent.getAdditionalInformation();
        retainAdditionalAttributes(additionalAttributesFromAlarm, additionalAttributesFromAlarmInDatabase);
    }

    public static void retainAdditionalAttributesPresentInDatabase(final Map<String, String> additionalAttributesFromAlarm,
            final Map<String, String> additionalAttributesFromAlarmInDatabase) {
        retainAdditionalAttributes(additionalAttributesFromAlarm, additionalAttributesFromAlarmInDatabase);
    }

    private static void retainAdditionalAttributes(final Map<String, String> additionalAttributesFromAlarm,
            final Map<String, String> additionalAttributesFromAlarmInDatabase) {
        final Set<String> keysFromAlarm = additionalAttributesFromAlarm.keySet();
        final Set<String> keysFromAlarmInDatabase = additionalAttributesFromAlarmInDatabase.keySet();
        //Remove fm-fmxadaptor added attributes.
        removeFmInternalAttributes(keysFromAlarmInDatabase);
        keysFromAlarmInDatabase.removeAll(keysFromAlarm);
        LOGGER.trace("Attributes retained are:{} ", keysFromAlarmInDatabase);
        //Add retained attributes to additionalAttributesFromAlarm
        for (final String key : keysFromAlarmInDatabase) {
            final String value = additionalAttributesFromAlarmInDatabase.get(key);
            if (value != null) {
                additionalAttributesFromAlarm.put(key, value);
            }
        }
        LOGGER.trace("Additional attributes after retaining from alarm present in database is:{}", additionalAttributesFromAlarm);
    }

    private static void removeFmInternalAttributes(final Set<String> attributesFromAlarmInDatabase) {
        attributesFromAlarmInDatabase.remove(HIDE_OPERATION);
    }

}

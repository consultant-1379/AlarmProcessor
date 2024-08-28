/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.BACKUP_STATUS;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OSCILLATION_COUNT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.REPEAT_COUNT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AdditionalAttributesRetainerTest {

    private final Map<String,String> additionalAlarmAttributesInDatabase = new HashMap<String,String>();
    private final Map<String,String> additionalAttributesInAlarm = new HashMap<String,String>();
    private final String routeToNms = "RouteToNms";
    private final String hideOperation = "hideOperation";
    
    @Test
    public void retainAdditionalAttributesPresentInDatabaseTest() {
        initializeAlarmAttributesInMaps();
        AdditionalAttributesRetainer.retainAdditionalAttributesPresentInDatabase(additionalAttributesInAlarm, additionalAlarmAttributesInDatabase);
        assertTrue(additionalAttributesInAlarm.containsKey(routeToNms) && additionalAttributesInAlarm.get(routeToNms).equals("0"));
        assertTrue(additionalAttributesInAlarm.get(REPEAT_COUNT).equals("2"));
        assertNull(additionalAttributesInAlarm.get(hideOperation));
    }

    private void initializeAlarmAttributesInMaps() {
        additionalAlarmAttributesInDatabase.put(BACKUP_STATUS ,"false");
        additionalAlarmAttributesInDatabase.put(VISIBILITY ,"true");
        additionalAlarmAttributesInDatabase.put(routeToNms ,"0");
        additionalAlarmAttributesInDatabase.put(REPEAT_COUNT ,"1");
        additionalAlarmAttributesInDatabase.put(hideOperation, "false");
        additionalAttributesInAlarm.put(REPEAT_COUNT,"2");
        additionalAttributesInAlarm.put(OSCILLATION_COUNT,"2");
    }

}

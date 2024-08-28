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

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.BACKUP_STATUS;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.REPEAT_COUNT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class FakeClearToNorthBoundUtilityTest {

    private final Map<String, Object> additionalAlarmAttributesInDatabase = new HashMap<String, Object>();
    final Map<String, String> additionalInformation = new HashMap<String, String>();
    private final String routeToNms = "RouteToNMS";

    @Mock
    private ProcessedAlarmEvent processedAlarmEvent;

    @Test
    public void checkIfFakeClearIsToBeSentToNorthBoundTest() {
        initializeAlarmAttributesInMaps();
        additionalInformation.put(routeToNms, "0");
        when(processedAlarmEvent.getAdditionalInformation()).thenReturn(additionalInformation);
        assertTrue(FakeClearToNorthBoundUtility.checkIfFakeClearIsToBeSentToNorthBound(processedAlarmEvent, additionalAlarmAttributesInDatabase));
        additionalInformation.put(routeToNms, "1");
        when(processedAlarmEvent.getAdditionalInformation()).thenReturn(additionalInformation);
        assertFalse(FakeClearToNorthBoundUtility.checkIfFakeClearIsToBeSentToNorthBound(processedAlarmEvent, additionalAlarmAttributesInDatabase));
        
    }

    private void initializeAlarmAttributesInMaps() {
        additionalAlarmAttributesInDatabase.put(BACKUP_STATUS, "false");
        additionalAlarmAttributesInDatabase.put(VISIBILITY, true);
        additionalAlarmAttributesInDatabase.put(routeToNms, "1");
        additionalAlarmAttributesInDatabase.put(REPEAT_COUNT, "1");
        additionalAlarmAttributesInDatabase
                .put(ADDITIONAL_INFORMATION,
                        "behalf:ManagementSystem=ENM#sourceType:RadioNode#managedObject:ManagedElement#externalEventId:#"
                                + "translateResult:FORWARD_ALARM#eventAgentId:#fdn:NetworkElement=lienb0961#originalEventTimeFromNode:1518919754921#operator:#RouteToNMS:1");
    }

}

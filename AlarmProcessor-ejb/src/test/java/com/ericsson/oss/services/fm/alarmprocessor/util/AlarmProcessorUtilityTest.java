/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static org.junit.Assert.assertEquals;

import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.getManagedObject;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.EMPTY_STRING;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AlarmProcessorUtilityTest {

    @InjectMocks
    private AlarmProcessorUtility alarmProcessorUtility;

    @Test
    public void test_getManagedObject() {
        final String input = "MeContext=ABCD,ManagedElement=1,ENodeBFunction=1,Slot=234";
        final String result = getManagedObject(input);
        assertEquals("Slot", result);
    }

    @Test
    public void test_exception_getManagedObject() {
        final String input = null;
        final String result = getManagedObject(input);
        assertEquals(EMPTY_STRING, result);
    }
}

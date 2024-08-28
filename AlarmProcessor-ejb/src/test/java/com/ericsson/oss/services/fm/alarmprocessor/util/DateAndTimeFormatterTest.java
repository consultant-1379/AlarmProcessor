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

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.util.DateAndTimeFormatter;

@RunWith(MockitoJUnitRunner.class)
public class DateAndTimeFormatterTest {

    @InjectMocks
    private DateAndTimeFormatter dateAndTimeFormatter;

    @Test
    public void testParseTime() {
        final Date time = new Date();
        final Date result = dateAndTimeFormatter.parseTime(time.toString(), "UTC");
        assertEquals(time.getYear(), result.getYear());
    }

}

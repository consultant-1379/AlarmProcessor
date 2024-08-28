/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.ErrorAlarmCommonHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.ErrorAlarmHandler;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class ErrorAlarmHandlerTest {

    @InjectMocks
    private ErrorAlarmHandler errorAlarmHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ErrorAlarmCommonHandler errorAlarmCommonHandler;

    @Test
    public void testHandleAlarm() {
        errorAlarmHandler.handleAlarm(alarmRecord);
        verify(errorAlarmCommonHandler).handleAlarm(alarmRecord);

    }

}

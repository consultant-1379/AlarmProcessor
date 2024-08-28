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
package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.HeartBeatCommonHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.HeartBeatFailureNoSyncHandler;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class HeartBeatFailureNoSyncHandlerTest {

    @InjectMocks
    private HeartBeatFailureNoSyncHandler heartBeatFailureNoSyncHandler;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private HeartBeatCommonHandler heartBeatCommonHandler;

    @Test
    public void testhandleAlarm() {
        alarmRecord = new ProcessedAlarmEvent();
        heartBeatFailureNoSyncHandler.handleAlarm(alarmRecord);
        verify(heartBeatCommonHandler).handleAlarm(alarmRecord);

    }
}

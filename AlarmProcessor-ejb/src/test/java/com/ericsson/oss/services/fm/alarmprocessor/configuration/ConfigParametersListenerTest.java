/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.configuration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.AlarmSyncAbortTimer;

@RunWith(MockitoJUnitRunner.class)
public class ConfigParametersListenerTest {

    @InjectMocks
    private ConfigParametersListener configParametersListener;

    @Mock
    private AlarmSyncAbortTimer alarmSyncAbortTimer;

    @Test
    public void listenForOscillationCorrelationChangesTest() {
        configParametersListener.listenForOscillationCorrelationChanges(true);
        assertEquals(true, configParametersListener.getOscillatingCorrelation());
    }

    @Test
    public void listenForUpdateInsertTimeTest() {
        configParametersListener.listenForUpdateInsertTimeChanges(true);
        assertEquals(true, configParametersListener.getUpdateInsertTime());
    }

    @Test
    public void listenForTimerIntervalToDiscardOngoingAlarmSyncTest() {
        configParametersListener.listenForTimerIntervalToDiscardOngoingAlarmSync(10000);
        assertEquals(10000, configParametersListener.getTimerIntervalToDiscardOngoingAlarmSync());
        verify(alarmSyncAbortTimer).recreateTimerWithNewInterval(10000);
    }

    @Test
    public void listenForRretryLimitForAlarmProcessingTest() {
        configParametersListener.listenForRetryLimitForAlarmProcessing(10000);
        assertEquals(10000, configParametersListener.getRetryLimitForAlarmProcessing());
    }

//    @Test
//    public void listenForPperiodicAlarmSynchronizationTest() {
//        configParametersListener.listenForPeriodicAlarmSynchronizationChanges(true);
//        assertEquals(true, configParametersListener.getPeriodicAlarmSynchronization());
//        verify(periodicAlarmSyncTimer).handlePeriodicAlarmSynchronizationChanges(true);
//    }
//
//    @Test
//    public void listenForTimerIntervalToInitiateAlarmSyncTest() {
//        configParametersListener.listenForTimerIntervalToInitiateAlarmSync(10000);
//        assertEquals(10000, configParametersListener.getTimerIntervalToInitiateAlarmSync());
//        verify(periodicAlarmSyncTimer).recreateTimerWithNewInterval();
//    }
}

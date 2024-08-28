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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.FmxAttributesWriter;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class AlarmCorrelatorTest {

    @InjectMocks
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ProcessedAlarmEvent clearAlarm;

    @Mock
    private ProcessedAlarmEvent originalAlarm;

    @Mock
    private FmxAttributesWriter fmxAttributesSetter;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private PersistenceObject po;

    @Test
    public void testGetCorrelatedAlarm() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setAlarmNumber(1235L);
        alarmRecord.setObjectOfReference("oor");
        alarmRecord.setSpecificProblem("SpecificProblem");
        alarmRecord.setProbableCause("probableCause");
        alarmRecord.setEventType("evenType");
        alarmRecord.setRecordType(FMProcessedEventType.ALARM);
        alarmCorrelator.getCorrelatedAlarm(alarmRecord);
        verify(fmxAttributesSetter).updateVisibilityForNodeClearAlarm((ProcessedAlarmEvent) anyObject(), (ProcessedAlarmEvent) anyObject());
    }

    /**
     * Test method for {@link com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator#correlateAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}.
     */
    @Test
    public void testCorrelateAlarm_True() {
        clearAlarm = new ProcessedAlarmEvent();
        originalAlarm = new ProcessedAlarmEvent();
        clearAlarm.setAlarmNumber(1234L);
        originalAlarm.setAlarmNumber(1234L);
        clearAlarm.setSpecificProblem("specificProblem");
        originalAlarm.setSpecificProblem("specificProblem");
        clearAlarm.setProbableCause("probableCause");
        originalAlarm.setProbableCause("probableCause");
        clearAlarm.setEventType("eventType");
        originalAlarm.setEventType("eventType");
        clearAlarm.setEventTime(new Date());
        originalAlarm.setEventTime(new Date());
        final boolean result = alarmCorrelator.correlateAlarm(clearAlarm, originalAlarm);
        assertTrue(result);

    }

    @Test
    public void testCorrelateAlarm() {
        clearAlarm = new ProcessedAlarmEvent();
        originalAlarm = new ProcessedAlarmEvent();
        clearAlarm.setAlarmNumber(-2L);
        originalAlarm.setAlarmNumber(-2L);
        clearAlarm.setSpecificProblem("specificProblem");
        originalAlarm.setSpecificProblem("specificProblem");
        clearAlarm.setProbableCause("probableCause");
        originalAlarm.setProbableCause("probableCause");
        clearAlarm.setEventType("eventType");
        originalAlarm.setEventType("eventType");
        clearAlarm.setEventTime(new Date());
        originalAlarm.setEventTime(new Date());
        final boolean result = alarmCorrelator.correlateAlarm(clearAlarm, originalAlarm);
        assertTrue(result);

    }

}

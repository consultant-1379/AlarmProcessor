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

package com.ericsson.oss.services.fm.alarmprocessor.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ORIGINAL_EVENTTIME_FROM_NODE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.NetworkElementMoReader;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

@RunWith(MockitoJUnitRunner.class)
public class AlarmValidatorTest {

    @InjectMocks
    private AlarmValidator alarmValidator;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private NetworkElementMoReader networkElementMOReader;

    @Mock
    private SystemRecorder systemRecorder;

    @Test
    public void testIsAlarmValid() {
        final Date date = new Date();
        alarmRecord = new ProcessedAlarmEvent();
        correlatedAlarm = new ProcessedAlarmEvent();
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(ORIGINAL_EVENTTIME_FROM_NODE, ((Long) date.getTime()).toString());
        additionalInformation.put(HIDE_OPERATION, FMX_HIDE);
        alarmRecord.setAdditionalInformation(additionalInformation);
        correlatedAlarm.setAdditionalInformation(additionalInformation);
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        correlatedAlarm.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm);
        assertTrue(alarmValidator.isAlarmValid(alarmRecord, correlatedAlarm));

    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator#isAlarmToBeHandled(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testIsAlarmToBeHandled() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setRecordType(FMProcessedEventType.UNKNOWN_RECORD_TYPE);
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_IGNORED);
        // alarmRecord.setObjectOfReference("objectOfReference");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        apsInstrumentedBean.incrementDiscardedAlarmCount(alarmRecord.getPresentSeverity());
        alarmValidator.isAlarmToBeHandled(alarmRecord);
        assertFalse(alarmValidator.isAlarmToBeHandled(alarmRecord));

    }

    @Test
    public void testIsAlarmToBeHandled_oor() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setObjectOfReference("objectOfReference");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        alarmRecord.setRecordType(FMProcessedEventType.CLEAR_LIST);
        alarmRecord.setFdn("fdn");
        alarmValidator.isAlarmToBeHandled(alarmRecord);
        assertTrue(alarmValidator.isAlarmToBeHandled(alarmRecord));

    }

    @Test
    public void testIsNetworkElementExists_null_Fdn() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setObjectOfReference("objectOfReference");
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        alarmRecord.setRecordType(FMProcessedEventType.CLEARALL);
        assertFalse(alarmValidator.isNetworkElementExists(alarmRecord.getFdn()));
    }
}

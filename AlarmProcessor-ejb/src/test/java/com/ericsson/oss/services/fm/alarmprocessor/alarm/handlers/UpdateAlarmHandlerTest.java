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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.CI_GROUP_1;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.CI_GROUP_2;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.ROOT;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.TARGET_ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.EVENT_PO_ID;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ACK_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CORRELATEDVISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.INSERT_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OBJECT_OF_REFERENCE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OSCILLATION_COUNT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.REPEAT_COUNT;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_HIDE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.HIDE_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_ALARM_OPERATION;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.api.alarmsender.AlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.ServiceProxyProviderBean;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.common.addinfo.CorrelationType;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class UpdateAlarmHandlerTest {

    @InjectMocks
    private UpdateAlarmHandler updateAlarmHandler;

    @Mock
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ProcessedAlarmEvent processedAlarmEvent;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private AlarmSender alarmSender;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleAlarm() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setObjectOfReference("MAJOR");
        alarmRecord.setRecordType(FMProcessedEventType.ERROR_MESSAGE);
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(EVENT_PO_ID, "1234");
        alarmRecord.setAdditionalInformation(additionalInformation);
        final Map<String, Object> existingAlarmAttributes = new HashMap<String, Object>();
        final String severity = "MAJOR";
        final String oor = "MAJOR";
		final String perceivedSeverity = "MAJOR";
		final String ceaseOperator="administrator";
		final String lastAlarmOperation ="CLEAR";
		existingAlarmAttributes.put(LAST_ALARM_OPERATION, lastAlarmOperation);
		existingAlarmAttributes.put(CEASE_OPERATOR, ceaseOperator);
		existingAlarmAttributes.put(PREVIOUS_SEVERITY, perceivedSeverity);
        existingAlarmAttributes.put(PRESENT_SEVERITY, severity);
        existingAlarmAttributes.put(OBJECT_OF_REFERENCE, oor);
        existingAlarmAttributes.put(INSERT_TIME, new Date());
        existingAlarmAttributes.put(CEASE_TIME, new Date());
        existingAlarmAttributes.put(REPEAT_COUNT, 0);
        existingAlarmAttributes.put(OSCILLATION_COUNT, 0);
        existingAlarmAttributes.put(RECORD_TYPE, "ERROR_MESSAGE");
        existingAlarmAttributes.put(CORRELATEDVISIBILITY, false);
        existingAlarmAttributes.put(ADDITIONAL_INFORMATION, HIDE_OPERATION);
        existingAlarmAttributes.put(VISIBILITY, true);
        when(alarmReader.readAllAttributes(1234L)).thenReturn(existingAlarmAttributes);
        updateAlarmHandler.handleAlarm(alarmRecord);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleAlarm_Cleared_Alarm() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setObjectOfReference("MAJOR");
        alarmRecord.setVisibility(false);
        alarmRecord.setRecordType(FMProcessedEventType.ERROR_MESSAGE);
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(EVENT_PO_ID, "1234");
        additionalInformation.put(HIDE_OPERATION, FMX_HIDE);
        alarmRecord.setAdditionalInformation(additionalInformation);
        final Map<String, Object> existingAlarmAttributes = new HashMap<String, Object>();
        final String severity = "CLEARED";
        final String oor = "MAJOR";
		final String perceivedSeverity = "MAJOR";
		final String ceaseOperator="administrator";
		final String lastAlarmOperation ="CLEAR";
		existingAlarmAttributes.put(LAST_ALARM_OPERATION, lastAlarmOperation);
		existingAlarmAttributes.put(CEASE_OPERATOR, ceaseOperator);
		existingAlarmAttributes.put(PREVIOUS_SEVERITY, perceivedSeverity);
        existingAlarmAttributes.put(PRESENT_SEVERITY, severity);
        existingAlarmAttributes.put(OBJECT_OF_REFERENCE, oor);
        existingAlarmAttributes.put(INSERT_TIME, new Date());
        existingAlarmAttributes.put(CEASE_TIME, new Date());
        existingAlarmAttributes.put(REPEAT_COUNT, 0);
        existingAlarmAttributes.put(OSCILLATION_COUNT, 0);
        existingAlarmAttributes.put(VISIBILITY, true);
        existingAlarmAttributes.put(CORRELATEDVISIBILITY, false);
        existingAlarmAttributes.put(RECORD_TYPE, "ERROR_MESSAGE");
        existingAlarmAttributes.put(ADDITIONAL_INFORMATION, HIDE_OPERATION);
        when(alarmReader.readAllAttributes(1234L)).thenReturn(existingAlarmAttributes);
        updateAlarmHandler.handleAlarm(alarmRecord);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleAlarm_Cleared_ACTIVE_ACKNOWLEDGED() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setObjectOfReference("MAJOR");
        alarmRecord.setRecordType(FMProcessedEventType.ERROR_MESSAGE);
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(EVENT_PO_ID, "1234");
        alarmRecord.setAdditionalInformation(additionalInformation);
        final Map<String, Object> existingAlarmAttributes = new HashMap<String, Object>();
        final String severity = "CLEARED";
        final String oor = "MAJOR";
		final String perceivedSeverity = "MAJOR";
		final String ceaseOperator="administrator";
		final String lastAlarmOperation ="CLEAR";
		existingAlarmAttributes.put(LAST_ALARM_OPERATION, lastAlarmOperation);
		existingAlarmAttributes.put(CEASE_OPERATOR, ceaseOperator);
		existingAlarmAttributes.put(PREVIOUS_SEVERITY, perceivedSeverity);
        existingAlarmAttributes.put(PRESENT_SEVERITY, severity);
        existingAlarmAttributes.put(OBJECT_OF_REFERENCE, oor);
        existingAlarmAttributes.put(INSERT_TIME, new Date());
        existingAlarmAttributes.put(CEASE_TIME, new Date());
        existingAlarmAttributes.put(REPEAT_COUNT, 0);
        existingAlarmAttributes.put(OSCILLATION_COUNT, 0);
        existingAlarmAttributes.put(RECORD_TYPE, "ERROR_MESSAGE");
        existingAlarmAttributes.put(ALARM_STATE, "ACTIVE_ACKNOWLEDGED");
        existingAlarmAttributes.put(ACK_OPERATOR, "ERICSSON");
        existingAlarmAttributes.put(ACK_TIME, new Date());
        existingAlarmAttributes.put(VISIBILITY, true);
        existingAlarmAttributes.put(CORRELATEDVISIBILITY, false);
        existingAlarmAttributes.put(ADDITIONAL_INFORMATION, HIDE_OPERATION);
        when(alarmReader.readAllAttributes(1234L)).thenReturn(existingAlarmAttributes);
        updateAlarmHandler.handleAlarm(alarmRecord);
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.UpdateAlarmHandler#setSeverity(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, java.util.Map)}
     * .
     */
    @Test
    public void testHandleAlarm_AlarmAttriubtes_Null() {
        alarmRecord = new ProcessedAlarmEvent();

        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(EVENT_PO_ID, "1234");
        alarmRecord.setAdditionalInformation(additionalInformation);
        final Map<String, Object> existingAlarmAttributes = new HashMap<String, Object>();
        when(alarmReader.readAllAttributes(1234L)).thenReturn(existingAlarmAttributes);
        when(alarmProcessingResponse.getProcessedAlarms()).thenReturn(new ArrayList<ProcessedAlarmEvent>());
        when(serviceProxyProviderBean.getAlarmSender()).thenReturn(alarmSender);
        updateAlarmHandler.handleAlarm(alarmRecord);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTargetAdditionalInformationAlarm() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setObjectOfReference("MAJOR");
        alarmRecord.setRecordType(FMProcessedEventType.ERROR_MESSAGE);
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(EVENT_PO_ID, "1234");
        additionalInformation.put(TARGET_ADDITIONAL_INFORMATION,
                        "DN2=ManagedElement\\=1,Equipment\\=1,RbsSubrack\\=RUW1,RbsSlot\\=5,AuxPlugInUnit\\=RUW-2,DeviceGroup\\=RUW,AiDeviceSet\\=1,AiDevice\\=1;CI = {\"C\": [{\"I\": \"201f0123-88ca-23a2-7451-8B5872ac457b\",\"n\": \"vRC\"}]};");
        alarmRecord.setAdditionalInformation(additionalInformation);
        final Map<String, Object> existingAlarmAttributes = new HashMap<String, Object>();
        final String severity = "MAJOR";
        final String oor = "MAJOR";
		final String perceivedSeverity = "MAJOR";
		final String ceaseOperator="administrator";
		final String lastAlarmOperation ="CLEAR";
		existingAlarmAttributes.put(LAST_ALARM_OPERATION, lastAlarmOperation);
		existingAlarmAttributes.put(CEASE_OPERATOR, ceaseOperator);
		existingAlarmAttributes.put(PREVIOUS_SEVERITY, perceivedSeverity);
        existingAlarmAttributes.put(PRESENT_SEVERITY, severity);
        existingAlarmAttributes.put(OBJECT_OF_REFERENCE, oor);
        existingAlarmAttributes.put(INSERT_TIME, new Date());
        existingAlarmAttributes.put(CEASE_TIME, new Date());
        existingAlarmAttributes.put(REPEAT_COUNT, 0);
        existingAlarmAttributes.put(OSCILLATION_COUNT, 0);
        existingAlarmAttributes.put(RECORD_TYPE, "ERROR_MESSAGE");
        existingAlarmAttributes.put(VISIBILITY, true);
        final String additionalInfoString = EVENT_PO_ID + "=" + "1234" + TARGET_ADDITIONAL_INFORMATION + "=" +
              "DN2=ManagedElement\\=1,Equipment\\=1,RbsSubrack\\=RUW1,RbsSlot\\=5,AuxPlugInUnit\\=RUW-2,DeviceGroup\\=RUW,AiDeviceSet\\=1,AiDevice\\=1;CI = {\"C\": [{\"I\": \"201f0123-88ca-23a2-7451-8B5872ac457b\",\"n\": \"vRC\"}]};";
        existingAlarmAttributes.put(ADDITIONAL_INFORMATION, additionalInfoString);
        existingAlarmAttributes.put(ROOT, CorrelationType.SECONDARY);
        existingAlarmAttributes.put(CI_GROUP_2, "f91a6e32-e523-b217-7C3912ad3012");
        existingAlarmAttributes.put(CI_GROUP_1, "81d4fae-7dec-11d0-a765-00a0c91e6bf6");
        when(alarmReader.readAllAttributes(1234L)).thenReturn(existingAlarmAttributes);
        final AlarmProcessingResponse alarmProcessingResponse = updateAlarmHandler.handleAlarm(alarmRecord);
        assertTrue(alarmRecord.getAdditionalInformationString().equals(
                alarmProcessingResponse.getProcessedAlarms().get(0).getAdditionalInformationString()));
    }
}

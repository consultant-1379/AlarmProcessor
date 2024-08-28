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
 *----------------------------------------------------------------------------
 */

package com.ericsson.oss.services.fm.alarmprocessor.eventhandlers;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.OSSPREFIX_NOT_SET;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.SOURCE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.fm.capability.util.ModelCapabilities;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.SyncAbortAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers.SyncStartAlarmHandler;
import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.SyncInitiator;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.OssPrefixHolder;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class AlarmHandlerBeanTest {

    @InjectMocks
    private AlarmHandlerBean alarmHandlerBean;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private ProcessedAlarmSender processedAlarmSender;

    @Mock
    private SyncInitiator syncInitiator;

    @Mock
    private ModelCapabilities modelCapabilities;

    @Mock
    private SyncAbortAlarmHandler syncAbortAlarmHandler;

    @Mock
    private SyncStartAlarmHandler syncStartAlarmHandler;

    @Mock
    private AlarmValidator alarmValidator;

    @Mock
    private OssPrefixHolder ossPrefixHolder;

    @Test
    public void testOnEvent() {
        alarmProcessingResponse = new AlarmProcessingResponse();
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("FDN");
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ABORTED);
        when(syncAbortAlarmHandler.handleAlarm(alarmRecord)).thenReturn(alarmProcessingResponse);
        when(alarmValidator.isNetworkElementExists("FDN")).thenReturn(true);
        alarmHandlerBean.processAlarm(alarmRecord);
        verify(syncAbortAlarmHandler).handleAlarm(alarmRecord);
    }

    @Test
    public void testOnEventwithAlarmSync() {
        alarmProcessingResponse = new AlarmProcessingResponse();
        alarmProcessingResponse.setInitiateAlarmSync(true);
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("FDN");
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(SOURCE_TYPE, "AlarmSync");
        alarmRecord.setAdditionalInformation(additionalInformation);
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_STARTED);
        when(syncStartAlarmHandler.handleAlarm(alarmRecord)).thenReturn(alarmProcessingResponse);
        when(modelCapabilities.isAlarmSyncSupportedByNode("AlarmSync")).thenReturn(true);
        when(alarmValidator.isNetworkElementExists("FDN")).thenReturn(true);
        alarmHandlerBean.processAlarm(alarmRecord);
        verify(syncStartAlarmHandler).handleAlarm(alarmRecord);
    }

    @Test
    public void testOssPrefixNotSet() {
        alarmProcessingResponse = new AlarmProcessingResponse();
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("FDN");
        alarmRecord.setObjectOfReference("Test=1");
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ABORTED);
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(OSSPREFIX_NOT_SET, "false");
        alarmRecord.setAdditionalInformation(additionalInformation);
        when(syncAbortAlarmHandler.handleAlarm(alarmRecord)).thenReturn(alarmProcessingResponse);
        when(alarmValidator.isNetworkElementExists("FDN")).thenReturn(true);
        when(ossPrefixHolder.getOssPrefix(alarmRecord.getFdn())).thenReturn("ossPrefix");
        alarmHandlerBean.processAlarm(alarmRecord);
        verify(syncAbortAlarmHandler).handleAlarm(alarmRecord);
        assertEquals(alarmRecord.getObjectOfReference(), "ossPrefix,Test=1");
    }

}

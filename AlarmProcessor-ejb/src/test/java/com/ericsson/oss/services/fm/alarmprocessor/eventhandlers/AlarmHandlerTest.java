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

package com.ericsson.oss.services.fm.alarmprocessor.eventhandlers;

import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.SOURCE_TYPE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.INTERNAL_RETRYS_REACHED;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.fm.capability.util.ModelCapabilities;
import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.SyncInitiator;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.UnProcessedAlarmSender;
import com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability.DatabaseStatusProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder;
import com.ericsson.oss.services.fm.alarmprocessor.processing.analyser.ActiveThreadsHolder.ProcessingState;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class AlarmHandlerTest {

    @InjectMocks
    private AlarmHandler alarmHandler;

    @Mock
    private AlarmHandlerBean alarmHandlerBean;

    @Mock
    private ActiveThreadsHolder activeThreadsHolder;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private ProcessedAlarmSender processedAlarmSender;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private DatabaseStatusProcessor versantDbStatusHolder;

    @Mock
    private UnProcessedAlarmSender unProcessedAlarmSender;

    @Mock
    private ModelCapabilities modelCapabilities;

    @Mock
    private SyncInitiator syncInitiator;

    @Test
    public void testOnEvent() {
        alarmProcessingResponse = new AlarmProcessingResponse();
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("FDN");
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_ABORTED);
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put("alarmReceivedTime", "123456789");
        alarmRecord.setAdditionalInformation(additionalInformation);
        when(configParametersListener.getRetryLimitForAlarmProcessing()).thenReturn(0);
        when(versantDbStatusHolder.isDatabaseAvailable()).thenReturn(true);
        when(alarmHandlerBean.processAlarm(alarmRecord)).thenReturn(alarmProcessingResponse);
        when(activeThreadsHolder.getProcessingState()).thenReturn(ProcessingState.RUNNING_NORMALLY);
        alarmHandler.onEvent(alarmRecord);
        verify(alarmHandlerBean).processAlarm(alarmRecord);
        verify(processedAlarmSender).sendAlarms(alarmProcessingResponse, "123456789");
    }

    @Test
    public void testOnEventwithAlarmSync() {
        alarmProcessingResponse = new AlarmProcessingResponse();
        alarmProcessingResponse.setInitiateAlarmSync(true);
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("FDN");
        final Map<String, String> additionalInformation = new HashMap<String, String>();
        additionalInformation.put(SOURCE_TYPE, "AlarmSync");
        additionalInformation.put("alarmReceivedTime", "123456789");
        alarmRecord.setAdditionalInformation(additionalInformation);
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_STARTED);
        when(configParametersListener.getRetryLimitForAlarmProcessing()).thenReturn(0);
        when(versantDbStatusHolder.isDatabaseAvailable()).thenReturn(true);
        final Date date = new Date();
        when(alarmHandlerBean.processAlarm(alarmRecord)).thenReturn(alarmProcessingResponse);
        when(modelCapabilities.isAlarmSyncSupportedByNode("AlarmSync")).thenReturn(true);
        when(activeThreadsHolder.getProcessingState()).thenReturn(ProcessingState.RUNNING_NORMALLY);
        alarmHandler.onEvent(alarmRecord);
        verify(alarmHandlerBean).processAlarm(alarmRecord);
        verify(processedAlarmSender).sendAlarms(alarmProcessingResponse, "123456789");
        verify(syncInitiator).initiateAlarmSynchronization(alarmRecord.getFdn());
    }

    @Test
    public void testOnEventWithUnprocessedAlarmtoNBI() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFdn("FDN");
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_STARTED);
        when(configParametersListener.getRetryLimitForAlarmProcessing()).thenReturn(0);
        when(versantDbStatusHolder.isDatabaseAvailable()).thenReturn(false);
        when(activeThreadsHolder.getProcessingState()).thenReturn(ProcessingState.RUNNING_NORMALLY);

        alarmHandler.onEvent(alarmRecord);
        verify(unProcessedAlarmSender).sendUnProcessedEvents(alarmRecord, INTERNAL_RETRYS_REACHED);
    }
}
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

package com.ericsson.oss.services.fm.alarmprocessor.processors;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmSyncStateUpdator;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.ServiceProxyProviderBean;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;

@RunWith(MockitoJUnitRunner.class)
public class CorrelatedAlarmProcessorTest {

    @InjectMocks
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    @Mock
    private CorrelatedClearAlarmProcessor correlatedClearAlarmProcessor;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private CorrelatedUpdateAlarmProcessor correlatedUpdateAlarmProcessor;

    @Mock
    private OscillationCorrelationProcessor oscillationCorrelationProcessor;

    @Mock
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private ServiceStateModifier serviceStateModifier;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Mock
    private DataBucket liveBucket;

    @Mock
    private PersistenceObject persistenceObject;

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor#processNormalAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessNormalAlarm_WithSeverityClear() {

        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        correlatedAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm);
        when(correlatedClearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        verify(correlatedClearAlarmProcessor, times(1)).processAlarm(alarmRecord, correlatedAlarm);

    }

    @Test
    public void testProcessNormalAlarm_WithSeverity() {

        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        correlatedAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm);
        when(correlatedUpdateAlarmProcessor.processNormalAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        verify(correlatedUpdateAlarmProcessor, times(1)).processNormalAlarm(alarmRecord, correlatedAlarm);

    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor#processRepeated(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessRepeated_WithSeverityClear() {

        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        correlatedAlarmProcessor.processRepeated(alarmRecord, correlatedAlarm);
        when(correlatedClearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        verify(correlatedClearAlarmProcessor, times(1)).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testProcessRepeated_WithSeverity() {

        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        correlatedAlarmProcessor.processRepeated(alarmRecord, correlatedAlarm);
        when(correlatedUpdateAlarmProcessor.processRepeatedAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        verify(correlatedUpdateAlarmProcessor, times(1)).processRepeatedAlarm(alarmRecord, correlatedAlarm);
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor#processAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessAlarm_WithSeverityClear() {
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        when(correlatedClearAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        verify(correlatedClearAlarmProcessor, times(1)).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testProcessAlarm_WithSeverity() {
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        correlatedAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm);
        when(correlatedUpdateAlarmProcessor.processAlarm(alarmRecord, correlatedAlarm)).thenReturn(alarmProcessingResponse);
        verify(correlatedUpdateAlarmProcessor, times(1)).processAlarm(alarmRecord, correlatedAlarm);
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor#processClearAllAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessClearAllAlarm_WithSeverityClear() {
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        correlatedAlarmProcessor.processClearAllAlarm(alarmRecord, correlatedAlarm);
        verify(oscillationCorrelationProcessor, times(1)).processAlarm(alarmRecord, correlatedAlarm);
    }

    @Test
    public void testProcessClearAllAlarm_WithSeverity() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setCorrelatedPOId(1234L);
        final Map<String, Object> pOAttributes = new HashMap<String, Object>();
        final Map<String, Object> alarmAttributes = new HashMap<String, Object>();
        when(alarmReader.readAllAttributes(alarmRecord.getCorrelatedPOId())).thenReturn(pOAttributes);
        when(alarmAttributesPopulator.populateUpdateAlarm(alarmRecord, pOAttributes)).thenReturn(alarmAttributes);
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        correlatedAlarmProcessor.processClearAllAlarm(alarmRecord, correlatedAlarm);
        verify(openAlarmService).updateAlarm(alarmRecord.getCorrelatedPOId(), alarmAttributes);
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor#processSyncAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessSyncAlarm_WithSeverity() {
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        when(alarmRecord.getFmxGenerated()).thenReturn("NOT_SET");
        when(alarmRecord.getVisibility()).thenReturn(false);
        when(correlatedAlarm.getVisibility()).thenReturn(false);
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findPoById(anyLong())).thenReturn(persistenceObject);
        when(alarmRecord.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        when(correlatedAlarm.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        when(alarmRecord.getRecordType()).thenReturn(FMProcessedEventType.SYNCHRONIZATION_ALARM);
        correlatedAlarmProcessor.processSyncAlarm(alarmRecord, correlatedAlarm);
        apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(ProcessedEventSeverity.CRITICAL);
        verify(openAlarmSyncStateUpdator).updateSyncStateForPoId(0L, true);

    }

    @Test
    public void testProcessSyncAlarm_ChangeDispStatus() {
        final Object visibility = null;
        when(alarmRecord.getEventPOId()).thenReturn(1234L);
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CRITICAL);
        when(alarmRecord.getFmxGenerated()).thenReturn("NOT_SET");
        // when(alarmRecord.getVisibility()).thenReturn(true);
        when(alarmRecord.isVisibility()).thenReturn(true);
        when(correlatedAlarm.getVisibility()).thenReturn(false);
        when(alarmReader.readAttribute(alarmRecord.getCorrelatedPOId(), VISIBILITY)).thenReturn(visibility);
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findPoById(anyLong())).thenReturn(persistenceObject);
        when(alarmRecord.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_ACKNOWLEDGED);
        when(correlatedAlarm.getAlarmState()).thenReturn(ProcessedEventState.ACTIVE_UNACKNOWLEDGED);
        when(alarmRecord.getRecordType()).thenReturn(FMProcessedEventType.SYNCHRONIZATION_ALARM);
        correlatedAlarmProcessor.processSyncAlarm(alarmRecord, correlatedAlarm);
        apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(ProcessedEventSeverity.CRITICAL);
        verify(openAlarmSyncStateUpdator).updateSyncStateForPoId(0L, true);

    }

    @Test
    public void testProcessSyncAlarm_With_clear_Severity() {
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.MAJOR);
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        when(correlatedAlarm.getSyncState()).thenReturn(true);
        correlatedAlarmProcessor.processSyncAlarm(alarmRecord, correlatedAlarm);
        verify(oscillationCorrelationProcessor).processAlarm(alarmRecord, correlatedAlarm);

    }

    @Test
    public void testProcessSyncAlarm_With_Correlated_Severity() {
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.MAJOR);
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.MINOR);
        when(correlatedAlarm.getSyncState()).thenReturn(true);
        apsInstrumentedBean.incrementCorrelatedProcessedAlarmCount(ProcessedEventSeverity.MINOR);
        final Map<String, Object> pOAttributes = new HashMap<String, Object>();
        final Map<String, Object> alarmAttributes = new HashMap<String, Object>();
        when(alarmReader.readAllAttributes(1234L)).thenReturn(pOAttributes);
        when(alarmAttributesPopulator.populateUpdateAlarm(alarmRecord, pOAttributes)).thenReturn(alarmAttributes);
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        correlatedAlarmProcessor.processSyncAlarm(alarmRecord, correlatedAlarm);
        verify(openAlarmService).updateAlarm(0L, alarmAttributes);

    }

    @Test
    public void testProcessSyncAlarm_WithSeverityClear() {
        when(alarmRecord.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        when(correlatedAlarm.getPresentSeverity()).thenReturn(ProcessedEventSeverity.CLEARED);
        when(correlatedAlarm.getSyncState()).thenReturn(true);
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        correlatedAlarmProcessor.processSyncAlarm(alarmRecord, correlatedAlarm);
        verify(openAlarmSyncStateUpdator).updateSyncStateForPoId(0L, true);

    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor#processSyncReplaceAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testProcessSyncReplaceAlarm() {
        correlatedAlarm = new ProcessedAlarmEvent();
        alarmRecord = new ProcessedAlarmEvent();
        correlatedAlarm.setEventPOId(1234L);
        correlatedAlarm.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        correlatedAlarm.setRepeatCount(3);
        alarmRecord.setCorrelatedPOId(correlatedAlarm.getEventPOId());
        alarmRecord.setPreviousSeverity(correlatedAlarm.getPresentSeverity());
        alarmRecord.setRepeatCount(correlatedAlarm.getRepeatCount());
        final Date date = new Date();
        alarmRecord.setLastUpdatedTime(date);
        alarmRecord.setLastAlarmOperation(ProcessedLastAlarmOperation.CHANGE);
        when(configParametersListener.getUpdateInsertTime()).thenReturn(false);
        correlatedAlarmProcessor.processSyncReplaceAlarm(alarmRecord, correlatedAlarm);
        verify(serviceStateModifier).updateFmFunctionBasedOnSpecificProblem(alarmRecord);
    }

}

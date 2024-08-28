/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarm.handlers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.EMPTY_STRING;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.translator.model.Constants;
import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.SyncInitiator;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmSupervisionMoReader;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.processors.AlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.CorrelatedAlarmProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.processors.OscillationCorrelationProcessor;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmCorrelator;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

@RunWith(MockitoJUnitRunner.class)
public class ClearAllAlarmHandlerTest {

    @InjectMocks
    private ClearAllAlarmHandler clearAllHandler;

    @Mock
    private SyncInitiator syncInitiator;

    @Mock
    private FmSupervisionMoReader fmSupervisionMOReader;

    @Mock
    private AlarmCorrelator alarmCorrelator;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private OpenAlarmService openAlarmService;

    @Mock
    private Map<String, Boolean> managedObjectAttributes;

    @Mock
    private AlarmProcessingResponse alarmProcessingResponse;

    @Mock
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Mock
    private FmFunctionMoService fmFunctionMOFacade;

    @Mock
    private AlarmProcessor newAlarmProcessor;

    private static final String NE_FDN = "NetworkElement=TESTNODE";

    private ProcessedAlarmEvent alarmRecord;

    private ProcessedAlarmEvent correlatedAlarmEvent;

    @Mock
    private Map<String, Object> alarmAtributes;

    @Mock
    private OscillationCorrelationProcessor oscillationCorrelationProcessor;

    @Mock
    private CorrelatedAlarmProcessor correlatedAlarmProcessor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        alarmRecord = new ProcessedAlarmEvent();

    }

    @Test
    public void testClearAllforNode_CorrelatedAlarmExists_AutoSyncEnabled() {
        alarmRecord.setFdn(NE_FDN);
        correlatedAlarmEvent = new ProcessedAlarmEvent();
        correlatedAlarmEvent.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        correlatedAlarmEvent.setCorrelatedPOId(1234L);
        correlatedAlarmEvent.setEventPOId(1234L);
        when(fmSupervisionMOReader.readSupervisionAndAutoSyncAttributes(alarmRecord.getFdn())).thenReturn(managedObjectAttributes);
        when(managedObjectAttributes.get(Constants.AUTOMATIC_SYNCHRONIZATION)).thenReturn(true);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarmEvent);
        when(configParametersListener.getOscillatingCorrelation()).thenReturn(false);
        clearAllHandler.handleAlarm(alarmRecord);
        verify(correlatedAlarmProcessor, times(1)).processClearAllAlarm(alarmRecord, correlatedAlarmEvent);
    }

    @Test
    public void testClearAllforNode_CorrelatedAlarmNotExists_AutoSyncEnabled() {
        alarmRecord.setFdn(NE_FDN);
        correlatedAlarmEvent = null;
        when(fmSupervisionMOReader.readSupervisionAndAutoSyncAttributes(alarmRecord.getFdn())).thenReturn(managedObjectAttributes);
        when(managedObjectAttributes.get(Constants.AUTOMATIC_SYNCHRONIZATION)).thenReturn(true);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarmEvent);
        clearAllHandler.handleAlarm(alarmRecord);
        verify(newAlarmProcessor, times(1)).processClearAllAlarm(alarmRecord);

    }

    @Test
    public void testClearAllforNode_CorrelatedAlarmNotExists_AutoSyncDisabled() {
        alarmRecord.setFdn(NE_FDN);
        correlatedAlarmEvent = null;
        when(fmSupervisionMOReader.readSupervisionAndAutoSyncAttributes(alarmRecord.getFdn())).thenReturn(managedObjectAttributes);
        when(managedObjectAttributes.get(Constants.AUTOMATIC_SYNCHRONIZATION)).thenReturn(false);
        when(alarmCorrelator.getCorrelatedAlarm(alarmRecord)).thenReturn(correlatedAlarmEvent);
        clearAllHandler.handleAlarm(alarmRecord);
        verify(newAlarmProcessor, times(1)).processClearAllAlarm(alarmRecord); // ====>>>> Check why not working - Mrudula

    }

    @Test
    public void testClearAllforNbiSync() {
        alarmRecord.setFdn(EMPTY_STRING);
        when(fmSupervisionMOReader.readSupervisionAndAutoSyncAttributes(alarmRecord.getFdn())).thenReturn(managedObjectAttributes);
        when(managedObjectAttributes.get(Constants.AUTOMATIC_SYNCHRONIZATION)).thenReturn(false);
        clearAllHandler.handleAlarm(alarmRecord);
    }
}

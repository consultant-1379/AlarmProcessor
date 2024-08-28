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

package com.ericsson.oss.services.fm.alarmprocessor.eventsender;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.SECONDARY_CONSUMER;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.fm.capability.util.ModelCapabilities;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability.FmDatabaseAvailabilityCacheManager;
import com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability.FmDatabaseAvailabilityConfigurationListener;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.common.targettype.handler.TargetTypeHandler;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class UnProcessedAlarmSenderTest {

    @InjectMocks
    private UnProcessedAlarmSender unProcessedAlarmSender;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private FmDatabaseAvailabilityCacheManager fmDBAvailabilityCacheManager;

    @Mock
    private FmDatabaseAvailabilityConfigurationListener fmDBAvailabilityConfigurationListener;

    @Mock
    private EventSender<ProcessedAlarmEvent> modeledEventSender;

    @Mock
    private SystemRecorder systemRecorder;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private ModelCapabilities modelCapabilities;
    
    @Mock
    TargetTypeHandler targetTypeHandler;

    @Test
    public void test_sendUnProcessedEvents() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setRecordType(FMProcessedEventType.SYNCHRONIZATION_STARTED);
        alarmRecord.setFdn("Test");
        when(fmDBAvailabilityConfigurationListener.getHandleDbFailure()).thenReturn(true);
        unProcessedAlarmSender.sendUnProcessedEvents(alarmRecord, SECONDARY_CONSUMER);
        verify(fmDBAvailabilityCacheManager, times(1)).addFdn(alarmRecord.getFdn());
    }

    @Test
    public void test_sendNotSynchableUnProcessedEvents() {
        final List<FMProcessedEventType> unsynchronizableRecordTypes = new ArrayList<FMProcessedEventType>();
        unsynchronizableRecordTypes.add(FMProcessedEventType.ERROR_MESSAGE);
        unsynchronizableRecordTypes.add(FMProcessedEventType.NON_SYNCHABLE_ALARM);
        unsynchronizableRecordTypes.add(FMProcessedEventType.REPEATED_ERROR_MESSAGE);
        unsynchronizableRecordTypes.add(FMProcessedEventType.REPEATED_NON_SYNCHABLE);

        for (FMProcessedEventType recordType : unsynchronizableRecordTypes) {
            alarmRecord = new ProcessedAlarmEvent();
            alarmRecord.setRecordType(recordType);
            alarmRecord.setFdn("Test");
            when(fmDBAvailabilityConfigurationListener.getHandleDbFailure()).thenReturn(true);
            when(targetTypeHandler.get("Test")).thenReturn("TargetType");
            when(modelCapabilities.isErrorMessageSyncSupportedByNode("TargetType")).thenReturn(false);
            unProcessedAlarmSender.sendUnProcessedEvents(alarmRecord, SECONDARY_CONSUMER);
            verify(fmDBAvailabilityCacheManager, times(0)).addFdn(alarmRecord.getFdn());
        }
    }

    @Test
    public void test_sendNotSynchableUnProcessedEventsWithResources() {
        final List<FMProcessedEventType> unsynchronizableRecordTypes = new ArrayList<FMProcessedEventType>();
        unsynchronizableRecordTypes.add(FMProcessedEventType.ERROR_MESSAGE);
        unsynchronizableRecordTypes.add(FMProcessedEventType.NON_SYNCHABLE_ALARM);
        unsynchronizableRecordTypes.add(FMProcessedEventType.REPEATED_ERROR_MESSAGE);
        unsynchronizableRecordTypes.add(FMProcessedEventType.REPEATED_NON_SYNCHABLE);

        for (FMProcessedEventType recordType : unsynchronizableRecordTypes) {
            alarmRecord = new ProcessedAlarmEvent();
            alarmRecord.setRecordType(recordType);
            alarmRecord.setFdn("Test");
            when(fmDBAvailabilityConfigurationListener.getHandleDbFailure()).thenReturn(true);
            when(targetTypeHandler.get("Test")).thenReturn("TargetType");
            when(modelCapabilities.isErrorMessageSyncSupportedByNode("TargetType")).thenReturn(true);
            unProcessedAlarmSender.sendUnProcessedEvents(alarmRecord, SECONDARY_CONSUMER);
        }
        verify(fmDBAvailabilityCacheManager, times(2)).addFdn(alarmRecord.getFdn());
    }
}

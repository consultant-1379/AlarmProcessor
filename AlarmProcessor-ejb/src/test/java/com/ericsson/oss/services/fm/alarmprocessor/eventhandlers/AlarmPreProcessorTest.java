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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader;
import com.ericsson.oss.services.fm.alarmprocessor.enrichment.AlarmEnricher;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.protection.AlarmOverloadProtectionService;
import com.ericsson.oss.services.fm.alarmprocessor.util.FmxAttributesWriter;
import com.ericsson.oss.services.fm.alarmprocessor.validators.AlarmValidator;
import com.ericsson.oss.services.fm.common.targettype.handler.TargetTypeHandler;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.ratedetectionengine.api.RateDetectionService;

@RunWith(MockitoJUnitRunner.class)
public class AlarmPreProcessorTest {

    @InjectMocks
    private AlarmPreProcessor alarmPreProcessor;

    @Mock
    private AlarmReader alarmReader;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private TargetTypeHandler targetTypeHandler;

    @Mock
    private EventNotification eventNotification;

    @Mock
    private Iterator<EventNotification> iterator;

    @Mock
    @EServiceRef
    private AlarmEnricher alarmEnricher;

    @Mock
    @EServiceRef
    private RateDetectionService rateDetectionService;

    @Mock
    private ConfigParametersListener configParametersListener;

    @Mock
    private FmxAttributesWriter fmxAttributesWriter;

    @Mock
    private APSInstrumentedBean apsInstrumentedBean;

    @Mock
    private AlarmValidator alarmValidator;

    @Mock
    private AlarmHandler alarmHandler;

    @Mock
    private AlarmOverloadProtectionService alarmOverloadProtectionService;

    @Test
    public void testOnEvent() {
        final ProcessedAlarmEvent alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setFmxGenerated("FMX_PROCESSED");
        alarmRecord.setRecordType(FMProcessedEventType.NODE_SUSPENDED);
        alarmRecord.setCorrelatedPOId(12345L);
        final List<EventNotification> eventNotifications = new ArrayList<EventNotification>();
        eventNotification = new EventNotification();
        eventNotification.setRecordType("NODE_SUSPENDED");
        eventNotification.setFmxGenerated("FMX_PROCESSED");
        eventNotifications.add(eventNotification);
        when(alarmValidator.isAlarmToBeHandled(alarmRecord)).thenReturn(true);
        when(alarmEnricher.enrichNotification(eventNotification)).thenReturn(eventNotification);
        alarmHandler.onEvent(alarmRecord);
        alarmPreProcessor.onEvent(eventNotifications);
        verify(apsInstrumentedBean).increaseAlarmCountReceivedByAPS();
        verify(rateDetectionService, never()).increment("alarmrate", Long.valueOf(eventNotifications.size()));
    }
}

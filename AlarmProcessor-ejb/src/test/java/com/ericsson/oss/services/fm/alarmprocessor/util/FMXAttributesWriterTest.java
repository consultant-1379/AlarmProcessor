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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_PROCESSED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NORMAL_PROC;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

@RunWith(MockitoJUnitRunner.class)
public class FMXAttributesWriterTest {

    @InjectMocks
    private FmxAttributesWriter fmxAttributesWriter;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

    @Mock
    private EventNotification eventNotification;

    @Mock
    private ProcessedAlarmEvent correlatedAlarm;

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.util.FmxAttributesWriter#setEnrichmentValues(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.mediation.translator.model.EventNotification)}
     * .
     */
    @Test
    public void testSetEnrichmentValues() {
        final List<String> discriminatorList = new ArrayList<String>();
        eventNotification = new EventNotification();
        alarmRecord = new ProcessedAlarmEvent();
        eventNotification.setDiscriminatorList(discriminatorList);
        eventNotification.setProcessingType("NORMAL_PROC");
        eventNotification.setFmxGenerated("FMX_PROCESSED");
        eventNotification.setVisibility(true);
        fmxAttributesWriter.setEnrichmentValues(alarmRecord, eventNotification);
        assertEquals(alarmRecord.getProcessingType(), eventNotification.getProcessingType());
        assertEquals(alarmRecord.getDiscriminatorList(), eventNotification.getDiscriminatorList());
        assertEquals(alarmRecord.getFmxGenerated(), eventNotification.getFmxGenerated());
        assertNotEquals(alarmRecord.getCorrelatedVisibility(), eventNotification.isVisibility());

    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.util.FmxAttributesWriter#updateVisibilityForNodeClearAlarm(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent, com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */

    @Test
    public void testUpdateVisibilityForNodeClearAlarm() {
        alarmRecord.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        alarmRecord.setProcessingType(NORMAL_PROC);
        alarmRecord.setFmxGenerated(FMX_PROCESSED);
        fmxAttributesWriter.updateVisibilityForNodeClearAlarm(alarmRecord, correlatedAlarm);
        assertEquals(alarmRecord.getVisibility(), correlatedAlarm.getVisibility());
        assertEquals(alarmRecord.getCorrelatedVisibility(), correlatedAlarm.getCorrelatedVisibility());
    }

}

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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;

@RunWith(MockitoJUnitRunner.class)
public class ServiceStateModifierTest {

    @InjectMocks
    private ServiceStateModifier serviceStateModifier;
    @Mock
    private ProcessedAlarmEvent alarmRecord;
    @Mock
    private FmFunctionMoService fmFunctionMOFacade;

    @Mock
    private SystemRecorder systemRecorder;

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.util.ServiceStateModifier#updateFmFunctionBasedOnSpecificProblem(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testUpdateFmFuntionBasedOnSP_AlarmSuppressedState() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setSpecificProblem(AlarmProcessorConstants.ALARMSUPPRESSED_SP);
        alarmRecord.setFdn("NetworkElement=LTE09ERBS00009");
        when(fmFunctionMOFacade.read(alarmRecord.getFdn(), AlarmProcessorConstants.ALARM_SUPPRESSED_STATE)).thenReturn(false);
        serviceStateModifier.updateFmFunctionBasedOnSpecificProblem(alarmRecord);
        verify(fmFunctionMOFacade).update(alarmRecord.getFdn(), AlarmProcessorConstants.ALARM_SUPPRESSED_STATE, true);
        verify(systemRecorder).recordEvent("APS", EventLevel.DETAILED, "AlarmSuppressedState/TechnicianPresentState ",
                "is changed to true for fdn", alarmRecord.getFdn());

    }

    @Test
    public void testUpdateFmFuntionBasedOnSP_TechnicianPresentState() {
        alarmRecord = new ProcessedAlarmEvent();
        alarmRecord.setSpecificProblem(AlarmProcessorConstants.TECHNICIANPRESENT_SP);
        alarmRecord.setFdn("NetworkElement=LTE09ERBS00009");
        when(fmFunctionMOFacade.read(alarmRecord.getFdn(), AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE)).thenReturn(false);
        serviceStateModifier.updateFmFunctionBasedOnSpecificProblem(alarmRecord);
        verify(fmFunctionMOFacade).update(alarmRecord.getFdn(), AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE, true);
        verify(systemRecorder).recordEvent("APS", EventLevel.DETAILED, "AlarmSuppressedState/TechnicianPresentState ",
                "is changed to true for fdn", alarmRecord.getFdn());

    }

}

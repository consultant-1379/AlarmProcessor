/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.alarmsync;

import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.AlarmSupervisionController;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.FmCliCommand;
import static com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100.IN_SERVICE;

@RunWith(MockitoJUnitRunner.class)
public class SyncInitiatorTest {

    @InjectMocks
    private final SyncInitiator syncInitiator = new SyncInitiator();

    @Mock
    private AlarmSupervisionController alarmSupervisionController;

    @Mock
    private FmCliCommand fmCliCommand;
    
    @Mock
    private ClearAlarmsCacheManager clearAlarmsCacheManager;
 
    @Mock
    private FmFunctionMoService fmFunctionMOFacade;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        String currentServiceState = "IN_SERVICE";
    }

    @Test
    public void testInitiateSyncForSingleNode() {
        final String FDN = "NetworkElement=TEST";
        syncInitiator.initiateAlarmSynchronization(FDN);
        verify(alarmSupervisionController, times(1)).initiateSync((FmCliCommand) anyObject(), anyBoolean());
    }

    @Test
    public void testIinitiateAlarmSyncForSetofNodes() {
        final Set<String> neFdns = new HashSet<String>();
        neFdns.add("NetworkElement=TEST");
        neFdns.add("NetworkElement=TEST1");
        when(fmFunctionMOFacade.read("NetworkElement=TEST", FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(IN_SERVICE.name());
        when(fmFunctionMOFacade.read("NetworkElement=TEST1", FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(IN_SERVICE.name());
        syncInitiator.initiateAlarmSync(neFdns);
        verify(alarmSupervisionController, times(1)).initiateSync((FmCliCommand) anyObject(), anyBoolean());
    }

    @Test
    public void testIinitiateAlarmSyncForSetofNodesWithClearAlarmsException() {
        final Set<String> neFdns = new HashSet<String>();
        neFdns.add("NetworkElement=TEST");
        neFdns.add("NetworkElement=TEST1");
        when(fmFunctionMOFacade.read("NetworkElement=TEST", FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(IN_SERVICE.name());
        when(fmFunctionMOFacade.read("NetworkElement=TEST1", FM_SUPERVISEDOBJECT_SERVICE_STATE)).thenReturn(IN_SERVICE.name());
        doThrow(new IllegalStateException()).when(clearAlarmsCacheManager).removeFdnFromCache(neFdns);
        syncInitiator.initiateAlarmSync(neFdns);
        verify(alarmSupervisionController, times(1)).initiateSync((FmCliCommand) anyObject(), anyBoolean());
    }

}
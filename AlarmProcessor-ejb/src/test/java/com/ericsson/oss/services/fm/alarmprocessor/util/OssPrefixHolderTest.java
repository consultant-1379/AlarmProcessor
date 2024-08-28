/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
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
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.NetworkElementMoReader;

@RunWith(MockitoJUnitRunner.class)
public class OssPrefixHolderTest {
    
    @InjectMocks
    private OssPrefixHolder ossPrefixHolder;
    
    @Mock
    private NetworkElementMoReader networkElementMOReader;
    
    @Mock
    private ManagedObject managedObject;
    
    @Mock
    private Map<String,Object> attributes;
    
    @Test
    public void getOssPrefixTest_FromDatabase(){
        final String fdn = "NetworkElement=Test";
        attributes = new HashMap<String,Object>();
        attributes.put("ossPrefix", "MeContext=Test,NetworkElement=Test");
        when(networkElementMOReader.read(fdn)).thenReturn(managedObject);
        when(managedObject.getAllAttributes()).thenReturn(attributes);
        ossPrefixHolder.getOssPrefix(fdn);
        assertEquals(ossPrefixHolder.getOssPrefix(fdn),"MeContext=Test,NetworkElement=Test");
    }

}

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
package com.ericsson.oss.services.fm.alarmprocessor.dps.util;

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;

@RunWith(MockitoJUnitRunner.class)
public class NetworkElementMOReaderTest {

    @InjectMocks
    private NetworkElementMoReader networkElementMOReader;

    @Mock
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Mock
    private ManagedObject managedObject;

    @Mock
    private DataBucket liveBucket;

    @Test
    public void testRead() {
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn("fdn")).thenReturn(managedObject);
        networkElementMOReader.read("fdn");

    }

}

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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.object.builder.PersistenceObjectBuilder;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.RestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;

@RunWith(MockitoJUnitRunner.class)
public class FmSupervisionMOReaderTest {

    @InjectMocks
    private FmSupervisionMoReader fmSupervisionMOReader;

    @Mock
    private ServiceProxyProviderBean serviceProxyProviderBean;

    @Mock
    private QueryBuilder queryBuilder;

    @Mock
    private DataBucket liveBucket;

    @Mock
    private Query<TypeRestrictionBuilder> typeQuery;

    @Mock
    private TypeRestrictionBuilder typeRestrictionBuilder;

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private Restriction restriction;

    @Mock
    private RestrictionBuilder restrictionBuilder;

    @Mock
    private Iterator<Object> poListIterator;

    @Mock
    private PersistenceObject poObject;

    @Mock
    private PersistenceObjectBuilder persistenceObjectBuilder;

    @Mock
    private ManagedObject managedObject;

    @Test
    public void testRead() {
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(anyString())).thenReturn(managedObject);
        final Object obj = "TestResult";
        when(managedObject.getAttribute("Test")).thenReturn(obj);
        final Object result = fmSupervisionMOReader.read("FDN", "Test");
        assertEquals(result, obj);

    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmSupervisionMoReader#readSupervisionAndAutoSyncAttributes(java.lang.String)}.
     */
    @Test
    public void testReadSupervisionAndAutoSyncAttributes() {
        new HashMap<String, Boolean>(2);
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(anyString())).thenReturn(managedObject);
        fmSupervisionMOReader.readSupervisionAndAutoSyncAttributes("fdn");
        // assertFalse(supervisionAndAutosyncAttributes.get(AUTOMATIC_SYNCHRONIZATION);

    }

}

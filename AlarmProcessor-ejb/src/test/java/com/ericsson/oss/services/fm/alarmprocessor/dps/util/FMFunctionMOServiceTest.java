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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LASTUPDATEDTIMESTAMP;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_UPDATED;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_FUNCTION;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OSS_NE_FM_DEF;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
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
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

@RunWith(MockitoJUnitRunner.class)
public class FMFunctionMOServiceTest {

    @InjectMocks
    private FmFunctionMoService fmFunctionMOService;

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
    private Restriction currentServiceStateRestriction;

    @Mock
    private Restriction lessThanDateRestriction;

    @Mock
    private Restriction equalToDateRestriction;

    @Mock
    private RestrictionBuilder restrictionBuilder;

    @Mock
    private Restriction dateRestriction;

    @Mock
    private Restriction finalRestriction;

    @Mock
    private Iterator<Object> poListIterator;

    @Mock
    private PersistenceObject poObject;

    @Mock
    private PersistenceObjectBuilder persistenceObjectBuilder;

    @Mock
    private ManagedObject managedObject;

    @Mock
    private Projection fdnProjection;

    @Test
    public void testReadNodeListForLongOngoingSync() {

        final Date currentTime = new Date();
        final int duration = 10;
        final long milliseconds = currentTime.getTime() - duration * 60 * 1000;
        final Date pastTime = new Date(milliseconds);
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(serviceProxyProviderBean.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(OSS_NE_FM_DEF, FM_FUNCTION)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(typeQuery.getRestrictionBuilder().equalTo(FM_SUPERVISEDOBJECT_SERVICE_STATE, FmSyncStatus100.SYNC_ONGOING.name())).thenReturn(
                currentServiceStateRestriction);
        when(typeQuery.getRestrictionBuilder().lessThan(LASTUPDATEDTIMESTAMP, pastTime)).thenReturn(lessThanDateRestriction);
        when(typeQuery.getRestrictionBuilder().equalTo(LASTUPDATEDTIMESTAMP, pastTime)).thenReturn(equalToDateRestriction);
        when(restrictionBuilder.anyOf(lessThanDateRestriction, equalToDateRestriction)).thenReturn(dateRestriction);
        when(restrictionBuilder.allOf(currentServiceStateRestriction, dateRestriction)).thenReturn(finalRestriction);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        typeQuery.setRestriction(finalRestriction);
        fmFunctionMOService.readNodeListForLongOngoingSync(duration);
        verify(typeQuery).setRestriction(finalRestriction);

    }

    /**
     * Test method for {@link com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService#read(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testRead() {
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(anyString())).thenReturn(managedObject);
        final Object obj = "TestResult";
        when(managedObject.getAttribute("Test")).thenReturn(obj);
        final Object result = fmFunctionMOService.read("FDN", "Test");
        assertEquals(result, obj);

    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService#update(java.lang.String, java.lang.String, boolean)}.
     */
    @Test
    public void testUpdate() {

        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(anyString())).thenReturn(managedObject);
        fmFunctionMOService.update("fdn", "attribute", true);
        verify(managedObject).setAttribute("attribute", true);

    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService#updateCurrentServiceState(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testUpdateCurrentServiceState() {
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(anyString())).thenReturn(managedObject);
        fmFunctionMOService.updateCurrentServiceState("fdn", "newState");
        final Map<String, Object> attributeMap = new HashMap<String, Object>(2);
        attributeMap.put(FM_SUPERVISEDOBJECT_SERVICE_STATE, "newState");
        attributeMap.put(LAST_UPDATED, String.valueOf(System.currentTimeMillis()));
        attributeMap.put(LASTUPDATEDTIMESTAMP, new Date());
        verify(managedObject).setAttributes(Matchers.anyMap());
    }

}

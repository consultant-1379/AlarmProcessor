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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_PROCESSED;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NORMAL_PROC;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OPEN_ALARM;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OSS_FM;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Before;
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
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

@RunWith(MockitoJUnitRunner.class)
public class OpenAlarmServiceTest {

    @InjectMocks
    private OpenAlarmService openAlarmService;

    @Mock
    private ProcessedAlarmEvent alarmEvent;

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
    //
    @Mock
    private QueryExecutor queryExecutor;
    //
    @Mock
    private Restriction restriction;
    //
    @Mock
    private RestrictionBuilder restrictionBuilder;
    //
    @Mock
    private Iterator<Object> poListIterator;
    //
    @Mock
    private PersistenceObject poObject;
    @Mock
    private PersistenceObjectBuilder persistenceObjectBuilder;

    @Mock
    private ManagedObject managedObject;

    @Before
    public void SetUp() {
        alarmEvent = new ProcessedAlarmEvent();

        alarmEvent.setObjectOfReference("MeContext=LTE09ERBS00009");
        alarmEvent.setSpecificProblem("SpecificProblem");
        alarmEvent.setProbableCause("ProbableCause");
        alarmEvent.setEventType("EventType");
        alarmEvent.setRecordType(FMProcessedEventType.ALARM);
        alarmEvent.setAlarmNumber(12345L);
        alarmEvent.setPresentSeverity(ProcessedEventSeverity.CLEARED);
        alarmEvent.setProcessingType(NORMAL_PROC);
        alarmEvent.setVisibility(true);
        alarmEvent.setCorrelatedVisibility(true);
        alarmEvent.setAlarmNumber(12L);
        alarmEvent.setFmxGenerated(FMX_PROCESSED);
        alarmEvent.setEventPOId(123456L);
        alarmEvent.setCorrelatedPOId(12345L);
        alarmEvent.setCeaseOperator(CEASE_OPERATOR);

    }

    @Test
    public void testRemoveAlarm() {
        final Map<String, Object> newAttributes = new HashMap<String, Object>();

        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findPoById(Matchers.anyLong())).thenReturn(poObject);
        openAlarmService.removeAlarm(0, newAttributes);
        verify(liveBucket).deletePo(poObject);

    }

    /**
     * Test method for {@link com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService#updateAlarm(java.lang.Long, java.util.Map)}.
     */
    @Test
    public void testUpdateAlarm() {
        final Map<String, Object> newAttributes = new HashMap<String, Object>();

        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);

        when(liveBucket.findPoById(0)).thenReturn(poObject);
        openAlarmService.updateAlarm((long) 0, newAttributes);
        verify(poObject).setAttributes(Matchers.anyMap());

    }

    /**
     * Test method for {@link com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService#insertAlarmRecord(java.util.Map)}.
     */
    @Test
    public void testInsertAlarmRecord() {
        final Map<String, Object> alarmAttributes = new HashMap<String, Object>();
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findMoByFdn(Matchers.anyString())).thenReturn(managedObject);
        when(liveBucket.getPersistenceObjectBuilder()).thenReturn(persistenceObjectBuilder);
        when(persistenceObjectBuilder.namespace(OSS_FM)).thenReturn(persistenceObjectBuilder);
        when(persistenceObjectBuilder.type(OPEN_ALARM)).thenReturn(persistenceObjectBuilder);
        when(persistenceObjectBuilder.addAttributes(alarmAttributes)).thenReturn(persistenceObjectBuilder);
        when(persistenceObjectBuilder.create()).thenReturn(poObject);
        when(poObject.getPoId()).thenReturn(1234L);

        final long resultFdn = openAlarmService.insertAlarmRecord(alarmAttributes);
        assertEquals(resultFdn, 1234L);
    }

}

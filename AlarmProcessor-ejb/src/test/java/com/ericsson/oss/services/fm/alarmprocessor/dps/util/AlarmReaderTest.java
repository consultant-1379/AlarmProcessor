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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.TREND_INDICATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_NUMBER;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.CEASE_OPERATOR;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.EVENT_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_ALARM_OPERATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OBJECT_OF_REFERENCE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PREVIOUS_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBABLE_CAUSE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.SPECIFIC_PROBLEM;
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
import org.mockito.Mockito;
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
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventTrendIndication;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;
import com.ericsson.oss.services.models.alarm.AlarmRecordType;

@RunWith(MockitoJUnitRunner.class)
public class AlarmReaderTest {

    @InjectMocks
    private AlarmReader alarmReader;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

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

    private Map<String, Object> alarmAttributes;

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

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader#correlateWithSpPcEt(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testCorrelateWithSPPCET() {
        alarmAttributes = new HashMap<String, Object>();
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(serviceProxyProviderBean.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.execute(typeQuery)).thenReturn(poListIterator);
        when(typeRestrictionBuilder.equalTo(PROBABLE_CAUSE, alarmRecord.getProbableCause())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(SPECIFIC_PROBLEM, alarmRecord.getSpecificProblem())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(EVENT_TYPE, alarmRecord.getEventType())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(OBJECT_OF_REFERENCE, alarmRecord.getObjectOfReference())).thenReturn(restriction);

        when(
                restrictionBuilder.allOf((Restriction) Matchers.anyObject(), (Restriction) Matchers.anyObject(), (Restriction) Matchers.anyObject(),
                        (Restriction) Matchers.anyObject())).thenReturn(restriction);
        Mockito.when(poListIterator.hasNext()).thenReturn(true).thenReturn(false);
        when(poListIterator.next()).thenReturn(poObject);

        alarmAttributes.put(RECORD_TYPE, AlarmRecordType.NON_SYNCHABLE_ALARM.name());
        alarmAttributes.put(ALARM_STATE, ProcessedEventState.CLEARED_ACKNOWLEDGED.name());
        alarmAttributes.put(TREND_INDICATION, ProcessedEventTrendIndication.NO_CHANGE.name());
        alarmAttributes.put(PRESENT_SEVERITY, ProcessedEventSeverity.MAJOR.name());
        alarmAttributes.put(PREVIOUS_SEVERITY, ProcessedEventSeverity.WARNING.name());
        alarmAttributes.put(LAST_ALARM_OPERATION, ProcessedLastAlarmOperation.CHANGE.name());
        when(poObject.getAllAttributes()).thenReturn(alarmAttributes);
        alarmReader.correlateWithSpPcEt(alarmRecord);
        verify(queryExecutor).execute(typeQuery);

    }

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.dps.util.AlarmReader#correlateWithAlarmNumber(com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent)}
     * .
     */
    @Test
    public void testCorrelateWithAlarmNumber() {
        alarmAttributes = new HashMap<String, Object>();
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(serviceProxyProviderBean.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.execute(typeQuery)).thenReturn(poListIterator);
        when(typeRestrictionBuilder.equalTo(ALARM_NUMBER, alarmEvent.getAlarmNumber())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(OBJECT_OF_REFERENCE, alarmEvent.getObjectOfReference())).thenReturn(restriction);
        when(restrictionBuilder.allOf((Restriction) Matchers.anyObject(), (Restriction) Matchers.anyObject())).thenReturn(restriction);
        Mockito.when(poListIterator.hasNext()).thenReturn(true).thenReturn(false);
        when(poListIterator.next()).thenReturn(poObject);
        alarmAttributes.put(RECORD_TYPE, AlarmRecordType.ERROR_MESSAGE.name());
        alarmAttributes.put(ALARM_STATE, ProcessedEventState.ACTIVE_UNACKNOWLEDGED.name());
        alarmAttributes.put(TREND_INDICATION, ProcessedEventTrendIndication.MORE_SEVERE.name());
        alarmAttributes.put(PRESENT_SEVERITY, ProcessedEventSeverity.CLEARED.name());
        alarmAttributes.put(PREVIOUS_SEVERITY, ProcessedEventSeverity.MINOR.name());
        alarmAttributes.put(LAST_ALARM_OPERATION, ProcessedEventSeverity.UNDEFINED.name());
        when(poObject.getAllAttributes()).thenReturn(alarmAttributes);
        alarmReader.correlateWithAlarmNumber(alarmRecord);
        verify(queryExecutor).execute(typeQuery);
    }

    @Test
    public void testReadAttribute() {
        // poObject.setTarget(poObject);
        poObject.setAttribute("FDN", "MeContext=LTE09ERBS00009");
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findPoById(1234L)).thenReturn(poObject);
        // when(poObject).thenReturn((PersistenceObject) anyObject());
        when(poObject.getAttribute("FDN")).thenReturn("MeContext=LTE09ERBS00009");
        final Object result = alarmReader.readAttribute(1234L, "FDN");
        assertEquals(result.toString(), "MeContext=LTE09ERBS00009");
    }

    @Test
    public void testReadAllAttributes() {
        poObject.setAttribute("eventPoId", 1234L);
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findPoById(1234L)).thenReturn(poObject);
        when(poObject.getAttribute("eventPoId")).thenReturn(1234L);
        final Object result = alarmReader.readAllAttributes(1234L);
        assertTrue(result.toString(), true);
    }

}

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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FMX_GENERATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_CREATED;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.SYNC_STATE;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OPEN_ALARM;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OSS_FM;

import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.RestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.models.alarm.AlarmRecordType;

@RunWith(MockitoJUnitRunner.class)
public class OpenAlarmSyncStateUpdatorTest {

    @InjectMocks
    private OpenAlarmSyncStateUpdator openAlarmSyncStateUpdator;

    @Mock
    private ProcessedAlarmEvent alarmRecord;

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

    /**
     * Test method for
     * {@link com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmSyncStateUpdator#updateSyncState(java.lang.String, boolean)}.
     */
    @Test
    public void testUpdate() {

        when(alarmRecord.getFdn()).thenReturn("NetworkElement=LTE09ERBS00009");
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(serviceProxyProviderBean.getQueryBuilder()).thenReturn(queryBuilder);
        when(queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM)).thenReturn(typeQuery);
        when(typeQuery.getRestrictionBuilder()).thenReturn(typeRestrictionBuilder);
        when(liveBucket.getQueryExecutor()).thenReturn(queryExecutor);
        when(queryExecutor.execute(typeQuery)).thenReturn(poListIterator);
        when(typeRestrictionBuilder.equalTo(FDN, "NetworkElement=LTE09ERBS00009")).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(RECORD_TYPE, AlarmRecordType.ALARM.name())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(RECORD_TYPE, AlarmRecordType.UPDATE.name())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(RECORD_TYPE, AlarmRecordType.REPEATED_ALARM.name())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(RECORD_TYPE, AlarmRecordType.SYNCHRONIZATION_ALARM.name())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(RECORD_TYPE, AlarmRecordType.TECHNICIAN_PRESENT.name())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(RECORD_TYPE, AlarmRecordType.ALARM_SUPPRESSED_ALARM.name())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(RECORD_TYPE, AlarmRecordType.OUT_OF_SYNC.name())).thenReturn(restriction);
        when(typeRestrictionBuilder.not((Restriction) Matchers.anyObject())).thenReturn(restriction);
        when(typeRestrictionBuilder.equalTo(FMX_GENERATED, FMX_CREATED)).thenReturn(restriction);
        when(typeRestrictionBuilder.not((Restriction) Matchers.anyObject())).thenReturn(restriction);

        when(
                restrictionBuilder.anyOf((Restriction) Matchers.anyObject(), (Restriction) Matchers.anyObject(), (Restriction) Matchers.anyObject(),
                        (Restriction) Matchers.anyObject(), (Restriction) Matchers.anyObject(), (Restriction) Matchers.anyObject(),
                        (Restriction) Matchers.anyObject(), (Restriction) Matchers.anyObject())).thenReturn(restriction);

        when(
                restrictionBuilder.allOf((Restriction) Matchers.anyObject(), (Restriction) Matchers.anyObject(), (Restriction) Matchers.anyObject(),
                        (Restriction) Matchers.anyObject())).thenReturn(restriction);
        Mockito.when(poListIterator.hasNext()).thenReturn(true).thenReturn(false);
        when(poListIterator.next()).thenReturn(poObject);
        openAlarmSyncStateUpdator.updateSyncState(SYNC_STATE, true);
        verify(poObject).setAttribute(SYNC_STATE, true);

    }

    /**
     * Test method for {@link com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmSyncStateUpdator#updateSyncStateForPoId(long, boolean)}.
     */
    @Test
    public void testUpdateForSyncAlarm() {
        when(serviceProxyProviderBean.getLiveBucket()).thenReturn(liveBucket);
        when(liveBucket.findPoById(0)).thenReturn(poObject);

        openAlarmSyncStateUpdator.updateSyncStateForPoId(0, true);

        verify(poObject).setAttribute(anyString(), anyObject());

    }

}

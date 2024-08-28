/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.dps.util;

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_NUMBER;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.EVENT_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.INSERT_TIME;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OBJECT_OF_REFERENCE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBABLE_CAUSE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.SPECIFIC_PROBLEM;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.SYNC_STATE;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OPEN_ALARM;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OSS_FM;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.RestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.SortDirection;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.models.alarm.EventSeverity;

/**
 * Class responsible for reading alarms from database.
 */
public class AlarmReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmReader.class);

    @Inject
    private ServiceProxyProviderBean serviceProxyProviderBean;

    /**
     * Method that retrieves alarms corresponding to a NetworkElement with given fdn and with the given SynsState if applicable.
     * @param String
     *            fdn
     * @param {link
     *        boolean} considerSyncState
     * @param {link
     *        boolean} syncState
     * @return {link Iterator}
     */
    public Iterator<PersistenceObject> readAlarms(final String fdn, final boolean considerSyncState, final boolean syncState) {
        final Iterator<PersistenceObject> iterator = getPersistentObjectIterator(fdn, considerSyncState, syncState);
        return iterator;
    }

    /**
     * Method that Returns Map of all the attributes of the alarm in the DB matching with the given PoId.
     * @param {link
     *        long} eventPoId PoId value of the alarm
     * @return {link Map} allAttributes
     */
    public Map<String, Object> readAllAttributes(final long eventPoId) {
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final PersistenceObject alarmFromDataBase = liveBucket.findPoById(eventPoId);
        Map<String, Object> alarmAttributes = new HashMap<String, Object>();
        if (alarmFromDataBase != null) {
            alarmAttributes = alarmFromDataBase.getAllAttributes();
        }
        return alarmAttributes;
    }

    public Map<String, Object> readAttributes(final Long eventpoId, final List<String> outputAttributes) {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        final Map<String, Object> alarmAttributes = readAllAttributes(eventpoId);
        for (final String outputAttribute : outputAttributes) {
            attributes.put(outputAttribute, alarmAttributes.get(outputAttribute));
        }
        LOGGER.debug("Output attributes obtained : {} and their values are :{}", attributes.keySet(), attributes.values());
        return attributes;
    }

    /**
     * Method returns value of given attribute from an OpenAlarm with the given PoId.
     * @param poId
     *            PoId value of the alarm
     * @param attribute
     *            attribute value
     * @return returns attribute value
     */
    public Object readAttribute(final long poId, final String attribute) {
        Object alarmAttribute = null;
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final PersistenceObject alarmFromDataBase = liveBucket.findPoById(poId);
        if (alarmFromDataBase != null) {
            alarmAttribute = alarmFromDataBase.getAttribute(attribute);
        }
        return alarmAttribute;
    }

    /**
     * Method performs Alarm Correlation with SP,PC,ET and OOR.
     * @param {@link ProcessedAlarmEvent} alarmRecord
     * @return Iterator over Correlated Alarm Objects
     */
    public Iterator<PersistenceObject> correlateWithSpPcEt(final ProcessedAlarmEvent alarmRecord) {
        QueryBuilder queryBuilder;
        Query<TypeRestrictionBuilder> typeQuery;
        QueryExecutor queryExecutor;
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        queryBuilder = serviceProxyProviderBean.getQueryBuilder();
        typeQuery = queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM);
        queryExecutor = liveBucket.getQueryExecutor();
        final RestrictionBuilder restrictionBuilder = typeQuery.getRestrictionBuilder();
        final Restriction pcRestriction = typeQuery.getRestrictionBuilder().equalTo(PROBABLE_CAUSE, alarmRecord.getProbableCause());
        final Restriction spRestriction = typeQuery.getRestrictionBuilder().equalTo(SPECIFIC_PROBLEM, alarmRecord.getSpecificProblem());
        final Restriction etRestriction = typeQuery.getRestrictionBuilder().equalTo(EVENT_TYPE, alarmRecord.getEventType());
        final Restriction oorRestriction = typeQuery.getRestrictionBuilder().equalTo(OBJECT_OF_REFERENCE, alarmRecord.getObjectOfReference());
        final Restriction restriction = restrictionBuilder.allOf(oorRestriction, pcRestriction, spRestriction, etRestriction);
        typeQuery.setRestriction(restriction);
        typeQuery.addSortingOrder(INSERT_TIME, SortDirection.DESCENDING);
        final Iterator<PersistenceObject> correlatedAlarmIterator = queryExecutor.execute(typeQuery);
        return correlatedAlarmIterator;
    }

    /**
     * Method performs Alarm correlation with AlarmNumber and OOR.
     * @param {@link ProcessedAlarmEvent} alarmRecord for which Correlation should be performed.
     * @return Iterator over Correlated Alarm Objects
     */
    public Iterator<PersistenceObject> correlateWithAlarmNumber(final ProcessedAlarmEvent alarmRecord) {
        QueryBuilder queryBuilder;
        Query<TypeRestrictionBuilder> typeQuery;
        QueryExecutor queryExecutor;
        queryBuilder = serviceProxyProviderBean.getQueryBuilder();
        typeQuery = queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM);
        final RestrictionBuilder restrictionBuilder = typeQuery.getRestrictionBuilder();
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        queryExecutor = liveBucket.getQueryExecutor();
        final Restriction alarmNumberRestriction = typeQuery.getRestrictionBuilder().equalTo(ALARM_NUMBER, alarmRecord.getAlarmNumber());
        final Restriction oorRestriction = typeQuery.getRestrictionBuilder().equalTo(OBJECT_OF_REFERENCE, alarmRecord.getObjectOfReference());
        final Restriction restriction = restrictionBuilder.allOf(alarmNumberRestriction, oorRestriction);
        typeQuery.setRestriction(restriction);
        typeQuery.addSortingOrder(INSERT_TIME, SortDirection.DESCENDING);
        final Iterator<PersistenceObject> correlatedAlarmIterator = queryExecutor.execute(typeQuery);
        return correlatedAlarmIterator;
    }

    private Iterator<PersistenceObject> getPersistentObjectIterator(final String fdn, final boolean considerSyncState, final boolean syncState) {
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final QueryBuilder queryBuilder = serviceProxyProviderBean.getQueryBuilder();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM);
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final RestrictionBuilder restrictionBuilder = typeQuery.getRestrictionBuilder();
        Restriction restriction = typeQuery.getRestrictionBuilder().equalTo(FDN, fdn);
        if (considerSyncState) {
            final Restriction syncStateRestriction = typeQuery.getRestrictionBuilder().equalTo(SYNC_STATE, syncState);
            restriction = restrictionBuilder.allOf(restriction, syncStateRestriction);
        }
        typeQuery.setRestriction(restriction);
        return queryExecutor.execute(typeQuery);
    }

    public Long getMatchedHbAlarms(final ProcessedAlarmEvent alarmRecord) {
        LOGGER.debug("fetching existin HB alarms against {}", alarmRecord.getFdn());
        final String fdn = alarmRecord.getFdn();
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final QueryBuilder queryBuilder = serviceProxyProviderBean.getQueryBuilder();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM);
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final RestrictionBuilder restrictionBuilder = typeQuery.getRestrictionBuilder();
        final Restriction fdnRestriction = typeQuery.getRestrictionBuilder().equalTo(FDN, fdn);
        final Restriction recordTypeRestriction = typeQuery.getRestrictionBuilder().equalTo(RECORD_TYPE, alarmRecord.getRecordType().name());

        final Restriction severityRestriction = typeQuery.getRestrictionBuilder().equalTo(PRESENT_SEVERITY, EventSeverity.CRITICAL.name());
        final Restriction finalRestriction = restrictionBuilder.allOf(recordTypeRestriction, fdnRestriction, severityRestriction);
        typeQuery.setRestriction(finalRestriction);
        final Long hbAlarmCount = queryExecutor.executeCount(typeQuery);
        return hbAlarmCount;
    }
}

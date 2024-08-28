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

package com.ericsson.oss.services.fm.alarmprocessor.dps.util;

import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.SYNC_STATE;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OPEN_ALARM;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OSS_FM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;

/**
 * Class that deals with Open Alarm add,remove & update to db concerns.
 */
public class OpenAlarmService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAlarmService.class);

    @Inject
    private ServiceProxyProviderBean serviceProxyProviderBean;

    /**
     * Method that updates alarm and then removes the alarm from database.
     * @param long poId
     * @param Map
     *            alarmAttributes
     */
    public void removeAlarm(final long poId, final Map<String, Object> alarmAttributes) {
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final PersistenceObject alarmFromDatabase = liveBucket.findPoById(poId);
        LOGGER.debug("Removing alarm record with poId : {} and attributes : {}", poId, alarmAttributes);
        if (alarmFromDatabase != null) {
            alarmFromDatabase.setAttributes(alarmAttributes);
            liveBucket.deletePo(alarmFromDatabase);
        }
    }

    /**
     * Method that updates an already existing Alarm PersistenceObject with new attributes.
     * @param Long
     *            poId
     * @param Map
     *            newAttributes
     */
    public void updateAlarm(final Long poId, final Map<String, Object> newAttributes) {
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final PersistenceObject alarmFromDatabase = liveBucket.findPoById(poId);
        newAttributes.put(SYNC_STATE, true);
        if (alarmFromDatabase != null) {
            alarmFromDatabase.setAttributes(newAttributes);
            LOGGER.debug("New attributes: {} and saved alarm attributes : {}", newAttributes, alarmFromDatabase.getAllAttributes());
        } else {
            LOGGER.info("There exists no PO with POId: {} in database.Attributes: {} is not updated.", poId, newAttributes);
        }
    }

    /**
     * Method inserts alarm in Database.
     * @param String
     *            neFdn
     * @param Map
     *            alarmAttributes --alarm attributes map
     * @return {@link Long} returns eventPoID
     */
    public Long insertAlarmRecord(final Map<String, Object> alarmAttributes) {
        long eventId = AlarmProcessorConstants.DEFAULT_EVENTPOID_VALUE;
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final PersistenceObject alarmFromDatabase =
                liveBucket.getPersistenceObjectBuilder().namespace(OSS_FM).type(OPEN_ALARM).addAttributes(alarmAttributes).create();
        eventId = alarmFromDatabase.getPoId();
        return eventId;
    }

    /**
     * Method gets all the open alarms for given matching attributes. Attributes with single value and multiple values can be provided as criteria.
     * @param singleValuedAttributes
     *            attributes having single value.
     * @param multipleValuedAttributes
     *            attributes having multiple values.
     * @return Open Alarm PO Iterator.
     */
    public Iterator<PersistenceObject> getOpenAlarmPO(final Map<String, Object> singleValuedAttributes,
            final Map<String, List<String>> multipleValuedAttributes) {
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final QueryBuilder queryBuilder = serviceProxyProviderBean.getDataPersistenceService().getQueryBuilder();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM);
        final List<Restriction> restrictions = new ArrayList<Restriction>();

        // Iterating and forming IN restriction for attributes having multiple values.
        for (final Entry<String, List<String>> attributeCriterion : multipleValuedAttributes.entrySet()) {
            final String attributeName = attributeCriterion.getKey();
            final List<String> attributeValue = attributeCriterion.getValue();
            if (attributeValue != null && !attributeValue.isEmpty()) {
                restrictions.add(typeQuery.getRestrictionBuilder().in(attributeName, attributeValue.toArray()));
            }
        }

        // Iterating and forming equal restriction for attributes having single values.
        for (final Entry<String, Object> attributeCriterion : singleValuedAttributes.entrySet()) {
            final String attributeName = attributeCriterion.getKey();
            final Object attributeValue = attributeCriterion.getValue();

            if (attributeValue != null) {
                restrictions.add(typeQuery.getRestrictionBuilder().equalTo(attributeName, attributeValue));
            }
        }
        final Restriction finalRestriction = typeQuery.getRestrictionBuilder().allOf(restrictions.toArray(new Restriction[restrictions.size()]));
        typeQuery.setRestriction(finalRestriction);
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final Iterator<PersistenceObject> poListIterator = queryExecutor.execute(typeQuery);
        return poListIterator;
    }
}

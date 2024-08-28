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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_SUPPRESSED;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_SUPPRESSED_STATE;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LASTUPDATEDTIMESTAMP;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.LAST_UPDATED;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FMFUNCTION_SUFFIX;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_FUNCTION;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OSS_NE_FM_DEF;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.RestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

/**
 * Class that deals with managed object FmFunction concerns.
 */
public class FmFunctionMoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FmFunctionMoService.class);

    @Inject
    private ServiceProxyProviderBean serviceProxyProviderBean;

    /**
     * Method returns the list of FmFunction fdns whose currentServiceState is SYNC_ONGOING for more than given duration.
     * @param Integer
     *            duration
     * @return List of FmFunction FDNs
     */
    public List<String> readNodeListForLongOngoingSync(final Integer duration) {
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final QueryBuilder queryBuilder = serviceProxyProviderBean.getQueryBuilder();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(OSS_NE_FM_DEF, FM_FUNCTION);
        final RestrictionBuilder restrictionBuilder = typeQuery.getRestrictionBuilder();
        final Restriction currentServiceStateRestriction = typeQuery.getRestrictionBuilder().equalTo(FM_SUPERVISEDOBJECT_SERVICE_STATE,
                FmSyncStatus100.SYNC_ONGOING.name());
        final Date currentTime = new Date();
        final long milliseconds = currentTime.getTime() - duration * 60 * 1000;
        final Date pastTime = new Date(milliseconds);
        final Restriction lessThanDateRestriction = typeQuery.getRestrictionBuilder().lessThan(LASTUPDATEDTIMESTAMP, pastTime);
        final Restriction equalToDateRestriction = typeQuery.getRestrictionBuilder().equalTo(LASTUPDATEDTIMESTAMP, pastTime);
        final Restriction dateRestriction = restrictionBuilder.anyOf(lessThanDateRestriction, equalToDateRestriction);
        final Restriction finalRestriction = restrictionBuilder.allOf(currentServiceStateRestriction, dateRestriction);
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final Projection fdnProjection = ProjectionBuilder.field(ObjectField.MO_FDN);
        typeQuery.setRestriction(finalRestriction);
        final List<String> fdnList = queryExecutor.executeProjection(typeQuery, fdnProjection);
        LOGGER.debug("NetworkElements with long Ongoing Alarm Synchronization are: {}", fdnList);
        return new ArrayList<>(fdnList);
    }

    /**
     * Method reads given attribute value of FMFunctionMO for the given fdn.
     * @param String
     *            fdn
     * @param String
     *            attributeName
     * @return Object The attribute value
     */
    public Object read(final String fdn, final String attributeName) {
        Object value = null;
        if (fdn != null) {
            final String fmFunctionManagedObjectFdn = fdn.concat(FMFUNCTION_SUFFIX);
            final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
            final ManagedObject managedObject = liveBucket.findMoByFdn(fmFunctionManagedObjectFdn);
            if (managedObject != null) {
                value = managedObject.getAttribute(attributeName);
            }
            LOGGER.debug("Attribute: {} for fdn: {} is: {}", attributeName, fdn, value);
        }
        return value;
    }

    /**
     * Method updates attribute of FmFunctionMO with the given value for a NetworkElement with given fdn.
     * @param String
     *            fdn
     * @param String
     *            attribute
     * @param boolean value
     */
    public void update(final String fdn, final String attribute, final boolean value) {
        final String fmFunctionFdn = fdn.concat(FMFUNCTION_SUFFIX);
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final ManagedObject functionMo = liveBucket.findMoByFdn(fmFunctionFdn);
        if (functionMo != null) {
            functionMo.setAttribute(attribute, value);
        }
        LOGGER.debug(": {} set to : {} for fdn :{} ", attribute, value, fdn);
    }

    /**
     * Method updates currentServiceState of a FmFunctionMO with the given fdn.
     * @param String
     *            fdn
     * @param String
     *            newState
     */
    public void updateCurrentServiceState(final String fdn, final String newState) {
        final String fmFunctionFdn = fdn.concat(FMFUNCTION_SUFFIX);
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final ManagedObject functionMo = liveBucket.findMoByFdn(fmFunctionFdn);
        final Map<String, Object> attributeMap = new HashMap<>(3);
        attributeMap.put(FM_SUPERVISEDOBJECT_SERVICE_STATE, newState);
        attributeMap.put(LAST_UPDATED, String.valueOf(System.currentTimeMillis()));
        attributeMap.put(LASTUPDATEDTIMESTAMP, new Date());
        if (ALARM_OVERLOAD_PROTECTION_SUPPRESSED.equals(newState)) {
            attributeMap.put(ALARM_OVERLOAD_PROTECTION_SUPPRESSED_STATE, true);
        }
        functionMo.setAttributes(attributeMap);
        LOGGER.debug("currentServicestate set to: {} for fdn: {} ", newState, fdn);
    }
}

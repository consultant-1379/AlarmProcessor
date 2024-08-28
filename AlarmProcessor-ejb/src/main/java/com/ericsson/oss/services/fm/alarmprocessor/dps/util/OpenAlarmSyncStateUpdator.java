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

import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FMX_GENERATED;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_CREATED;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.SYNC_STATE;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OPEN_ALARM;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OSS_FM;

import java.util.Iterator;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.*;
import com.ericsson.oss.services.models.alarm.AlarmRecordType;
import com.ericsson.oss.services.models.alarm.EventSeverity;

/**
 * Class for updating sync state of alarm in database.
 */
public class OpenAlarmSyncStateUpdator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAlarmSyncStateUpdator.class);

    @Inject
    private ServiceProxyProviderBean serviceProxyProviderBean;

    /**
     * Method updates syncState attribute of all OpenAlarms present in the database for a given NetworkElement with the given fdn.
     * <p>
     * Sync state will be updated for the Alarms with only certain Record types(ALARM , UPDATE , CLEARALL , REPEATED_ALARM ,
     * SYNCHRONIZATION_ALARMTECHNICIAN_PRESENT, ALARM_SUPPRESSED_ALARM ,OUT_OF_SYNC) and whose severity is not cleared
     * @param String
     *            fdn
     * @param boolean value -Value of sync state attribute(true/false)
     */
    public void updateSyncState(final String fdn, final boolean value) {
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final QueryBuilder queryBuilder = serviceProxyProviderBean.getQueryBuilder();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM);
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();
        final RestrictionBuilder restrictionBuilder = typeQuery.getRestrictionBuilder();
        final Restriction restriction = typeQuery.getRestrictionBuilder().equalTo(FDN, fdn);
        final Restriction normalAlarm = typeQuery.getRestrictionBuilder().equalTo(RECORD_TYPE, AlarmRecordType.ALARM.name());
        final Restriction updateAlarm = typeQuery.getRestrictionBuilder().equalTo(RECORD_TYPE, AlarmRecordType.UPDATE.name());
        final Restriction clearAllAlarm = typeQuery.getRestrictionBuilder().equalTo(RECORD_TYPE, AlarmRecordType.CLEARALL.name());
        final Restriction repeatedAlarm = typeQuery.getRestrictionBuilder().equalTo(RECORD_TYPE, AlarmRecordType.REPEATED_ALARM.name());
        final Restriction syncAlarm = typeQuery.getRestrictionBuilder().equalTo(RECORD_TYPE, AlarmRecordType.SYNCHRONIZATION_ALARM.name());
        final Restriction technicianPresentAlarm = typeQuery.getRestrictionBuilder().equalTo(RECORD_TYPE, AlarmRecordType.TECHNICIAN_PRESENT.name());
        final Restriction oscillatoryHbAlarm = typeQuery.getRestrictionBuilder().equalTo(RECORD_TYPE, AlarmRecordType.OSCILLATORY_HB_ALARM.name());
        final Restriction alarmSuppressedAlarm = typeQuery.getRestrictionBuilder()
                .equalTo(RECORD_TYPE, AlarmRecordType.ALARM_SUPPRESSED_ALARM.name());
        final Restriction outOfSyncdAlarm = typeQuery.getRestrictionBuilder().equalTo(RECORD_TYPE, AlarmRecordType.OUT_OF_SYNC.name());
        final Restriction hearbeatAlarm = typeQuery.getRestrictionBuilder().equalTo(RECORD_TYPE, AlarmRecordType.HEARTBEAT_ALARM.name());
        final Restriction recordType = restrictionBuilder.anyOf(normalAlarm, repeatedAlarm, syncAlarm, technicianPresentAlarm,
                alarmSuppressedAlarm, outOfSyncdAlarm, clearAllAlarm, updateAlarm, hearbeatAlarm, oscillatoryHbAlarm);
        final Restriction clearAlarmRestriction = typeQuery.getRestrictionBuilder().equalTo(PRESENT_SEVERITY, EventSeverity.CLEARED.name());
        final Restriction notClearAlarm = restrictionBuilder.not(clearAlarmRestriction);
        final Restriction fmxCreatedAlarm = typeQuery.getRestrictionBuilder().equalTo(FMX_GENERATED, FMX_CREATED);
        final Restriction fmxCreatedAlarmAdditionalInformation = typeQuery.getRestrictionBuilder().matchesString("additionalInformation","fmxCreatedAlarm",StringMatchCondition.CONTAINS);
        final Restriction fmxCreatedAlarmAnyOffRestriction = restrictionBuilder.anyOf(fmxCreatedAlarm,fmxCreatedAlarmAdditionalInformation);
        final Restriction nonFmxCreatedAlarm = restrictionBuilder.not(fmxCreatedAlarmAnyOffRestriction);

        final Restriction restrictions = restrictionBuilder.allOf(restriction, recordType, notClearAlarm, nonFmxCreatedAlarm);
        typeQuery.setRestriction(restrictions);

        final Iterator<PersistenceObject> iterator = queryExecutor.execute(typeQuery);
        while (iterator != null && iterator.hasNext()) {
            final PersistenceObject po = iterator.next();
            po.setAttribute(SYNC_STATE, value);
            LOGGER.debug("PO : {} after setting synch state to : {}", po.getAllAttributes().entrySet(), value);
        }
    }

    /**
     * Method updates syncState to true/false for an alarm with given poId.
     * @param long poId
     * @param boolean value
     */
    public void updateSyncStateForPoId(final long poId, final boolean value) {
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final PersistenceObject alarmFromDatabase = liveBucket.findPoById(poId);
        if (alarmFromDatabase != null) {
            alarmFromDatabase.setAttribute(SYNC_STATE, value);
        }
    }

}
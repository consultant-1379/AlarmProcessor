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

import javax.enterprise.context.ApplicationScoped;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.fm.alarmprocessor.api.alarmsender.AlarmSender;

/**
 * An ApplicationScoped bean, for providing a DPS instance and create/get AlarmSender object for sending alarm to FMX services. .
 */
@ApplicationScoped
public class ServiceProxyProviderBean {

    @EServiceRef
    private DataPersistenceService dps;

    @EServiceRef
    private AlarmSender alarmSender;

    public DataPersistenceService getDataPersistenceService() {
        return dps;
    }

    public AlarmSender getAlarmSender() {
        return alarmSender;
    }

    public DataBucket getLiveBucket() {
        return dps.getLiveBucket();
    }

    public QueryBuilder getQueryBuilder() {
        return dps.getQueryBuilder();
    }
}
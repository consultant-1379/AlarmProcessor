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

import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;

/**
 * Class reads networkElementMO for the given fdn.
 */
public class NetworkElementMoReader {

    @Inject
    private ServiceProxyProviderBean serviceProxyProviderBean;

    /**
     * Method reads networkElementMO for the given fdn.
     * @param String
     *            fdn
     * @return Object managedObject
     */
    public Object read(final String fdn) {
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final ManagedObject managedObject = liveBucket.findMoByFdn(fdn);
        return managedObject;
    }
}
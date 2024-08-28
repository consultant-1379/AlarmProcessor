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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.OSSPREFIX;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.NetworkElementMoReader;

/**
 * Retrieves OssPrefix value configured in the database for a given NetworkElement and stores locally in a map.
 *
 *
 */
@ApplicationScoped
public class OssPrefixHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OssPrefixHolder.class);

    private final Map<String, String> neFdnOssPrefixMap = new HashMap<String, String>();

    @Inject
    private NetworkElementMoReader networkElementMoReader;

    public String getOssPrefix(final String fdn) {
        String ossPrefix = neFdnOssPrefixMap.get(fdn);
        if (ossPrefix == null) {
            ossPrefix = fetchOssPrefixFromDbAndStore(fdn);
        }
        return ossPrefix;
    }

    /**
     * Retrieves OssPrefix from Database for a NetworkElement and stores it in local memory if the value returned is not null.
     *
     * @param String
     *            fdn of NetworkElement
     */
    private String fetchOssPrefixFromDbAndStore(final String fdn) {
        String ossPrefix = null;
        final Object object = networkElementMoReader.read(fdn);
        if (object != null) {
            final ManagedObject managedObject = (ManagedObject) object;
            final Map<String, Object> attributes = managedObject.getAllAttributes();
            if (attributes.get(OSSPREFIX) != null) {
                ossPrefix = (String) attributes.get(OSSPREFIX);
                neFdnOssPrefixMap.put(fdn, ossPrefix);
            }
        }
        LOGGER.info("OssPrefix retained from database for {} is:{}", fdn , ossPrefix);
        return ossPrefix;
    }
}

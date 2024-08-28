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

import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.ALARM_SUPERVISION_STATE;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.AUTOMATIC_SYNCHRONIZATION;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FMSUPERVISION_SUFFIX;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;

/**
 * Class for reading FMSupervisionMO from the database.
 */
public class FmSupervisionMoReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FmSupervisionMoReader.class);

    @Inject
    private ServiceProxyProviderBean serviceProxyProviderBean;

    /**
     * Method reads attribute values from FMSupervision MO.
     * @param String
     *            fdn
     * @param String
     *            attributeName
     * @return Object attribute value
     */
    public Object read(final String fdn, final String attributeName) {
        Object attributeValue = null;
        if (fdn != null) {
            final String supervisionManagedObjectFdn = fdn.concat(FMSUPERVISION_SUFFIX);
            final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
            final ManagedObject managedObject = liveBucket.findMoByFdn(supervisionManagedObjectFdn);
            if (managedObject != null) {
                attributeValue = managedObject.getAttribute(attributeName);
            }
            LOGGER.debug("Attribute : {} for fdn: {} is :{}", attributeName, fdn, attributeValue);
        }
        return attributeValue;
    }

    /**
     * Method returns alarmSupervisionState, automaticSynchronization attributes from FmAlarmSupervision ManagedObject.
     * @param String
     *            fdn
     * @return Map of alarmSupervisionState,automaticSynchronization attributes and their values
     */
    public Map<String, Boolean> readSupervisionAndAutoSyncAttributes(final String fdn) {
        final Map<String, Boolean> supervisionAndAutosyncAttributes = new HashMap<String, Boolean>(2);
        supervisionAndAutosyncAttributes.put(AUTOMATIC_SYNCHRONIZATION, false);
        supervisionAndAutosyncAttributes.put(ALARM_SUPERVISION_STATE, false);
        final Map<String, Object> supervisionManagedObjectAttributes = readAttributes(fdn.concat(FMSUPERVISION_SUFFIX));
        if (supervisionManagedObjectAttributes != null) {
            final Object alarmSupervisionState = supervisionManagedObjectAttributes.get(ALARM_SUPERVISION_STATE);
            if (alarmSupervisionState != null) {
                supervisionAndAutosyncAttributes.put(ALARM_SUPERVISION_STATE, (boolean) alarmSupervisionState);
            }
            final Object autoSyncState = supervisionManagedObjectAttributes.get(AUTOMATIC_SYNCHRONIZATION);
            if (autoSyncState != null) {
                supervisionAndAutosyncAttributes.put(AUTOMATIC_SYNCHRONIZATION, (boolean) autoSyncState);
            }
        }
        return supervisionAndAutosyncAttributes;
    }

    /**
     * Method returns all attributes for a ManagedElement with given fdn.
     * @param String
     *            fdn of the MO whose attributes is to be retrieved.
     * @return Map of all attributes of MO if MO exists return null if specified MO doesn't exists
     */
    private Map<String, Object> readAttributes(final String fdn) {
        Map<String, Object> managedObjectAttributes = null;
        final DataBucket liveBucket = serviceProxyProviderBean.getLiveBucket();
        final ManagedObject managedObject = liveBucket.findMoByFdn(fdn);
        if (managedObject != null) {
            managedObjectAttributes = managedObject.getAllAttributes();
        }
        LOGGER.debug("Attributes for fdn : {} is : {}", fdn, managedObjectAttributes);
        return managedObjectAttributes;
    }
}
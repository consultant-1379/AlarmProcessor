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

package com.ericsson.oss.services.fm.alarmprocessor.alarmsync;

import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.createNodeSearchCriteria;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;
import static com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100.IN_SERVICE;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.AlarmSupervisionController;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.FmCliCommand;
import com.ericsson.oss.services.fm.services.alarmsupervisioncontroller.api.NodesSearchCriteriaData;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;

/**
 * Class responsible for initiating alarm synchronization for a NetworkElement with given fdn.
 */
@Singleton
@Lock(LockType.READ)
public class SyncInitiator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncInitiator.class);

    @EServiceRef
    private AlarmSupervisionController alarmSupervisionController;

    @Inject
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    /**
     * Method that initiates alarm Synchronization for the NetworkElement with the given fdn.
     * @param fdn the FDN.
     */
    @Asynchronous
    public void initiateAlarmSynchronization(final String fdn) {
        final FmCliCommand fmCliCommand = new FmCliCommand();
        final NodesSearchCriteriaData nodesSearchCriteriaData = createNodeSearchCriteria(fdn);
        final List<NodesSearchCriteriaData> searchCriteriaList = new ArrayList<>();
        searchCriteriaList.add(nodesSearchCriteriaData);
        fmCliCommand.setSearchCriteria(searchCriteriaList);
        fmCliCommand.setAttributeName(FM_SUPERVISEDOBJECT_SERVICE_STATE);
        initiateSynchronization(fmCliCommand);
    }

    /**
     * Initiates alarm synchronization for given NetworkElements.
     * @param {@link Set} neFdns.
     */
    public void initiateAlarmSync(final Set<String> neFdns) {
        final Iterator<String> fdnIterator = neFdns.iterator();
        final FmCliCommand fmCliCommand = new FmCliCommand();
        final List<NodesSearchCriteriaData> searchCriteriaList = new ArrayList<>();
        while (fdnIterator.hasNext()) {
            String neFdn = fdnIterator.next();
            if(isSyncToBeInitiated(neFdn)) {
                final NodesSearchCriteriaData nodesSearchCriteriaData = createNodeSearchCriteria(neFdn);
                searchCriteriaList.add(nodesSearchCriteriaData);
            }
        }
        if (!searchCriteriaList.isEmpty()) {
            fmCliCommand.setSearchCriteria(searchCriteriaList);
            fmCliCommand.setAttributeName(FM_SUPERVISEDOBJECT_SERVICE_STATE);
            initiateSynchronization(fmCliCommand);
        }
        try{
            clearAlarmsCacheManager.removeFdnFromCache(neFdns);
        }catch(IllegalStateException exception){
            LOGGER.error("Exception occured while removing FDNs {} from clearAlarmsCache.", neFdns, exception);
        }
    }
    /**
     * Method that initiates alarm Synchronization with the given FmCliCommand.
     * @param fmCliSyncCommand the CLi sync comamnd.
     */
    private void initiateSynchronization(final FmCliCommand fmCliSyncCommand) {
        LOGGER.info("Initiating sync with command: {}.", fmCliSyncCommand);
        alarmSupervisionController.initiateSync(fmCliSyncCommand, false);
    }

    private boolean isSyncToBeInitiated(String fdn) {
        boolean isSync = false;
        final String currentServiceState = (String) fmFunctionMoService.read(fdn, FM_SUPERVISEDOBJECT_SERVICE_STATE);
        if(currentServiceState != null && IN_SERVICE.name().equals(currentServiceState)) {
            isSync = true;
        } else {
            LOGGER.info("Sync is skipped for orphan clear alarm as Node {} is not in service state", fdn);
        }
        return isSync;
    }

}

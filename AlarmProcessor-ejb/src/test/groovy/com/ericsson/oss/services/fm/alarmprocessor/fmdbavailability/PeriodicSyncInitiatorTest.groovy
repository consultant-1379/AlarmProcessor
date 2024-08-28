/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability

import com.ericsson.cds.cdi.support.providers.custom.sfwk.PropertiesForTest
import com.ericsson.cds.cdi.support.rule.ImplementationInstance
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService
import com.ericsson.oss.services.fm.alarmprocessor.cluster.AlarmProcessorCluster
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor

class PeriodicSyncInitiatorTest extends AbstractBaseSpec{

    @ObjectUnderTest
    PeriodicSyncInitiator periodicSyncInitiator

    @ImplementationInstance
    AlarmProcessorCluster alarmProcessorCluster = [ sendClusterMessage : { x->
        }] as AlarmProcessorCluster

    @ImplementationInstance
    MembershipChangeProcessor MembershipChangeProcessor =[getMasterState :{true}]as MembershipChangeProcessor

    @MockedImplementation
    DataPersistenceService dataPersistenceService

    @MockedImplementation
    FmDatabaseAvailabilityHandler fmDatabaseAvailabilityHandler

    @PropertiesForTest(propertyFile = "alternative.properties")
    def "periodic sync initiate timer test with database readonly mode as true "() {
        given :" "
        System.setProperty("com.ericsson.oss.sdk.node.identifier","test")
        when: "timeout is triggered"
        periodicSyncInitiator.timeOut()
        then: "assert that sync was not initiated"
        0 * fmDatabaseAvailabilityHandler.checkCacheAndInitiateSync()
    }

    def "periodic sync initiate timer test with database readonly mode as false "() {
        given :" "
        System.setProperty("com.ericsson.oss.sdk.node.identifier","test")
        when: "timeout is triggered"
        periodicSyncInitiator.timeOut()
        then: "assert that sync was initiated"
        1*fmDatabaseAvailabilityHandler.checkCacheAndInitiateSync()
    }
}

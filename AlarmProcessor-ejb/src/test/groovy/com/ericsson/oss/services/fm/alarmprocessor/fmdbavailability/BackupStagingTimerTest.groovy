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
import com.ericsson.oss.services.fm.alarmprocessor.alarm.staging.BackupStagingTimer
import com.ericsson.oss.services.fm.alarmprocessor.alarm.staging.TransientAlarmStagingCacheManager
import com.ericsson.oss.services.fm.alarmprocessor.cluster.AlarmProcessorCluster
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor

class BackupStagingTimerTest extends AbstractBaseSpec {

    @ObjectUnderTest
    BackupStagingTimer backupStagingTimer

    @ImplementationInstance
    AlarmProcessorCluster alarmProcessorCluster = [sendClusterMessage: { x ->
    }] as AlarmProcessorCluster

    @ImplementationInstance
    MembershipChangeProcessor MembershipChangeProcessor = [getMasterState: { true }] as MembershipChangeProcessor

    @MockedImplementation
    TransientAlarmStagingCacheManager transientAlarmStagingCacheManager

    @PropertiesForTest(propertyFile = "alternative.properties")
    def "periodic sync initiate timer test with database readonly mode as true "() {
        given: " "
        System.setProperty("com.ericsson.oss.sdk.node.identifier", "test")
        when: "timeout is triggered"
        backupStagingTimer.checkMasterAndUnstageAlarms()
        then: "assert that no alarms were left in the cache"
        1 * transientAlarmStagingCacheManager.iterator()
    }
}

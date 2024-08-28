/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.fm.alarmprocessor.protection

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.DataBucket
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.datalayer.dps.query.Query
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction
import com.ericsson.oss.itpf.datalayer.dps.query.RestrictionBuilder
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.datalayer.dps.stub.object.StubbedPersistenceObject
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager
import com.ericsson.oss.services.fm.alarmactionservice.cache.AlarmActionsCacheManager
import com.ericsson.oss.services.fm.alarmactionservice.timer.AutoAckRetryTimerHandler
import com.ericsson.oss.services.fm.alarmactionservice.timer.CommentPurgingTimerService
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService
import com.ericsson.oss.services.fm.alarmprocessor.protection.AlarmOverloadProtectionService
import com.ericsson.oss.services.fm.alarmprocessor.util.InternalAlarmGenerator
import com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdCrossed

import javax.inject.Inject

import static com.ericsson.oss.services.fm.alarmactionservice.util.AlarmActionConstants.ALARMNUMBER
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_SUPPRESSED
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.ADDITIONAL_TEXT
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.EVENT_TYPE
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.OBJECT_OF_REFERENCE
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PRESENT_SEVERITY
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.PROBABLE_CAUSE
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.RECORD_TYPE
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.SPECIFIC_PROBLEM
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OPEN_ALARM
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OSS_FM

class ThresholdCrossingImplSpec extends CdiSpecification {

    @ObjectUnderTest
    ThresholdCrossingImpl thresholdCrossingImpl

    @Inject
    AlarmOverloadProtectionService protectionService

    @Inject
    FmFunctionMoService fmFunctionMoService

    @MockedImplementation
    InternalAlarmGenerator internalAlarmGenerator

    @MockedImplementation
    AutoAckRetryTimerHandler autoAckRetryTimerHandler

    @MockedImplementation
    CommentPurgingTimerService commentPurgingTimerService

    @MockedImplementation
    AlarmActionsCacheManager alarmActionsCacheManager

    @MockedImplementation
    MembershipChangeProcessor membershipChangeProcessor

    @MockedImplementation
    RetryManager retryManager

    @Override
    Object addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.autoLocateFrom('com.ericsson.oss.services.fm.alarmactionservice')
        injectionProperties.autoLocateFrom('com.ericsson.oss.services.fm.services.alarmsupervisioncontroller')
    }

    def setup() {
        createNode()
        membershipChangeProcessor.getMasterState() >> true
    }

    def createNode() {
        RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        DataBucket liveBucket = runtimeDps.build().getLiveBucket()
        final ManagedObject radioNode = liveBucket.getMibRootBuilder().namespace("OSS_NE_DEF").version("2.0.0").name("RadioNode").type("NetworkElement").create()
        final Map<String, Object> targetAttributesRadioNode = new HashMap<String, Object>()
        targetAttributesRadioNode.put("category", "NODE")
        targetAttributesRadioNode.put("type", "RadioNode")
        targetAttributesRadioNode.put("name", "NetworkElement=RadioNode")
        targetAttributesRadioNode.put("modelIdentity", "1294-439-662")
        final PersistenceObject targetPoRadioNode = runtimeDps.addPersistenceObject().namespace("DPS").type("Target").addAttributes(targetAttributesRadioNode).build()
        radioNode.setTarget(targetPoRadioNode)
        final ManagedObject managedElement = liveBucket.getMibRootBuilder().namespace("OSS_NE_FM_DEF").type("FmFunction").parent(radioNode).addAttribute("currentServiceState", "IN_SERVICE").create()
    }

    def createOverloadProtectionAlert(final String et, final String pc, final String sp, final String pt, final String ps,
                                      final String rt, final String oor, final String fdn, final long an) {
        final Map<String, Object> alarmAttributes = new HashMap<String, Object>()
        alarmAttributes.put(EVENT_TYPE, et)
        alarmAttributes.put(PROBABLE_CAUSE, pc)
        alarmAttributes.put(SPECIFIC_PROBLEM, sp)
        alarmAttributes.put(ADDITIONAL_TEXT, pt)
        alarmAttributes.put(PRESENT_SEVERITY, ps)
        alarmAttributes.put(RECORD_TYPE, rt)
        alarmAttributes.put(OBJECT_OF_REFERENCE, oor)
        alarmAttributes.put(FDN, fdn)
        alarmAttributes.put(ALARMNUMBER, an)
        RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        DataBucket liveBucket = runtimeDps.build().getLiveBucket()
        final StubbedPersistenceObject persistenceObject =
                liveBucket.getPersistenceObjectBuilder().namespace(OSS_FM).type(OPEN_ALARM).addAttributes(alarmAttributes).create()
        println("poId = " + persistenceObject.getPoId())
    }

    long getPoId(final String sp, final String pc, final String et, final String oor) {
        final String regEexp = "[^a-zA-Z0-9@#%~_+\\-=\\[\\]{}*\$^\\s,.:;!&()<>!`]"
        RuntimeConfigurableDps runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        DataBucket liveBucket = runtimeDps.build().getLiveBucket()
        final QueryBuilder queryBuilder = runtimeDps.build().getQueryBuilder()
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM)
        final RestrictionBuilder restrictionBuilder = typeQuery.getRestrictionBuilder()
        final Restriction restrictionSP = restrictionBuilder.equalTo(SPECIFIC_PROBLEM, sp)
        final Restriction restrictionPC = restrictionBuilder.equalTo(PROBABLE_CAUSE, pc)
        final Restriction restrictionET = restrictionBuilder.equalTo(EVENT_TYPE, et)
        final Restriction restrictionOOR = restrictionBuilder.equalTo(OBJECT_OF_REFERENCE, oor)
        final Restriction finalRestriction = restrictionBuilder.allOf(restrictionSP, restrictionPC, restrictionET, restrictionOOR)
        typeQuery.setRestriction(finalRestriction)
        final Iterator<StubbedPersistenceObject> alarmIterator = liveBucket.getQueryExecutor().execute(typeQuery)
        if (alarmIterator == null || !alarmIterator.hasNext()) {
            return 0L
        }
        final StubbedPersistenceObject po = alarmIterator.next()
        return po.getPoId()
    }

    def "updateThresholdCrossed case ON"() {
        given:
        retryManager.executeCommand(*_) >> { protectionService.raiseThresholdCrossingAlert() }

        when: "call updateThresholdCrossed"
        thresholdCrossingImpl.updateThresholdCrossed(ThresholdCrossed.ON)

        then: "expected valuation"
        1 * internalAlarmGenerator.raiseInternalAlarm({ Map<String, Object> alertDetails ->
            eventType.equals(alertDetails.get(EVENT_TYPE)) &&
                    probableCause.equals(alertDetails.get(PROBABLE_CAUSE)) &&
                    specificProblem.equals(alertDetails.get(SPECIFIC_PROBLEM)) &&
                    problemText.equals(alertDetails.get(ADDITIONAL_TEXT)) &&
                    presentSeverity.equals(alertDetails.get(PRESENT_SEVERITY)) &&
                    recordType.equals(alertDetails.get(RECORD_TYPE)) &&
                    oor.equals(alertDetails.get(OBJECT_OF_REFERENCE)) &&
                    fdn.equals(alertDetails.get(FDN))
        }, *_)

        where:
        eventType            | probableCause             | specificProblem           | problemText                                     | presentSeverity | recordType      | oor                    | fdn
        "Quality of Service" | "systemResourcesOverload" | "Alarm Overload detected" | "Input Alarm Load is more than 10 in 5 minutes" | "CRITICAL"      | "ERROR_MESSAGE" | "ManagementSystem=ENM" | "ManagementSystem=ENM"
    }

    def "updateThresholdCrossed case WARN"() {
        given: "overload protection alert"
        createOverloadProtectionAlert(eventType, probableCause, specificProblem, problemText, presentSeverity, recordType, oor, fdn, alarmNumber)
        retryManager.executeCommand(*_) >> { protectionService.ackThresholdCrossingAlert() }
        if (alarmSuppressed) {
            fmFunctionMoService.updateCurrentServiceState("NetworkElement=RadioNode", ALARM_OVERLOAD_PROTECTION_SUPPRESSED)
        }
        when: "call updateThresholdCrossed"
        thresholdCrossingImpl.updateThresholdCrossed(ThresholdCrossed.WARN)

        then: "expected valuation"
        getPoId(specificProblem, probableCause, eventType, oor) == poId
        fmFunctionMoService.read("NetworkElement=RadioNode", "currentServiceState") == currentServiceState

        where:
        eventType            | probableCause             | specificProblem           | problemText                                        | presentSeverity | recordType      | oor                    | fdn                    | alarmNumber | alarmSuppressed || poId | currentServiceState
        "Quality of Service" | "systemResourcesOverload" | "Alarm Overload detected" | "Input Alarm Load is more than 10 in 5 minutes"    | "CRITICAL"      | "ERROR_MESSAGE" | "ManagementSystem=ENM" | "ManagementSystem=ENM" | 1234L       | false           || 0L   | "IN_SERVICE"
        "Quality of Service" | "systemResourcesOverload" | "Alarm Overload detected" | "Input Alarm Load is more than 55200 in 5 minutes" | "CRITICAL"      | "ERROR_MESSAGE" | "ManagementSystem=ENM" | "ManagementSystem=ENM" | 1234L       | true            || 0L   | "OUT_OF_SYNC"
    }
}

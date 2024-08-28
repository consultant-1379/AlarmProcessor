package com.ericsson.oss.services.fm.alarmprocessor.eventprocessor

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener
import com.ericsson.oss.services.fm.alarmprocessor.orphanclear.PendingClearAlarmProcessorManager
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager
import com.ericsson.oss.services.fm.alarmprocessor.utility.MockedClearCache

import java.text.SimpleDateFormat

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_OVERLOAD_PROTECTION_SUPPRESSED
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FM_DB_AVAILABILITY_CACHE
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.TARGET_ADDITIONAL_INFORMATION
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity.CLEARED;
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity.CRITICAL;
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import javax.cache.Cache
import javax.inject.Inject
import javax.jms.ObjectMessage
import org.junit.Rule
import org.slf4j.Logger

import com.ericsson.cds.cdi.support.rule.ImplementationInstance
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator
import com.ericsson.oss.mediation.translator.model.EventNotification
import com.ericsson.oss.services.fm.alarmprocessor.alarmsync.SyncInitiator
import com.ericsson.oss.services.fm.alarmprocessor.cluster.MembershipChangeProcessor
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService
import com.ericsson.oss.services.fm.alarmprocessor.enrichment.AlarmEnricher
import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmHandler
import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmHandlerBean
import com.ericsson.oss.services.fm.alarmprocessor.eventhandlers.AlarmPreProcessor
import com.ericsson.oss.services.fm.alarmprocessor.eventsender.ModeledEventSender
import com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability.AbstractBaseSpec
import com.ericsson.oss.services.fm.alarmprocessor.fmdbavailability.DatabaseStatusProcessor
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.AOPInstrumentedBean
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean
import com.ericsson.oss.services.fm.alarmprocessor.protection.AlarmOverloadProtectionService
import com.ericsson.oss.services.fm.alarmprocessor.utility.ReplaceSlf4jLogger
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity
import com.ericsson.oss.services.fm.ratedetectionengine.api.ThresholdCrossed
import com.ericsson.oss.services.fm.ratedetectionengine.RateDetectionServiceImpl
import java.util.concurrent.TimeUnit
import com.jayway.awaitility.Awaitility

class AlarmProcessorSpec extends AbstractBaseSpec{

    @ObjectUnderTest
    AlarmPreProcessor alarmPreProcessor

    @ObjectUnderTest
    AlarmHandler alarmhandler

    @ObjectUnderTest
    AlarmHandlerBean alarmHandlerBean

    @ImplementationClasses
    def classes = [RateDetectionServiceImpl]

    @ImplementationInstance
    AlarmEnricher alarmEnricher =[enrichNotification :{ x -> x }]as AlarmEnricher

    @ImplementationInstance
    ChannelLocator channelLocator = Mock()

    @ImplementationInstance
    Channel mockChannel = Mock()

    @Inject
    ObjectMessage messageObject

    @Inject
    private APSInstrumentedBean apsInstrumentedBean

    @Inject
    private AOPInstrumentedBean aopInstrumentedBean

    @Inject
    @NamedCache(FM_DB_AVAILABILITY_CACHE)
    private Cache<String, String> fmDatabaseAvailabilityCache

    @MockedImplementation
    private ModeledEventSender modeledEventSender

    @MockedImplementation
    private SyncInitiator syncInitiator

    @MockedImplementation
    private DatabaseStatusProcessor versantDbStatusHolder;

    @MockedImplementation
    private MembershipChangeProcessor membershipChangeProcessor

    @Inject
    AlarmOverloadProtectionService protectionService

    @Inject
    FmFunctionMoService fmFunctionMoService

    @Inject
    PendingClearAlarmProcessorManager pendingClearAlarmProcessorManager

    @Inject
    ClearAlarmsCacheManager clearAlarmsCacheManager

    @Inject
    ConfigParametersListener configParametersListener

    private Logger logger = Mock(Logger.class)

    @Rule ReplaceSlf4jLogger replaceSlf4jLogger = new ReplaceSlf4jLogger(AlarmPreProcessor.class, logger)

    def "FM-FMX use cases for repeated, updated and synchronization alarm"() {
        given :"An alarm is received when FMX rule is enabled/disabled"
        //Protection service initialized
        channelLocator.lookupChannel(*_) >> mockChannel
        protectionService.init()

        //Set DB available
        versantDbStatusHolder.isDatabaseAvailable() >> true

        //Alarm1 sent to APS
        List<EventNotification> normalAlarmList = buildEventNotificationList(alarmType1, nodeNameInput, severityInput1, specificProblemInput, probableCauseInput, eventTypeInput, recordTypeInput1, processingTypeInput1, fmxGeneratedInput1, visibility1, targetAdditionalInformation)
        alarmPreProcessor.onEvent(normalAlarmList)

        //Update the currentServiceState
        fmFunctionMoService.updateCurrentServiceState("NetworkElement="+nodeNameInput, currentServiceState)

        Map<String, Object> alarmAttributes = readAlarm(specificProblemInput, probableCauseInput, eventTypeInput, "MeContext="+nodeNameInput)
        def eventPoId = alarmAttributes.get("eventPoId")
        println "ALARM ATTRIBUTES:::::::: $alarmAttributes"

        when: "Second alarm is sent to APS when FMX rule is enabled/disabled"
        // Alarm2 sent to APS
        List<EventNotification> normalAlarmList2 = buildEventNotificationList(alarmType2, nodeNameInput, severityInput2, specificProblemInput, probableCauseInput, eventTypeInput, recordTypeInput2, processingTypeInput2, fmxGeneratedInput2, visibility2, targetAdditionalInformation)
        alarmPreProcessor.onEvent(normalAlarmList2)

        then: "assert expected alarm attributes with received alarm attributes from DB"
        Map<String, Object> finalAlarmAttributes = readAlarm(specificProblemInput, probableCauseInput, eventTypeInput, "MeContext="+nodeNameInput)
        def finalCurrentServiceState = fmFunctionMoService.read("NetworkElement="+nodeNameInput, "currentServiceState")
        def finalRecordTypeReceived = finalAlarmAttributes.get("recordType")
        def finalAlarmStateReceived =  finalAlarmAttributes.get("alarmState")
        def finalVisibilityReceived =  finalAlarmAttributes.get("visibility")
        def finalSeverityReceived =  finalAlarmAttributes.get("presentSeverity")
        def finallastAlarmOperationReceived =  finalAlarmAttributes.get("lastAlarmOperation")
        def finalSpecificProblemReceived =  finalAlarmAttributes.get("specificProblem")
        def finalProbableCauseReceived =  finalAlarmAttributes.get("probableCause")
        def finalEventTypeReceived    =   finalAlarmAttributes.get("eventType")
        assertEquals(finalRecordTypeExpected, finalRecordTypeReceived)
        assertEquals(finalAlarmStateExpected, finalAlarmStateReceived)
        assertEquals(finalVisibilityExpected, finalVisibilityReceived)
        assertEquals(finalSeverityExpected, finalSeverityReceived)
        assertEquals(finalSpecificProblemExpected, finalSpecificProblemReceived)
        assertEquals(finalProbableCauseExpected, finalProbableCauseReceived);
        assertEquals(finalEventTypeInputExpected, finalEventTypeReceived);
        assertEquals(finalCurrentServiceStateExpected, finalCurrentServiceState);
        assertEquals(finallastAlarmOperationExpected, finallastAlarmOperationReceived);

        apsInstrumentedBean.getAlarmRootPrimaryProcessedByAPS() == primaryAlarms
        apsInstrumentedBean.getAlarmRootSecondaryProcessedByAPS() == secondaryAlarms
        apsInstrumentedBean.getAlarmRootNotApplicableProcessedByAPS() == notApplicableAlarms

        //EventNotification for alarm update processing keeps unchanged additionalInformation even in case of errors.
        finalAlarmAttributes.get("additionalInformation") != null
        !finalAlarmAttributes.get("additionalInformation").isEmpty()
        !finalAlarmAttributes.get("additionalInformation").contains(targetAdditionalInformation) || notApplicableAlarms != 0
        sent * modeledEventSender.sendEventToCorbaNbi({ProcessedAlarmEvent event -> event.getEventPOId() == eventPoId && event.getAdditionalInformationString().contains(targetAdditionalInformation)}, *_)
        sent * modeledEventSender.sendEventToCoreOutQueue({ ProcessedAlarmEvent event -> event.getEventPOId() == eventPoId && event.getAdditionalInformationString().contains(targetAdditionalInformation)}, *_)
        sent * modeledEventSender.sendEventToSnmpNbi({ ProcessedAlarmEvent event -> event.getEventPOId() == eventPoId && event.getAdditionalInformationString().contains(targetAdditionalInformation)}, *_)
        sent * modeledEventSender.sendAlarmMetaData({ProcessedAlarmEvent event -> event.getEventPOId() == eventPoId && event.getAdditionalInformationString().contains(targetAdditionalInformation)}, *_)
        sent * modeledEventSender.sendAtrInput({ProcessedAlarmEvent event -> event.getEventPOId() == eventPoId && event.getAdditionalInformationString().contains(targetAdditionalInformation)}, *_)
        sent * modeledEventSender.sendFakeClear({ProcessedAlarmEvent event -> event.getEventPOId() == eventPoId && event.getAdditionalInformationString().contains(targetAdditionalInformation)}, *_)
        syncIntiated * syncInitiator.initiateAlarmSynchronization(_)

        // Check counters from rate-detection-engine
        counter * logger.debug('Current Interval alarm counter on APS {}', rateDetectionEngineCounters)

        where:
        nodeNameInput           | severityInput1        | severityInput2       | specificProblemInput      | probableCauseInput    | eventTypeInput        | recordTypeInput1       | targetAdditionalInformation                                                                                                                                                                 | recordTypeInput2        | processingTypeInput1     | processingTypeInput2     | fmxGeneratedInput1        | fmxGeneratedInput2        | visibility1       | visibility2       | currentServiceState | finalCurrentServiceStateExpected| finalRecordTypeExpected       | finalAlarmStateExpected       | finalVisibilityExpected   | finalSeverityExpected    | finalSpecificProblemExpected      |finalProbableCauseExpected  |finalEventTypeInputExpected |finallastAlarmOperationExpected | primaryAlarms | secondaryAlarms  | notApplicableAlarms | alarmType1        | alarmType2              |sent | syncIntiated | counter | rateDetectionEngineCounters
        // FMX hide present. Rules off. repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // FMX hide present. Rules off. repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MAJOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"S\":[\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\",\"f91a6e32-e523-b217-7C3912ad3012\"],\"C\":[{\"I\":\"201f0123-88ca-23a2-7451-8B5872ac457b\",\"n\":\"vRC\"}]};"                          | "REPEATED_ALARM"        | "NORMAL_PROC"            | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MAJOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |0              |1                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // NormalProc show present. Rules off. Repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // NormalProc show present. Rules off. Repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MAJOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MAJOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // PostProc hide present. Rules off. Repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "NOT_SET"                | "FMX_PROCESSED"           | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // PostProc hide present. Rules off. Repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MINOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              |  "NOT_SET"               | "FMX_PROCESSED"           | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MINOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // PostProc show present. Rules off. Repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              |  "NOT_SET"               | "FMX_PROCESSED"           | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // PostProc show present. Rules off. Repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MINOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "NOT_SET"                | "FMX_PROCESSED"           | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MINOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // FMX hide present. Rules on. repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "NORMAL_PROC"            | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | false                     | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        //FMX CREATE alarm. Rules on. repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "NORMAL_PROC"            | "FMX_CREATED"             | "FMX_CREATED"             | false             | false             | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | false                     | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 0       | 2
        // FMX hide present. Rules on. repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MAJOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "NORMAL_PROC"            | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | false                     | "MAJOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // NormalProc show present. Rules on. Repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "NORMAL_PROC"            | "NOT_SET"                 | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // NormalProc show present. Rules on. Repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MAJOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "NORMAL_PROC"            | "NOT_SET"                 | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MAJOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // PostProc hide present. Rules on. Repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "NORMAL_PROC"            | "FMX_PROCESSED"           | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | false                     | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // PostProc hide present. Rules on. Repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MINOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "NORMAL_PROC"            | "FMX_PROCESSED"           | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | false                     | "MINOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // PostProc show present. Rules on. Repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "NORMAL_PROC"            | "FMX_PROCESSED"           | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // PostProc show present. Rules on. Repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MINOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "NORMAL_PROC"            | "FMX_PROCESSED"           | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MINOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // FMX hide present. Rules on. repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "POST_PROC"              | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | false                     | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // FMX hide present. Rules on. repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MAJOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "POST_PROC"              | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | false                     | "MAJOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // NormalProc show present. Rules on. Repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "POST_PROC"              | "NOT_SET"                 | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // NormalProc show present. Rules on. Repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MAJOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "NORMAL_PROC"            | "POST_PROC"              | "NOT_SET"                 | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MAJOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 2
        // PostProc hide present. Rules on. Repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "POST_PROC"              | "FMX_PROCESSED"           | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | false                     | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // PostProc hide present. Rules on. Repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MINOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "POST_PROC"              | "FMX_PROCESSED"           | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | false                     | "MINOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // PostProc show present. Rules on. Repeated alarm arrived.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "POST_PROC"              | "FMX_PROCESSED"           | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // PostProc show present. Rules on. Repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MINOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "POST_PROC"              | "FMX_PROCESSED"           | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MINOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        // PostProc show present. Rules on. Repeated alarm arrived with severity change.
        "APS_Groovy_003"        | "CRITICAL"            | "MINOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "REPEATED_ALARM"        | "POST_PROC"              | "NORMAL_PROC"            | "FMX_PROCESSED"           | "NOT_SET"                 | true              | false             | "IN_SERVICE"        | "IN_SERVICE"                    | "REPEATED_ALARM"              | "ACTIVE_UNACKNOWLEDGED"       | false                     | "MINOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_ALARM"       | "NORMAL_ALARM"          |1    | 0            | 1       | 1
        //Inputs for the alarm Synchronization (Uncorrelated alarm) use case for FMX updated alarm.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                |  "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                       | "ALARM"                 | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "SYNCHRONIZATION_ALARM"       | "CLEARED_UNACKNOWLEDGED"      | true                      | "CLEARED"                | "testSP"                           | "testPC"                  | "testET"                   |"CLEAR"                         |1              |0                 |0                    | "FMX_UPDATE"      | "SYNC_ALARM"            |0    | 0            | 0       | 0
        "APS_Groovy_003"        | "MAJOR"               | "MAJOR"              | "testSP_NonSynchable"     | "testPC"              | "testET"              | "NON_SYNCHABLE_ALARM"  |  "CI={\"S\":[\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\",\"f91a6e32-e523-b217-7C3912ad3012\"]}"                                                                                                 | "NON_SYNCHABLE_ALARM"   | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "UPDATE"                      | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MAJOR"                  | "testSP_NonSynchable"              | "testPC"                  | "testET"                   |"CHANGE"                        |0              |1                 |0                    | "FMX_UPDATE"      | "SYNC_ALARM"            |0    | 0            | 0       | 0
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                |  "DN2=ManagedElement\\=1,Equipment\\=1;CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                  | "ALARM"                 | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "SYNCHRONIZATION_ALARM"       | "CLEARED_UNACKNOWLEDGED"      | true                      | "CLEARED"                | "testSP"                           | "testPC"                  | "testET"                   |"CLEAR"                         |1              |0                 |0                    | "FMX_UPDATE"      | "SYNC_ALARM"            |0    | 0            | 0       | 0
        "APS_Groovy_003"        | "MAJOR"               | "MAJOR"              | "testSP_NonSynchable"     | "testPC"              | "testET"              | "NON_SYNCHABLE_ALARM"  |  "DN2=ManagedElement\\=1,Equipment\\=1,RbsSubrack\\=RUW1,RbsSlot\\=5,AuxPlugInUnit\\=RUW-2,DeviceGroup\\=RUW,AiDeviceSet\\=1,AiDevice\\=1;"                                                 | "NON_SYNCHABLE_ALARM"   | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "UPDATE"                      | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MAJOR"                  | "testSP_NonSynchable"              | "testPC"                  | "testET"                   |"CHANGE"                        |0              |0                 |1                    | "FMX_UPDATE"      | "SYNC_ALARM"            |0    | 0            | 0       | 0
        "APS_Groovy_003"        | "MAJOR"               | "MAJOR"              | "testSP_NonSynchable"     | "testPC"              | "testET"              | "NON_SYNCHABLE_ALARM"  |  "CI={}"                                                                                                                                                                                    | "NON_SYNCHABLE_ALARM"   | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "UPDATE"                      | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MAJOR"                  | "testSP_NonSynchable"              | "testPC"                  | "testET"                   |"CHANGE"                        |0              |0                 |1                    | "FMX_UPDATE"      | "SYNC_ALARM"            |0    | 0            | 0       | 0
        "APS_Groovy_003"        | "MAJOR"               | "MAJOR"              | "testSP_NonSynchable"     | "testPC"              | "testET"              | "NON_SYNCHABLE_ALARM"  |  "CI={\"S\":[\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\",\"f91a6e32-e523-b217-7C3912ad3012\"],\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                   | "NON_SYNCHABLE_ALARM"   | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "UPDATE"                      | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MAJOR"                  | "testSP_NonSynchable"              | "testPC"                  | "testET"                   |"CHANGE"                        |0              |0                 |1                    | "FMX_UPDATE"      | "SYNC_ALARM"            |0    | 0            | 0       | 0
        "APS_Groovy_003"        | "MAJOR"               | "MAJOR"              | "����??��??�������1234"   |"����??��??�������1234"|"����??��??�������1234"| "NON_SYNCHABLE_ALARM"  | "CI={\"S\":[\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\",\"f91a6e32-e523-b217-7C3912ad3012\"],\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                    | "NON_SYNCHABLE_ALARM"   | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "UPDATE"                      | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MAJOR"                  | "����??��??�������1234"            | "����??��??�������1234"   | "����??��??�������1234"    |"CHANGE"                        |0              |0                 |1                    | "FMX_UPDATE"      | "SYNC_ALARM"            |0    | 0            | 0       | 0
        //Below inputs are to check the currentServiceState of the node after performing SYNC.
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                |  "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                       | "ALARM"                 | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "IDLE"              | "IDLE"                          | "UPDATE"                      | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"CHANGE"                        |1              |0                 |0                    | "FMX_UPDATE"      | "SYNC_ALARM"            |0    | 0            | 0       | 0
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                |  "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                       | "ALARM"                 | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "OUT_OF_SYNC"       | "IN_SERVICE"                    | "SYNCHRONIZATION_ALARM"       | "CLEARED_UNACKNOWLEDGED"      | true                      | "CLEARED"                | "testSP"                           | "testPC"                  | "testET"                   |"CLEAR"                         |1              |0                 |0                    | "FMX_UPDATE"      | "SYNC_ALARM"            |0    | 0            | 0       | 0
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                |  "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                       | "ALARM"                 | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | false             | true              | "HEART_BEAT_FAILURE"| "IN_SERVICE"                    | "SYNCHRONIZATION_ALARM"       | "CLEARED_UNACKNOWLEDGED"      | true                      | "CLEARED"                | "testSP"                           | "testPC"                  | "testET"                   |"CLEAR"                         |1              |0                 |0                    | "FMX_UPDATE"      | "SYNC_ALARM"            |0    | 0            | 0       | 0
        //Instrumentation test for Correlated alarms synchronization
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "SYNCHRONIZATION_ALARM" | "NORMAL_PROC"            | "NORMAL_PROC"            | "FMX_PROCESSED"           | "NOT_SET"                 | true              | true              | "SYNC_ONGOING"      | "IN_SERVICE"                    | "ALARM"                       | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"NEW"                           |2              |0                 |0                    | "NORMAL_ALARM"    | "SYNCHRONIZATION_ALARM" |0    | 0            | 0       | 0
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={\"S\":[\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\",\"f91a6e32-e523-b217-7C3912ad3012\"]}"                                                                                                  | "SYNCHRONIZATION_ALARM" | "NORMAL_PROC"            | "NORMAL_PROC"            | "FMX_PROCESSED"           | "NOT_SET"                 | true              | true              | "SYNC_ONGOING"      | "IN_SERVICE"                    | "ALARM"                       | "ACTIVE_UNACKNOWLEDGED"       | true                      | "CRITICAL"               | "testSP"                           | "testPC"                  | "testET"                   |"NEW"                           |0              |2                 |0                    | "NORMAL_ALARM"    | "SYNCHRONIZATION_ALARM" |0    | 0            | 0       | 0
        "APS_Groovy_003"        | "MAJOR"               | "MAJOR"              | "testSP"                  | "testPC"              | "testET"              | "ALARM"                | "CI={}"                                                                                                                                                                                     | "SYNCHRONIZATION_ALARM" | "NORMAL_PROC"            | "NORMAL_PROC"            | "FMX_PROCESSED"           | "NOT_SET"                 | true              | true              | "SYNC_ONGOING"      | "IN_SERVICE"                    | "ALARM"                       | "ACTIVE_UNACKNOWLEDGED"       | true                      | "MAJOR"                  | "testSP"                           | "testPC"                  | "testET"                   |"NEW"                           |0              |0                 |2                    | "NORMAL_ALARM"    | "SYNCHRONIZATION_ALARM" |0    | 0            | 0       | 0
        // HEARTBEAT clear as part of sync
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "Heartbeat Failure"       | "Lan Error"           | "Communications alarm"| "HEARTBEAT_ALARM"      | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "ALARM"                 | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | true              | true              | "IN_SERVICE"        | "IN_SERVICE"                    | "HEARTBEAT_ALARM"             | "CLEARED_UNACKNOWLEDGED"     | true                       | "CLEARED"                | "Heartbeat Failure"                | "Lan Error"               | "Communications alarm"     |"CLEAR"                         |1              |0                 |0                    | "NORMAL_ALARM"    | "SYNC_ALARM"            |1    | 0            | 0       | 0
        "APS_Groovy_003"        | "CRITICAL"            | "CLEARED"            | "Heartbeat Failure"       | "Lan Error"           | "Communications alarm"| "HEARTBEAT_ALARM"      | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"                                                                                                                                        | "HEARTBEAT_ALARM"       | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | true              | true              | "HEART_BEAT_FAILURE"| "IN_SERVICE"                    | "HEARTBEAT_ALARM"             | "CLEARED_UNACKNOWLEDGED"     | true                       | "CLEARED"                | "Heartbeat Failure"                | "Lan Error"               | "Communications alarm"     |"CLEAR"                         |1              |0                 |0                    | "NORMAL_ALARM"    | "NORMAL_ALARM"          |1    | 1            | 0       | 0
        //Repeated alarm present .NORMAL_PROC Rules on.Repeated alarm arrived.
        "APS_Groovy_003"        |"CRITICAL"             |"CRITICAL"            |"testSP"                   |"testPC"               |"testET"               |"REPEATED_ALARM"        |"CI={}"                                                                                                                                                                                      |"REPEATED_ALARM"         |"NOT_SET"                 |"NORMAL_PROC"             |"NOT_SET"                  |"FMX_PROCESSED"            |true               |false              |"IN_SERVICE"         | "IN_SERVICE"                    |"REPEATED_ALARM"               | "ACTIVE_UNACKNOWLEDGED"      |false                       |"CRITICAL"                   |"testSP"                            |"testPC"                   | "testET"                   |"CHANGE"                        |0             |0                 |1                    | "NORMAL_ALARM"    |"FMX_ALARM"              |1     |0             |0       |0
        //Repeated alarm hidden .Rules on.Repeated alarm arrived to change the visibility to true.
        "APS_Groovy_003"        |"CRITICAL"             |"CRITICAL"            |"testSP"                   |"testPC"               |"testET"               |"REPEATED_ALARM"        |"CI={}"                                                                                                                                                                                      |"REPEATED_ALARM"         |"NORMAL_PROC"             |"NORMAL_PROC"             |"NOT_SET"                  |"FMX_PROCESSED"            |false               |true              |"IN_SERVICE"         | "IN_SERVICE"                    |"REPEATED_ALARM"               | "ACTIVE_UNACKNOWLEDGED"      |true                       |"CRITICAL"                   |"testSP"                            |"testPC"                   | "testET"                   |"CHANGE"                        |0             |0                 |1                    | "FMX_ALARM"    |"NORMAL_ALARM"              |1     |0             |0       |0
       }


   def "receives ProcessedAlarmEvent from AlarmPreProcessor}"() {
       given :"alarm received with data base unavailable"
       versantDbStatusHolder.isDatabaseAvailable() >> false
       ProcessedAlarmEvent processedAlarmEvent = new ProcessedAlarmEvent()
       processedAlarmEvent.setRecordType(recordType)
       processedAlarmEvent.setFdn(fdn)
       processedAlarmEvent.setPresentSeverity(presentSeverity)
       //init protection Service.
       protectionService.init()

       when: "onEvent is triggered"
       alarmhandler.onEvent(processedAlarmEvent)

       then: "only syncable resources chached"
       final String value = fmDatabaseAvailabilityCache.get(fdn)
       if (checkAssertNull) {
           assertNull(value)
       } else {
           assertNotNull(value)
       }

       where:
       recordType                                      | fdn                                  | checkAssertNull|presentSeverity
       FMProcessedEventType.ERROR_MESSAGE              | "VirtualNetworkFunctionManager=ECM"  | false          |CRITICAL
       FMProcessedEventType.REPEATED_ERROR_MESSAGE     | "VirtualNetworkFunctionManager=ECM"  | false          |CRITICAL
       FMProcessedEventType.NON_SYNCHABLE_ALARM        | "VirtualNetworkFunctionManager=ECM"  | true           |CRITICAL
       FMProcessedEventType.REPEATED_NON_SYNCHABLE     | "VirtualNetworkFunctionManager=ECM"  | true           |CRITICAL
       FMProcessedEventType.HEARTBEAT_ALARM            | "VirtualNetworkFunctionManager=ECM"  | true           |CRITICAL
       FMProcessedEventType.HEARTBEAT_ALARM            | "VirtualNetworkFunctionManager=ECM"  | false          |CLEARED
       FMProcessedEventType.NODE_SUSPENDED             | "VirtualNetworkFunctionManager=ECM"  | true           |CRITICAL
       FMProcessedEventType.NODE_SUSPENDED             | "VirtualNetworkFunctionManager=ECM"  | false          |CLEARED
       FMProcessedEventType.ALARM                      | "VirtualNetworkFunctionManager=ECM"  | false          |CRITICAL
       FMProcessedEventType.ERROR_MESSAGE              | "NetworkElement=RadioNode"           | true           |CRITICAL
       FMProcessedEventType.REPEATED_ERROR_MESSAGE     | "NetworkElement=RadioNode"           | true           |CRITICAL
       FMProcessedEventType.NON_SYNCHABLE_ALARM        | "NetworkElement=RadioNode"           | true           |CRITICAL
       FMProcessedEventType.REPEATED_NON_SYNCHABLE     | "NetworkElement=RadioNode"           | true           |CRITICAL
       FMProcessedEventType.HEARTBEAT_ALARM            | "NetworkElement=RadioNode"           | true           |CRITICAL
       FMProcessedEventType.HEARTBEAT_ALARM            | "NetworkElement=RadioNode"           | false          |CLEARED
       FMProcessedEventType.NODE_SUSPENDED             | "NetworkElement=RadioNode"           | true           |CRITICAL
       FMProcessedEventType.NODE_SUSPENDED             | "NetworkElement=RadioNode"           | false          |CLEARED
       FMProcessedEventType.ALARM                      | "NetworkElement=RadioNode"           | false          |CRITICAL
       FMProcessedEventType.ERROR_MESSAGE              | "NetworkElement=BSC"                 | true           |CRITICAL
       FMProcessedEventType.REPEATED_ERROR_MESSAGE     | "NetworkElement=BSC"                 | true           |CRITICAL
       FMProcessedEventType.NON_SYNCHABLE_ALARM        | "NetworkElement=BSC"                 | true           |CRITICAL
       FMProcessedEventType.REPEATED_NON_SYNCHABLE     | "NetworkElement=BSC"                 | true           |CRITICAL
       FMProcessedEventType.HEARTBEAT_ALARM            | "NetworkElement=BSC"                 | true           |CRITICAL
       FMProcessedEventType.HEARTBEAT_ALARM            | "NetworkElement=BSC"                 | false          |CLEARED
       FMProcessedEventType.NODE_SUSPENDED             | "NetworkElement=BSC"                 | true           |CRITICAL
       FMProcessedEventType.NODE_SUSPENDED             | "NetworkElement=BSC"                 | false          |CLEARED
       FMProcessedEventType.ALARM                      | "NetworkElement=BSC"                 | false          |CRITICAL
   }

    def "handles EventNotification in case of normal or overload condition"() {
        given: "setup"
        //set DB available
        versantDbStatusHolder.isDatabaseAvailable() >> true

        //set fmalarmprocessing master
        membershipChangeProcessor.getMasterState() >> true

        //set the currentServiceState
        fmFunctionMoService.updateCurrentServiceState("NetworkElement=APS_Groovy_003", currentServiceState)

        // Init protection service.
        protectionService.init()

        //set safeMode
        protectionService.setSafeMode(safeMode)

        when: "alarm is sent to APS"
        //alarm sent to APS
        List<EventNotification> alarmList = buildEventNotificationList("NORMAL_ALARM", "APS_Groovy_003", severity, "testSP", "testPC", "testET", recordType, "NOT_SET", "NOT_SET", true, "")
        alarmPreProcessor.onEvent(alarmList)

        then: "assert expected alarm is handled or not"
        fmFunctionMoService.read("NetworkElement=APS_Groovy_003", "currentServiceState") == expCurrentServiceState
        sent * modeledEventSender.sendEventToCorbaNbi(*_)
        sent * modeledEventSender.sendEventToCoreOutQueue(*_)
        sent * modeledEventSender.sendEventToSnmpNbi(*_)
        sent * modeledEventSender.sendAlarmMetaData(*_)
        sent * modeledEventSender.sendAtrInput(*_)

        where:
        severity        | recordType                | currentServiceState | safeMode              || expCurrentServiceState               | sent
        //case alarm overload protection functionality enabled and safe mode OFF
        "CRITICAL"      | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MAJOR"         | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MINOR"         | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "WARNING"       | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "CRITICAL"      | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.OFF  || "SYNC_ONGOING"                       | 1
        "MAJOR"         | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.OFF  || "SYNC_ONGOING"                       | 1
        "MINOR"         | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.OFF  || "SYNC_ONGOING"                       | 1
        "INDETERMINATE" | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.OFF  || "SYNC_ONGOING"                       | 1
        "WARNING"       | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.OFF  || "SYNC_ONGOING"                       | 1
        "CRITICAL"      | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MAJOR"         | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MINOR"         | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "WARNING"       | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "CRITICAL"      | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.OFF  || "HEART_BEAT_FAILURE"                 | 1
        "MAJOR"         | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.OFF  || "HEART_BEAT_FAILURE"                 | 1
        "MINOR"         | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.OFF  || "HEART_BEAT_FAILURE"                 | 1
        "INDETERMINATE" | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.OFF  || "HEART_BEAT_FAILURE"                 | 1
        "WARNING"       | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.OFF  || "HEART_BEAT_FAILURE"                 | 1
        "CRITICAL"      | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MAJOR"         | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MINOR"         | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "WARNING"       | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "CRITICAL"      | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MAJOR"         | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MINOR"         | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "WARNING"       | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "CRITICAL"      | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MAJOR"         | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MINOR"         | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "WARNING"       | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "CRITICAL"      | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MAJOR"         | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MINOR"         | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "WARNING"       | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "CRITICAL"      | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MAJOR"         | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MINOR"         | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "WARNING"       | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "CRITICAL"      | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MAJOR"         | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MINOR"         | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "WARNING"       | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "CRITICAL"      | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MAJOR"         | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "MINOR"         | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "WARNING"       | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.OFF  || "IN_SERVICE"                         | 1
        "CRITICAL"      | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.OFF  || "HEART_BEAT_FAILURE"                 | 1
        "MAJOR"         | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.OFF  || "HEART_BEAT_FAILURE"                 | 1
        "MINOR"         | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.OFF  || "HEART_BEAT_FAILURE"                 | 1
        "INDETERMINATE" | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.OFF  || "HEART_BEAT_FAILURE"                 | 1
        "WARNING"       | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.OFF  || "HEART_BEAT_FAILURE"                 | 1
        "CRITICAL"      | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.OFF  || "NODE_SUSPENDED"                     | 1
        "MAJOR"         | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.OFF  || "NODE_SUSPENDED"                     | 1
        "MINOR"         | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.OFF  || "NODE_SUSPENDED"                     | 1
        "INDETERMINATE" | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.OFF  || "NODE_SUSPENDED"                     | 1
        "WARNING"       | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.OFF  || "NODE_SUSPENDED"                     | 1
        "CRITICAL"      | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.OFF  || "OUT_OF_SYNC"                        | 1
        "MAJOR"         | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.OFF  || "OUT_OF_SYNC"                        | 1
        "MINOR"         | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.OFF  || "OUT_OF_SYNC"                        | 1
        "INDETERMINATE" | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.OFF  || "OUT_OF_SYNC"                        | 1
        "WARNING"       | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.OFF  || "OUT_OF_SYNC"                        | 1
        //case alarm overload protection functionality enabled and safe mode ON
        "CRITICAL"      | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MAJOR"         | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MINOR"         | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "WARNING"       | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.ON   || "SYNC_ONGOING"                       | 1
        "MAJOR"         | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.ON   || "SYNC_ONGOING"                       | 1
        "MINOR"         | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.ON   || "SYNC_ONGOING"                       | 1
        "WARNING"       | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MAJOR"         | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MINOR"         | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "WARNING"       | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.ON   || "HEART_BEAT_FAILURE"                 | 1
        "MAJOR"         | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.ON   || "HEART_BEAT_FAILURE"                 | 1
        "MINOR"         | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.ON   || "HEART_BEAT_FAILURE"                 | 1
        "WARNING"       | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MAJOR"         | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MINOR"         | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "WARNING"       | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MAJOR"         | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MINOR"         | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "WARNING"       | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MAJOR"         | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MINOR"         | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "WARNING"       | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MAJOR"         | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "MINOR"         | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.ON   || "IN_SERVICE"                         | 1
        "WARNING"       | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MAJOR"         | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MINOR"         | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "WARNING"       | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MAJOR"         | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MINOR"         | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "WARNING"       | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MAJOR"         | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MINOR"         | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "WARNING"       | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MAJOR"         | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MINOR"         | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "WARNING"       | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MAJOR"         | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MINOR"         | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "WARNING"       | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "CRITICAL"      | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MAJOR"         | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "MINOR"         | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "INDETERMINATE" | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        "WARNING"       | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.ON   || ALARM_OVERLOAD_PROTECTION_SUPPRESSED | 0
        //case alarm overload protection functionality enabled and safe mode WARN
        "CRITICAL"      | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MAJOR"         | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MINOR"         | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "WARNING"       | "ALARM"                   | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "CRITICAL"      | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.WARN || "SYNC_ONGOING"                       | 1
        "MAJOR"         | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.WARN || "SYNC_ONGOING"                       | 1
        "MINOR"         | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.WARN || "SYNC_ONGOING"                       | 1
        "INDETERMINATE" | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.WARN || "SYNC_ONGOING"                       | 1
        "WARNING"       | "SYNCHRONIZATION_ALARM"   | "SYNC_ONGOING"      | ThresholdCrossed.WARN || "SYNC_ONGOING"                       | 1
        "CRITICAL"      | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MAJOR"         | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MINOR"         | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "WARNING"       | "REPEATED_ALARM"          | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "CRITICAL"      | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.WARN || "HEART_BEAT_FAILURE"                 | 1
        "MAJOR"         | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.WARN || "HEART_BEAT_FAILURE"                 | 1
        "MINOR"         | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.WARN || "HEART_BEAT_FAILURE"                 | 1
        "INDETERMINATE" | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.WARN || "HEART_BEAT_FAILURE"                 | 1
        "WARNING"       | "HEARTBEAT_ALARM"         | "IN_SERVICE"        | ThresholdCrossed.WARN || "HEART_BEAT_FAILURE"                 | 1
        "CRITICAL"      | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MAJOR"         | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MINOR"         | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "WARNING"       | "NON_SYNCHABLE_ALARM"     | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "CRITICAL"      | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MAJOR"         | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MINOR"         | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "WARNING"       | "REPEATED_NON_SYNCHABLE"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "CRITICAL"      | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MAJOR"         | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MINOR"         | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "WARNING"       | "ERROR_MESSAGE"           | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "CRITICAL"      | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MAJOR"         | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MINOR"         | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "WARNING"       | "REPEATED_ERROR_MESSAGE"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "CRITICAL"      | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MAJOR"         | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MINOR"         | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "WARNING"       | "TECHNICIAN_PRESENT"      | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "CRITICAL"      | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MAJOR"         | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MINOR"         | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "WARNING"       | "SYNCHRONIZATION_ABORTED" | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "CRITICAL"      | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MAJOR"         | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "MINOR"         | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "INDETERMINATE" | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "WARNING"       | "ALARM_SUPPRESSED_ALARM"  | "IN_SERVICE"        | ThresholdCrossed.WARN || "IN_SERVICE"                         | 1
        "CRITICAL"      | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.WARN || "HEART_BEAT_FAILURE"                 | 1
        "MAJOR"         | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.WARN || "HEART_BEAT_FAILURE"                 | 1
        "MINOR"         | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.WARN || "HEART_BEAT_FAILURE"                 | 1
        "INDETERMINATE" | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.WARN || "HEART_BEAT_FAILURE"                 | 1
        "WARNING"       | "HB_FAILURE_NO_SYNCH"     | "IN_SERVICE"        | ThresholdCrossed.WARN || "HEART_BEAT_FAILURE"                 | 1
        "CRITICAL"      | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.WARN || "NODE_SUSPENDED"                     | 1
        "MAJOR"         | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.WARN || "NODE_SUSPENDED"                     | 1
        "MINOR"         | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.WARN || "NODE_SUSPENDED"                     | 1
        "INDETERMINATE" | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.WARN || "NODE_SUSPENDED"                     | 1
        "WARNING"       | "NODE_SUSPENDED"          | "IN_SERVICE"        | ThresholdCrossed.WARN || "NODE_SUSPENDED"                     | 1
        "CRITICAL"      | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.WARN || "OUT_OF_SYNC"                        | 1
        "MAJOR"         | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.WARN || "OUT_OF_SYNC"                        | 1
        "MINOR"         | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.WARN || "OUT_OF_SYNC"                        | 1
        "INDETERMINATE" | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.WARN || "OUT_OF_SYNC"                        | 1
        "WARNING"       | "OUT_OF_SYNC"             | "IN_SERVICE"        | ThresholdCrossed.WARN || "OUT_OF_SYNC"                        | 1
    }

    def "checks Alarm Overload Protection instrumentation"() {
        given: "setup"
        //set DB available
        versantDbStatusHolder.isDatabaseAvailable() >> true

        //set fmalarmprocessing master
        membershipChangeProcessor.getMasterState() >> true

        // Init protection service.
        protectionService.init()

        when: "alarms are sent to APS"
        List<EventNotification> alarmList = new ArrayList<EventNotification>()
        EventNotification eventNotification = new EventNotification()
        Map<String, String> additionalInformation = new HashMap<>()
        additionalInformation.put("fdn", "NetworkElement=APS_Groovy_003")

        severity.each { it1->
            recordType.each { it2->
                eventNotification = buildEventNotification("MeContext=APS_Groovy_003", it1, "testSP", "testPC", "testET", it2, "NOT_SET", "NOT_SET", additionalInformation, null)
                eventNotification.setVisibility(true)
                alarmList.add(eventNotification)
            }
        }

        //alarms sent to APS during AOP
        protectionService.setSafeMode(ThresholdCrossed.ON)
        alarmPreProcessor.onEvent(alarmList)

        //alarms sent to APS after AOP
        protectionService.setSafeMode(ThresholdCrossed.WARN)
        alarmPreProcessor.onEvent(alarmList)

        then: "assert expected alarm counts"
        aopInstrumentedBean.getAlarmCountDiscardedByAPS() == alarmsDiscarded
        aopInstrumentedBean.getAlertCountDiscardedByAPS() == alertsDiscarded

        where:
        severity                                               | recordType                                         || alarmsDiscarded | alertsDiscarded
        ["CRITICAL","MAJOR","MINOR","INDETERMINATE","WARNING"] | ["ALARM","ERROR_MESSAGE","REPEATED_ERROR_MESSAGE"] || 2               | 4
    }

    def "handles Sync on Clear EventNotification"() {
        given: "setup"

        Date raiseTime = new Date()

        versantDbStatusHolder.isDatabaseAvailable() >> true
        membershipChangeProcessor.getMasterState() >> true
        MockedClearCache mockedClearCache = new MockedClearCache()
        pendingClearAlarmProcessorManager.cache = mockedClearCache
        clearAlarmsCacheManager.clearAlarmsCache = mockedClearCache

        def thread = Thread.start {
            pendingClearAlarmProcessorManager.onMembershipChange(true)
        }
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until({ mockedClearCache.getListener() != null })

        //set the currentServiceState
        fmFunctionMoService.updateCurrentServiceState("NetworkElement=APS_Groovy_003", currentServiceState)

        //set safeMode to OFF
        protectionService.setSafeMode(ThresholdCrossed.OFF)

        //init protection Service.
        protectionService.init()

        when: "alarm is sent to APS"
        severity.eachWithIndex { it,index->
            //alarm sent to APS
            Date notificationTime = new Date(raiseTime.getTime() + eventTimeOffset.get(index))
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddkkmmss")
            List<EventNotification> alarmList = buildEventNotificationList("NORMAL_ALARM", "APS_Groovy_003", it, "testSP", "testPC", "testET", recordType, "NOT_SET", "NOT_SET", true, "", simpleDateFormat.format(notificationTime))
            alarmPreProcessor.onEvent(alarmList)
        }

        if (expSynchronization>0) {
            sleep(configParametersListener.getTimerIntervalToInitiateAlarmSyncMultiplier() * configParametersListener.getTimerIntervalToInitiateAlarmSync() + 10000)
        }

        if (thread.isAlive()) {
            thread.interrupt()
        }

        then: "assert expected alarm is handled or not"
        sent * modeledEventSender.sendEventToCorbaNbi(*_)
        sent * modeledEventSender.sendEventToCoreOutQueue(*_)
        sent * modeledEventSender.sendEventToSnmpNbi(*_)
        sent * modeledEventSender.sendAlarmMetaData(*_)
        sent * modeledEventSender.sendAtrInput(*_)
        expSynchronization * syncInitiator.initiateAlarmSync(*_)

        where:
        severity                                | eventTimeOffset              | recordType | currentServiceState  || expSynchronization    | sent
        ["CLEARED"]                             | [10000]                      | "ALARM"    | "IN_SERVICE"         || 1                     | 0
        ["CLEARED","MAJOR"]                     | [20000, 10000]               | "ALARM"    | "IN_SERVICE"         || 0                     | 2
        ["CLEARED","CLEARED","MAJOR"]           | [20000, 30000, 10000]        | "ALARM"    | "IN_SERVICE"         || 1                     | 2
        ["CLEARED","CLEARED","MAJOR", "MAJOR"]  | [20000, 40000, 10000, 30000] | "ALARM"    | "IN_SERVICE"         || 0                     | 4
    }

    def "Verify if fake clear is sent in case of repeated alarm with NORMAL_PROC RULE"() {
        given :"An alarm is received when FMX rule is enabled/disabled"
        //Alarm1 sent
        ProcessedAlarmEvent processedAlarmEvent1 = buildProcessedAlarmEvent(nodeNameInput, severityInput1, specificProblemInput, probableCauseInput, eventTypeInput, recordTypeInput1, processingTypeInput1, fmxGeneratedInput1, visibility1)
        alarmHandlerBean.processAlarm(processedAlarmEvent1)
        when: "Second alarm is sent to APS when FMX rule is enabled/disabled"
        ProcessedAlarmEvent processedAlarmEvent2 = buildProcessedAlarmEvent(nodeNameInput, severityInput2, specificProblemInput, probableCauseInput, eventTypeInput, recordTypeInput2, processingTypeInput2, fmxGeneratedInput2, visibility2)
        def response =alarmHandlerBean.processAlarm(processedAlarmEvent2)
        then :"check whether fake clear is sent or not."
        //assertEquals(expectedisSendFakeClearToUiAndNbi, response.isSendFakeClearToUiAndNbi())

        where:
        nodeNameInput   | severityInput1                 |severityInput2                 | specificProblemInput| probableCauseInput| eventTypeInput   | recordTypeInput1                   |recordTypeInput2                            | processingTypeInput1|processingTypeInput2| fmxGeneratedInput1|fmxGeneratedInput2| visibility1 |visibility2 |expectedisSendFakeClearToUiAndNbi
        //NormalProc show present. Rules on. Repeated alarm arrived
        "APS_Groovy_003"| ProcessedEventSeverity.CRITICAL|ProcessedEventSeverity.CRITICAL| "testSp"            | "testPC"          |  "testET"        | FMProcessedEventType.ALARM         |FMProcessedEventType.REPEATED_ALARM         | "NORMAL_PROC"       |"NORMAL_PROC"       | "FMX_PROCESSED"   |"NOT_SET"         | true        |false       |true
        "APS_Groovy_003"| ProcessedEventSeverity.CRITICAL|ProcessedEventSeverity.CRITICAL| "testSp"            | "testPC"          |  "testET"        | FMProcessedEventType.ERROR_MESSAGE |FMProcessedEventType.REPEATED_ERROR_MESSAGE | "NORMAL_PROC"       |"NORMAL_PROC"       | "FMX_PROCESSED"   |"NOT_SET"         | true        |false       |true

        //NormalProc show present. Rules off. Repeated alarm arrived.
        "APS_Groovy_003"| ProcessedEventSeverity.CRITICAL|ProcessedEventSeverity.CRITICAL| "testSp"            | "testPC"          |  "testET"        | FMProcessedEventType.ALARM         |FMProcessedEventType.REPEATED_ALARM         | "NORMAL_PROC"       |"NOT_SET"           | "FMX_PROCESSED"   |"NOT_SET"         | true        |true        |false

        //NormalProc show present. Rules on. Repeated alarm arrived with record type of second alarm as ALARM
        "APS_Groovy_003"| ProcessedEventSeverity.CRITICAL|ProcessedEventSeverity.CRITICAL| "testSp"            | "testPC"          |  "testET"        | FMProcessedEventType.ALARM         |FMProcessedEventType.ALARM                  | "NORMAL_PROC"       |"NORMAL_PROC"       | "FMX_PROCESSED"   |"NOT_SET"         | true        |false       |false
        //NormalProc show present. Rules off. Repeated alarm arrived with record type of second alarm as ALARM.
        "APS_Groovy_003"| ProcessedEventSeverity.CRITICAL|ProcessedEventSeverity.CRITICAL| "testSp"            | "testPC"          |  "testET"        | FMProcessedEventType.ALARM         |FMProcessedEventType.ALARM                  | "NORMAL_PROC"       |"NOT_SET"           | "FMX_PROCESSED"   |"NOT_SET"         | true        |true        |false

        // FMX hide present. Rules off. repeated alarm arrived.
        "APS_Groovy_003"| ProcessedEventSeverity.CRITICAL|ProcessedEventSeverity.CRITICAL| "testSp"            | "testPC"          |  "testET"        | FMProcessedEventType.ALARM         |FMProcessedEventType.ALARM                  | "NORMAL_PROC"       |"NOT_SET"           | "NOT_SET"         |"NOT_SET"         | false        |true       |false
        // FMX hide present. Rules off. repeated alarm arrived.
        "APS_Groovy_003"| ProcessedEventSeverity.CRITICAL|ProcessedEventSeverity.CRITICAL| "testSp"            | "testPC"          |  "testET"        | FMProcessedEventType.ALARM         |FMProcessedEventType.ALARM                  | "NORMAL_PROC"       |"NORMAL_PROC"       | "NOT_SET"         |"NOT_SET"         | false        |false      |false

        //NormalProc show present. Rules on. Repeated alarm arrived with record type TECHNICIAN_PRESENT
        "APS_Groovy_003"| ProcessedEventSeverity.CRITICAL|ProcessedEventSeverity.CRITICAL| "testSp"            | "testPC"          |  "testET"        | FMProcessedEventType.ALARM         |FMProcessedEventType.TECHNICIAN_PRESENT     | "NORMAL_PROC"       |"NORMAL_PROC"       | "FMX_PROCESSED"   |"NOT_SET"         | true         |false       |true
        //NormalProc show present. Rules off. Repeated alarm arrived with record type TECHNICIAN_PRESENT
        "APS_Groovy_003"| ProcessedEventSeverity.CRITICAL|ProcessedEventSeverity.CRITICAL| "testSp"            | "testPC"          |  "testET"        | FMProcessedEventType.ALARM         |FMProcessedEventType.TECHNICIAN_PRESENT     | "NORMAL_PROC"       |"NORMAL_PROC"       | "FMX_PROCESSED"   |"NOT_SET"         | true         |true        |false
        }

    def "Verify alarmSuppressedState and technicianPresentState for active alarm and for clear alarm generated during sync"() {
        given :"An alarm with specific problem AlarmSuppressedMode/FieldTechnicianPresent is received"

        versantDbStatusHolder.isDatabaseAvailable() >> true

        fmFunctionMoService.update("NetworkElement="+nodeNameInput, "alarmSuppressedState", false)
        fmFunctionMoService.update("NetworkElement="+nodeNameInput, "technicianPresentState", false)

        //Alarm1 sent to APS
        List<EventNotification> normalAlarmList = buildEventNotificationList(alarmType1, nodeNameInput, severityInput1, specificProblemInput, probableCauseInput, eventTypeInput, recordTypeInput1, processingTypeInput1, fmxGeneratedInput1, visibility1, targetAdditionalInformation)
        alarmPreProcessor.onEvent(normalAlarmList)

        Map<String, Object> alarmAttributes = readAlarm(specificProblemInput, probableCauseInput, eventTypeInput, "MeContext="+nodeNameInput)
        def activeAlarmRecordType =alarmAttributes.get("recordType")
        def activeAlarmAlarmSuppressedState = fmFunctionMoService.read("NetworkElement="+nodeNameInput, "alarmSuppressedState")
        def activeAlarmTechnicianPresentState = fmFunctionMoService.read("NetworkElement="+nodeNameInput, "technicianPresentState")

        when: "Second alarm is sent to APS"
        // Alarm2 sent to APS
        List<EventNotification> normalAlarmList2 = buildEventNotificationList(alarmType2, nodeNameInput, severityInput2, specificProblemInput, probableCauseInput, eventTypeInput, recordTypeInput2, processingTypeInput2, fmxGeneratedInput2, visibility2, targetAdditionalInformation)
        alarmPreProcessor.onEvent(normalAlarmList2)

        then: "assert expected alarm attributes with received alarm attributes from DB"
        Map<String, Object> finalAlarmAttributes = readAlarm(specificProblemInput, probableCauseInput, eventTypeInput, "MeContext="+nodeNameInput)
        def clearAlarmRecordType =alarmAttributes.get("recordType")
        def clearAlarmAlarmSuppressedState = fmFunctionMoService.read("NetworkElement="+nodeNameInput, "alarmSuppressedState")
        def clearAlarmTechnicianPresentState = fmFunctionMoService.read("NetworkElement="+nodeNameInput, "technicianPresentState")

        assertEquals(activeAlarmRecordType ,expectedactiveAlarmRecordType)
        assertEquals(activeAlarmAlarmSuppressedState ,expectedActiveAlarmAlarmSuppressedState)
        assertEquals(clearAlarmRecordType ,expectedClearAlarmRecordType)
        assertEquals(clearAlarmAlarmSuppressedState ,expectedClearAlarmAlarmSuppressedState)
        assertEquals(activeAlarmAlarmSuppressedState ,expectedActiveAlarmAlarmSuppressedState)
        assertEquals(clearAlarmTechnicianPresentState ,expectedClearAlarmTechnicianPresentState)

        where:
        nodeNameInput           | severityInput1        | severityInput2       | specificProblemInput      | probableCauseInput    | eventTypeInput        | recordTypeInput1       | targetAdditionalInformation                              | recordTypeInput2        | processingTypeInput1     | processingTypeInput2     | fmxGeneratedInput1        | fmxGeneratedInput2        | visibility1       | visibility2       | expectedactiveAlarmRecordType | expectedActiveAlarmAlarmSuppressedState       | expectedClearAlarmRecordType       | expectedClearAlarmAlarmSuppressedState  |expectedActiveAlarmTechnicianPresentState|expectedClearAlarmTechnicianPresentState | alarmType1        | alarmType2
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "AlarmSuppressedMode"     | "Lan Error"           | "Communications alarm"| "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"     | "ALARM"                 | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | true              | true              | "ALARM_SUPPRESSED_ALARM"      | true                                          | "ALARM_SUPPRESSED_ALARM"           | false                                   |               false                     |  false                                  |"NORMAL_ALARM"     | "SYNC_ALARM"
        "APS_Groovy_003"        | "CRITICAL"            | "CRITICAL"           | "FieldTechnicianPresent"  | "Lan Error"           | "Communications alarm"| "ALARM"                | "CI={\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"}"     | "ALARM"                 | "NOT_SET"                | "NOT_SET"                | "NOT_SET"                 | "NOT_SET"                 | true              | true              | "TECHNICIAN_PRESENT"          | false                                         | "TECHNICIAN_PRESENT"               | false                                   |               true                      |  false                                  |"NORMAL_ALARM"     | "SYNC_ALARM"
    }

        private ProcessedAlarmEvent buildProcessedAlarmEvent(final String nodeNameInput, final ProcessedEventSeverity perceivedSeverity, final String specificProblem, final String probableCause, final String eventType, final FMProcessedEventType recordType, final String processingType, final String fmxGenerated, final boolean visibility){
        ProcessedAlarmEvent processedAlarmEvent =new ProcessedAlarmEvent();
        processedAlarmEvent.setFdn("NetworkElement="+nodeNameInput);
        processedAlarmEvent.setPresentSeverity(perceivedSeverity);
        processedAlarmEvent.setSpecificProblem(specificProblem);
        processedAlarmEvent.setProbableCause(probableCause);
        processedAlarmEvent.setEventType(eventType);
        processedAlarmEvent.setEventTime(new Date());
        processedAlarmEvent.setRecordType(recordType);
        processedAlarmEvent.setProcessingType(processingType);
        processedAlarmEvent.setFmxGenerated(fmxGenerated);
        processedAlarmEvent.setVisibility(visibility);
        return processedAlarmEvent;
        }


    private List<EventNotification> buildEventNotificationList(final String alarmType, final String nodeNameInput, final String perceivedSeverity, final String specificProblem, final String probableCause, final String eventType, final String recordType, final String processingType, final String fmxGenerated, final boolean visibility, final String targetAdditionalInformation, final String eventTime = null) {

        List<EventNotification> eventNotifications = new ArrayList<EventNotification>()
        final Map<String, String> additionalInformation = new HashMap<String,String>()

        if (eventTime== null) {
            additionalInformation.put("originalEventTimeFromNode", String.valueOf(System.currentTimeMillis()));
        }
        else {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddkkmmss")
            additionalInformation.put("originalEventTimeFromNode", String.valueOf(simpleDateFormat.parse(eventTime).getTime()));
        }

        additionalInformation.put("fdn", "NetworkElement="+nodeNameInput)
        additionalInformation.put("generatedAlarmId", "123")
        additionalInformation.put(TARGET_ADDITIONAL_INFORMATION, targetAdditionalInformation)

        EventNotification eventNotification = buildEventNotification("MeContext="+nodeNameInput, perceivedSeverity, specificProblem, probableCause, eventType, recordType, processingType, fmxGenerated, additionalInformation, eventTime)
        eventNotification.setVisibility(visibility)
        switch (alarmType) {
            case "NORMAL_ALARM":
                eventNotifications.add(eventNotification)
                break
            case "FMX_ALARM":
                List<String> discriminatorList = new ArrayList<String>()
                discriminatorList.add("hide:hide:1")
                eventNotification.setDiscriminatorList(discriminatorList)
                eventNotifications.add(eventNotification)
                break
            case "SYNC_ALARM":
                EventNotification syncStartAlarm = buildEventNotification("MeContext="+nodeNameInput, "INDETERMINATE","","", "","SYNCHRONIZATION_STARTED", "NOT_SET", "NOT_SET", additionalInformation);
                EventNotification syncEndAlarm = buildEventNotification("MeContext="+nodeNameInput, "INDETERMINATE","","", "","SYNCHRONIZATION_ENDED", "NOT_SET", "NOT_SET", additionalInformation);
                eventNotifications.add(syncStartAlarm);
                eventNotifications.add(syncEndAlarm);
                break
            case "SYNCHRONIZATION_ALARM":
                EventNotification syncStartAlarm = buildEventNotification("MeContext="+nodeNameInput, "INDETERMINATE","","", "","SYNCHRONIZATION_STARTED", "NOT_SET", "NOT_SET", additionalInformation);
                EventNotification syncEndAlarm = buildEventNotification("MeContext="+nodeNameInput, "INDETERMINATE","","", "","SYNCHRONIZATION_ENDED", "NOT_SET", "NOT_SET", additionalInformation);
                eventNotifications.add(syncStartAlarm);
                eventNotifications.add(eventNotification);
                eventNotifications.add(syncEndAlarm);
                break
            case "FMX_UPDATE":
                EventNotification alarm = buildEventNotification("MeContext="+nodeNameInput, perceivedSeverity, specificProblem,probableCause, eventType, recordType, "NOT_SET", "NOT_SET", additionalInformation)
                List<EventNotification> normalAlarmList = new ArrayList<EventNotification>()
                normalAlarmList.add(alarm)
                //Send normal alarm to APS(added here as part of merging the test cases).
                alarmPreProcessor.onEvent(normalAlarmList)
                Map<String, Object> alarmAttributes = readAlarm(specificProblem, probableCause, eventType,"MeContext="+nodeNameInput)
                // build FMX update alarm.
                final Map<String, String> additionalInfoWithFMXAttribute = new HashMap<String,String>();
                additionalInfoWithFMXAttribute.put("fdn", "NetworkElement="+nodeNameInput)
                additionalInfoWithFMXAttribute.put("Operator", "FMX")
                additionalInfoWithFMXAttribute.put("FMX info", "Updated by rule: UPDATE_trigger, module: Availability_UPDATE.")
                additionalInfoWithFMXAttribute.put("eventPoId", String.valueOf(alarmAttributes.get("eventPoId")))
                EventNotification fmxUpdatedAlarm = buildEventNotification("MeContext="+nodeNameInput, perceivedSeverity,specificProblem,probableCause, eventType,"UPDATE", "processingType", "FMX_UPDATED", additionalInfoWithFMXAttribute);
                eventNotifications.add(fmxUpdatedAlarm)
        }
        return eventNotifications
    }

    private EventNotification buildEventNotification(final String managedObjectInstance, final String perceivedSeverity, final String specificProblem,
            final String probableCause, final String eventType, final String recordType, final String processingType, final String fmxGenerated,final Map<String, String> addiationInformation, final String eventTime = null) {
        final EventNotification eventNotification = new EventNotification()
        if (eventTime!=null) {
            eventNotification.setEventTime(eventTime)
            eventNotification.setTimeZone(TimeZone.getDefault().getID())
        }
        eventNotification.setManagedObjectInstance(managedObjectInstance)
        eventNotification.setPerceivedSeverity(perceivedSeverity)
        eventNotification.setSpecificProblem(specificProblem)
        eventNotification.setProbableCause(probableCause)
        eventNotification.setEventType(eventType)
        eventNotification.setRecordType(recordType)
        eventNotification.setFmxGenerated(fmxGenerated)
        eventNotification.setProcessingType(processingType)
        eventNotification.setAdditionalAttributes(addiationInformation)
        return eventNotification
    }
}

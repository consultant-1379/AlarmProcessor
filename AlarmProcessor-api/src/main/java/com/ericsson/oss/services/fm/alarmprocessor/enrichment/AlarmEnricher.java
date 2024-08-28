/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.enrichment;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * Interface for enhancing the received alarm with attributes related to FMX processing.
 */
@EService
@Remote
public interface AlarmEnricher {

    /**
     * Method that enriches the incoming alarm or event after matching it with the active FMX subscriptions.
     * <p>
     * The list of active subscriptions with subscriptionId and {@link EventDiscriminator} are maintained in a cache
     * {@link EventDiscriminatorCacheDefinition}
     * <p>
     * <b>Example:</b>
     * <p>
     * 1. If a subscription has multiple {@link EventDiscriminator}s with different processingTypes(NormalProc or PostProc) Then the all matching
     * EventDiscriminatorId will be set to the EventNotification.
     * <p>
     * <b>Current active FMX subscription present is: </b> { "subscriptionId":"enmTriggerTest", "eventDiscriminators":[ {
     * "discriminatorId":"enmTriggerTest:SimpleENMTrigger:1", "specificProblemList":["SP"], "probableCauseList":["1"], "systemTypeList":[],
     * "severityList":[], "returnSyncAlarms":true, "includeOORList":[], "excludeOORList":[], "noramlProc":true}, {
     * "discriminatorId":"enmTriggerTest:SimpleENMTrigger:4", "specificProblemList":["SP"], "probableCauseList":["1"], "systemTypeList":[],
     * "severityList":[], "returnSyncAlarms":true, "includeOORList":[], "excludeOORList":[], "noramlProc":false} ] }
     * <p>
     * Fmx-adapter has received an event as EventNotification [eventType=ET_COMMUNICATIONS_ALARM, probableCause=1, perceivedSeverity=MINOR,
     * fmEventType=ALARM, specificProblem=SP, eventAgentId=, timeZone=UTC, time=20151109142952.000,
     * managedObjectInstance=MeContext=LTE02ERBS00004,ManagedElement=1, externalEventId=_15166, sourceType=LRAN, translateResult=FORWARD_ALARM,
     * isAcknowledged=false, ackTime=15821015000000.000, operator=, visibility=true,processingType=NOT_SET,fmxGeneratedNOT_SET, additionalAttributes=
     * name=notificationId value=30324 name=generatedAlarmId value=310706750 name=managedObjectClass value= name=fdn
     * value=NetworkElement=LTE02ERBS00004 name=neType value=ERBS name=additionalText value=FMX perf2 update al name=alarmId value=_15166,
     * discriminatorIdList=[]]
     * <p>
     * After checking the subscription visibility and disciriminatorIdList values will be set. As the processingTypes are different for both
     * EventDiscriminators preference should be given for NormalProc. For NormalProc matched alarm visibility should be false(which means alarm will
     * be in hidden state) by default.
     * <p>
     * <b>Then after matching the alarm with above subscription eventNotification will be updated as below: </b>
     * <p>
     * EventNotification [eventType=ET_COMMUNICATIONS_ALARM, probableCause=1, perceivedSeverity=MINOR, fmEventType=ALARM, specificProblem=SP,
     * eventAgentId=, timeZone=UTC, time=20151109142952.000, managedObjectInstance=MeContext=LTE02ERBS00004,ManagedElement=1, externalEventId=_15166,
     * sourceType=LRAN, translateResult=FORWARD_ALARM, isAcknowledged=false, ackTime=15821015000000.000, operator=,
     * <b>visibility=false</b>,processingType=NOT_SET,fmxGeneratedNOT_SET, additionalAttributes= name=notificationId value=30324 name=generatedAlarmId
     * value=310706750 name=managedObjectClass value= name=fdn value=NetworkElement=LTE02ERBS00004 name=neType value=ERBS name=additionalText
     * value=FMX perf2 update al name=alarmId value=_15166,<b> discriminatorIdList=[enmTriggerTest:SimpleENMTrigger:1,enmTriggerTest
     * :SimpleENMTrigger:4]]</b>
     * <p>
     * @param EventNotification
     *            eventNotification
     */
    EventNotification enrichNotification(EventNotification eventNotification);
}

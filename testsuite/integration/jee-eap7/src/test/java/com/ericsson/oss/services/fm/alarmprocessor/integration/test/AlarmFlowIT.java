/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2013
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.integration.test;

import static com.ericsson.oss.mediation.translator.model.Constants.CLEAR_ALL_EVENT_TYPE;
import static com.ericsson.oss.mediation.translator.model.Constants.CLEAR_ALL_PROBABLE_CAUSE;
import static com.ericsson.oss.mediation.translator.model.Constants.CLEAR_ALL_SPECIFIC_PROBLEM;
import static com.ericsson.oss.mediation.translator.model.Constants.NOTIF_TYPE_ALARM;
import static com.ericsson.oss.mediation.translator.model.Constants.NOTIF_TYPE_CLEARALL;
import static com.ericsson.oss.mediation.translator.model.Constants.NOTIF_TYPE_ERROR;
import static com.ericsson.oss.mediation.translator.model.Constants.NOTIF_TYPE_HB_ALARM;
import static com.ericsson.oss.mediation.translator.model.Constants.NOTIF_TYPE_SYNCALARM;
import static com.ericsson.oss.mediation.translator.model.Constants.NOTIF_TYPE_SYNCEND;
import static com.ericsson.oss.mediation.translator.model.Constants.NOTIF_TYPE_SYNCSTART;
import static com.ericsson.oss.mediation.translator.model.Constants.SEV_CLEARED;
import static com.ericsson.oss.mediation.translator.model.Constants.SEV_CRITICAL;
import static com.ericsson.oss.mediation.translator.model.Constants.SEV_INDETERMINATE;
import static com.ericsson.oss.mediation.translator.model.Constants.SEV_MAJOR;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARM_SUPPRESSED_STATE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.DATE_FORMAT;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.CI_GROUP_1;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.CI_GROUP_2;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.ROOT;
import static com.ericsson.oss.services.fm.common.constants.AddInfoConstants.TARGET_ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.ADDITIONAL_TEXT;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.GENERATED_ALARM_ID;
import static com.ericsson.oss.services.fm.common.constants.AdditionalAttrConstants.NOTIFY_CHANGED_ALARM;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ADDITIONAL_INFORMATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.BACKUP_STATUS;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.FDN;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.TREND_INDICATION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.VISIBILITY;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.COLON_DELIMITER;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.EMPTY_STRING;
import static com.ericsson.oss.services.fm.common.constants.GeneralConstants.HASH_DELIMITER;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FMFUNCTION_SUFFIX;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.FM_SUPERVISEDOBJECT_SERVICE_STATE;
import static com.ericsson.oss.services.fm.common.constants.SpPcConstants.ALARMSUPPRESSED_SP;
import static com.ericsson.oss.services.fm.common.constants.SpPcConstants.TECHNICIANPRESENT_SP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.TestChecker;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.AuthenticationHandler;
import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.EventNotificationBatchSender;
import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.FMAlarmOutBusTopicListener;
import com.ericsson.oss.services.fm.common.addinfo.CorrelationType;
import com.ericsson.oss.services.fm.fmxadaptor.models.FMXAdaptorRequest;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

@RunWith(Arquillian.class)
@SuppressWarnings("squid:S2925")
public class AlarmFlowIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmFlowIT.class);
    private static final String APS_TEST_EAR = "APS_TEST_EAR";
    private static final String APS_EAR = "APS_EAR";
    private static final String PIB_EAR = "PIB";
    private static final String RDE_EAR = "RDE_EAR";
    private static final String FMX_EAR = "FMX_EAR";
    private static final String TEST_NODE = "NetworkElement=LTE01ERBS11";
    private static final String SUBSCRIBE = "subscribe";
    private static final String UNSUBSCRIBE = "unsubscribe";
    private static final String FMX_BASE_URI = "http://localhost:8680/fm-fmx-adaptor/fmx/";
    private static final String FMX_SUBSCRIPTION_URI = FMX_BASE_URI.concat("subscription/").concat(SUBSCRIBE);
    private static final String FMX_UNSUBSCRIPTION_URI = FMX_BASE_URI.concat("subscription/").concat(UNSUBSCRIBE);
    private static final String FMX_SHOWHIDE_URI = FMX_BASE_URI.concat("alarmaction/").concat("showHide");
    private static final String FMX_UPDATE_URI = FMX_BASE_URI.concat("alarm/").concat("update");
    private final DefaultHttpClient httpclient = new DefaultHttpClient();
    private final String REDUCED_TARGETADDITIONALINFO = "DN2=ManagedElement\\=1,Equipment\\=1,RbsSubrack\\=RUW1,RbsSlot\\=5,AuxPlugInUnit\\=RUW-2,DeviceGroup\\=RUW,AiDeviceSet\\=1,AiDevice\\=1;CI={\"C\":[{\"I\":\"201f0123-88ca-23a2-7451-8B5872ac457b\",\"n\":\"vRC\"}]};";
    private final String EXTENDED_TARGETADDITIONALINFO_PRIMARY = "DN2=ManagedElement\\=1,Equipment\\=1,RbsSubrack\\=RUW1,RbsSlot\\=5,AuxPlugInUnit\\=RUW-2,DeviceGroup\\=RUW,AiDeviceSet\\=1,AiDevice\\=1;CI = {\"C\": [{\"I\": \"201f0123-88ca-23a2-7451-8B5872ac457b\",\"n\": \"vRC\"}],\"P\":\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\"};";
    private final String EXTENDED_TARGETADDITIONALINFO_SECONDARY = "DN2=ManagedElement\\=1,Equipment\\=1,RbsSubrack\\=RUW1,RbsSlot\\=5,AuxPlugInUnit\\=RUW-2,DeviceGroup\\=RUW,AiDeviceSet\\=1,AiDevice\\=1;CI = {\"C\": [{\"I\": \"201f0123-88ca-23a2-7451-8B5872ac457b\",\"n\": \"vRC\"}],\"S\": [\"81d4fae-7dec-11d0-a765-00a0c91e6bf6\",\"f91a6e32-e523-b217-7C3912ad3012\"]};";
    private final String CI_GROUP_1_STRING = "81d4fae-7dec-11d0-a765-00a0c91e6bf6";
    private final String CI_GROUP_2_STRING = "f91a6e32-e523-b217-7C3912ad3012";

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    @Inject
    private MoCreateBean moCreateBean;

    @Inject
    private FMAlarmOutBusTopicListener topicListener;

    @Inject
    private EventNotificationBatchSender sender;

    @Inject
    private TestChecker testChecker;

    @Deployment(name = PIB_EAR, testable = false, managed = false, order = 1)
    public static EnterpriseArchive createDeployment() {
        LOGGER.info("******Creating PIB deployment and deploying it to server******");
        final File earFile = new File("target/pib/PlatformIntegrationBridge.ear");
        final EnterpriseArchive archive = ShrinkWrap.createFromZipFile(EnterpriseArchive.class, earFile);
        return archive;
    }

    @Deployment(name = RDE_EAR, testable = false, managed = false, order = 2)
    public static Archive<?> createRDEEar() {
        LOGGER.debug("******Getting RDE and deploying it to server******");
        return Artifact.getRateDetectionEngineEAR();
    }

    @Deployment(name = FMX_EAR, testable = false, managed = false, order = 1)
    public static Archive<?> getFmxEar() {
        LOGGER.debug("******FMX ear******");
        return Artifact.getFmxEar();
    }

    @Deployment(name = APS_EAR, testable = false, managed = false, order = 3)
    public static EnterpriseArchive createApsEar() {
        return Artifact.createEnterpriseArchiveDeployment(Artifact.COM_ERICSSON_NMS_SERVICES_APS_EAR);
    }

    @Deployment(name = APS_TEST_EAR, managed = false, testable = true, order = 4)
    public static Archive<?> createApsTestArchive() {
        WebArchive testArchive = null;
        try {
            testArchive = Artifact.createApsTestArchive();
        } catch (final Exception exception) {
            LOGGER.error("Exception caught while creating Test Archive with error mesage : {}", exception.getMessage());
        }
        return testArchive;
    }

    @Test
    @InSequence(1)
    public void deployEars() {
        this.deployer.deploy(PIB_EAR);
        this.deployer.deploy(RDE_EAR);
        this.deployer.deploy(FMX_EAR);
        this.deployer.deploy(APS_EAR);
    }

    @Test
    @InSequence(2)
    public void deployaps() {
        this.deployer.deploy(APS_TEST_EAR);
    }

    @Test
    @OperateOnDeployment(APS_TEST_EAR)
    @InSequence(3)
    public void testActivateMessageListeners() throws Exception {
        final String channelName = "jms:queue/fmalarmqueue";
        final String URI = "http://localhost:8680/alarmprocessor/fmcommon/messagelisteners/activate?jndiChannelName=" + channelName;
        final int returnStatus = fetchPutAction(URI);
        // assert for Response.Status.OK
        assertEquals(200, returnStatus);
    }

    @Test
    @InSequence(4)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_ErrorAlarm() {
        try {
            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ERROR);
            LOGGER.info("Start of test_ErrorAlarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(120, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_ErrorAlarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(5)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_NormalAlarm() {
        try {
            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "12345");
            LOGGER.info("Start of test_NormalAlarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_NormalAlarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(6)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_EnrichedNormalAlarm() {
        try {
            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "12345");
            event.addAdditionalAttribute(TARGET_ADDITIONAL_INFORMATION, this.EXTENDED_TARGETADDITIONALINFO_SECONDARY);

            LOGGER.info("Start of test_EnrichedNormalAlarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_EnrichedNormalAlarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());

        // Check the ProcessedAlarmEvent towards NBIs
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertEquals(this.EXTENDED_TARGETADDITIONALINFO_SECONDARY, event.getAdditionalInformation().get(TARGET_ADDITIONAL_INFORMATION));

        // Check if correlation information have been correctly stored in database
        final Map<String, Object> correlationInformation = this.getCorrelationInformation(12345L);
        assertEquals(this.REDUCED_TARGETADDITIONALINFO, correlationInformation.get(TARGET_ADDITIONAL_INFORMATION));
        assertEquals(CorrelationType.SECONDARY.toString(),correlationInformation.get(ROOT));
        assertEquals(this.CI_GROUP_2_STRING, correlationInformation.get(CI_GROUP_2));
        assertEquals(this.CI_GROUP_1_STRING, correlationInformation.get(CI_GROUP_1));

        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(7)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_SyncAlarms() {
        try {
            final EventNotification syncStart = new EventNotification();
            syncStart.setRecordType(NOTIF_TYPE_SYNCSTART);
            syncStart.addAdditionalAttribute(FDN, TEST_NODE);
            syncStart.setManagedObjectInstance(TEST_NODE);
            syncStart.setEventTime(getEventTime(new Date()));

            final EventNotification syncEnd = new EventNotification();
            syncEnd.setRecordType(NOTIF_TYPE_SYNCEND);
            syncEnd.addAdditionalAttribute(FDN, TEST_NODE);
            syncEnd.setManagedObjectInstance(TEST_NODE);
            syncEnd.setEventTime(getEventTime(new Date()));

            // Send a new alarm which is not already present in db
            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_SYNCALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "123467");
            event.setRecordType(NOTIF_TYPE_SYNCALARM);
            Date theDate = new Date();
            event.setEventTime(getEventTime(theDate));
            LOGGER.info("Start of test_SyncAlarms testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();

            // Send a new alarm which is already present on the node
            final EventNotification event1 = this.prepareAlarm(NOTIF_TYPE_SYNCALARM);
            event1.setRecordType(NOTIF_TYPE_SYNCALARM);
            theDate.setMinutes(theDate.getMinutes()+1);
            event1.setEventTime(getEventTime(theDate));
            event1.addAdditionalAttribute(GENERATED_ALARM_ID, "123467");
            event1.setPerceivedSeverity(SEV_MAJOR);
            events.add(syncStart);
            events.add(event);
            events.add(event1);
            events.add(syncEnd);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_NormalAlarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(12345 == event.getAlarmId());
        assertTrue(FMProcessedEventType.SYNCHRONIZATION_ALARM.equals(event.getRecordType()));
        assertTrue(event.getEventPOId() > 0);
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(8)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_SyncEnrichedAlarms() {
        try {
            final EventNotification syncStart = new EventNotification();
            syncStart.setRecordType(NOTIF_TYPE_SYNCSTART);
            syncStart.addAdditionalAttribute(FDN, TEST_NODE);
            syncStart.setManagedObjectInstance(TEST_NODE);
            syncStart.setEventTime(getEventTime(new Date()));

            final EventNotification syncEnd = new EventNotification();
            syncEnd.setRecordType(NOTIF_TYPE_SYNCEND);
            syncEnd.addAdditionalAttribute(FDN, TEST_NODE);
            syncEnd.setManagedObjectInstance(TEST_NODE);
            syncEnd.setEventTime(getEventTime(new Date()));

            // Send a new alarm which is not already present in db final
            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_SYNCALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "7654321");
            event.setRecordType(NOTIF_TYPE_SYNCALARM);
            event.addAdditionalAttribute(TARGET_ADDITIONAL_INFORMATION, this.EXTENDED_TARGETADDITIONALINFO_PRIMARY);

            LOGGER.info("Start of test_Enriched SyncAlarms testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();

            events.add(syncStart);
            events.add(event);
            events.add(syncEnd);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_EnrichedNormalAlarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());

        // Check the ProcessedAlarmEvent towards NBIs
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 2);
        assertTrue(event.getEventPOId() > 0);
        assertEquals(this.EXTENDED_TARGETADDITIONALINFO_PRIMARY, event.getAdditionalInformation().get(TARGET_ADDITIONAL_INFORMATION));

        // Check if correlation information have been correctly stored in database
        final Map<String, Object> correlationInformation = this.getCorrelationInformation(7654321L);


        assertEquals(this.REDUCED_TARGETADDITIONALINFO, correlationInformation.get(TARGET_ADDITIONAL_INFORMATION));
        assertEquals(CorrelationType.PRIMARY.toString(), correlationInformation.get(ROOT));
        assertEquals(this.CI_GROUP_1_STRING, correlationInformation.get(CI_GROUP_1));
        assertEquals(null, correlationInformation.get(CI_GROUP_2));

        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(9)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_HeartBeat_Alarm() {
        try {
            final EventNotification event = this.prepareHeartBeatAlarm(false);
            LOGGER.info("Start of test_HeartBeat_Alarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_HeartBeat_Alarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.HEARTBEAT_ALARM.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CRITICAL.equals(event.getPresentSeverity()));
        final String currentServiceState = (String) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                FM_SUPERVISEDOBJECT_SERVICE_STATE);
        assertTrue(FmSyncStatus100.HEART_BEAT_FAILURE.name().equals(currentServiceState));
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(10)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_HeartBeat_Alarm_On_Node_AlreadyInHb() {
        try {
            final EventNotification event = this.prepareHeartBeatAlarm(false);
            LOGGER.info("Start of test_HeartBeat_Alarm_On_Node_AlreadyInHb testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_HeartBeat_Alarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.HEARTBEAT_ALARM.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CRITICAL.equals(event.getPresentSeverity()));
        assertTrue(1 == event.getRepeatCount());
        assertTrue(null != event.getInsertTime());
        final String currentServiceState = (String) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                FM_SUPERVISEDOBJECT_SERVICE_STATE);
        assertTrue(FmSyncStatus100.HEART_BEAT_FAILURE.name().equals(currentServiceState));
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(11)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_HeartBeat_Clear_Alarm() {
        try {
            final EventNotification event = this.prepareHeartBeatAlarm(true);
            LOGGER.info("Start of test_HeartBeat_Clear_Alarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_HeartBeat_Clear_Alarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.HEARTBEAT_ALARM.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CLEARED.equals(event.getPresentSeverity()));
        this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX), FM_SUPERVISEDOBJECT_SERVICE_STATE);
        final String currentServiceState = (String) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                FM_SUPERVISEDOBJECT_SERVICE_STATE);
        assertTrue(FmSyncStatus100.IN_SERVICE.name().equals(currentServiceState));
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(12)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_NodeSuspended_Alarm() {
        try {
            final EventNotification event = this.prepareNodeSuspendedAlarm(false);
            LOGGER.info("Start of test_NodeSuspended_Alarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_NodeSuspended_Alarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.NODE_SUSPENDED.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CRITICAL.equals(event.getPresentSeverity()));
        final String currentServiceState = (String) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                FM_SUPERVISEDOBJECT_SERVICE_STATE);
        assertTrue(FmSyncStatus100.NODE_SUSPENDED.name().equals(currentServiceState));
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(13)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_NodeSuspended_Alarm_On_Node_AlreadySuspended() {
        try {
            final EventNotification event = this.prepareNodeSuspendedAlarm(false);
            LOGGER.info("Start of test_NodeSuspended_Alarm_On_Node_AlreadySuspended testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_NodeSuspended_Alarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.NODE_SUSPENDED.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CRITICAL.equals(event.getPresentSeverity()));
        assertTrue(1 == event.getRepeatCount());
        assertTrue(null != event.getInsertTime());
        final String currentServiceState = (String) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                FM_SUPERVISEDOBJECT_SERVICE_STATE);
        assertTrue(FmSyncStatus100.NODE_SUSPENDED.name().equals(currentServiceState));
        this.topicListener.resetLatch();

    }

    @Test
    @InSequence(14)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_NodeSuspended_Clear_Alarm() {
        try {
            final EventNotification event = this.prepareNodeSuspendedAlarm(true);
            LOGGER.info("Start of test_NodeSuspended_Clear_Alarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_NodeSuspended_Clear_Alarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.NODE_SUSPENDED.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CLEARED.equals(event.getPresentSeverity()));
        assertTrue(1 == event.getRepeatCount());
        assertTrue(null != event.getInsertTime());
        final String currentServiceState = (String) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                FM_SUPERVISEDOBJECT_SERVICE_STATE);
        assertTrue(FmSyncStatus100.IN_SERVICE.name().equals(currentServiceState));
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(15)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_TechnicianPresent_Alarm() {
        try {
            final EventNotification event = this.prepareTPOrASAAlarm(false, TECHNICIANPRESENT_SP);
            LOGGER.info("Start of test_TechnicianPresent_Alarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_TechnicianPresent_Alarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.TECHNICIAN_PRESENT.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CRITICAL.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        final Boolean technicianPresentState = (Boolean) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                TECHNICIAN_PRESENT_STATE);
        assertTrue(technicianPresentState);
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(16)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_TechnicianPresent_Clear_Alarm() {
        try {
            final EventNotification event = this.prepareTPOrASAAlarm(true, TECHNICIANPRESENT_SP);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            LOGGER.info("Start of test_TechnicianPresent_Clear_Alarm testcase!!: {}", event);
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_TechnicianPresent_Clear_Alarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.TECHNICIAN_PRESENT.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CLEARED.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        final Boolean technicianPresentState = (Boolean) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                TECHNICIAN_PRESENT_STATE);
        assertFalse(technicianPresentState);
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(17)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_AlarmSuppressed_Alarm() {
        try {
            final EventNotification event = this.prepareTPOrASAAlarm(false, ALARMSUPPRESSED_SP);
            LOGGER.info("Start of test_AlarmSuppressed_Alarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_AlarmSuppressed_Alarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.ALARM_SUPPRESSED_ALARM.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CRITICAL.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        final Boolean alarmSuppressedState = (Boolean) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                ALARM_SUPPRESSED_STATE);
        assertTrue(alarmSuppressedState);
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(18)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_AlarmSuppressed_Clear_Alarm() {
        try {
            // registerJmsConsumer(listener);
            final EventNotification event = this.prepareTPOrASAAlarm(true, ALARMSUPPRESSED_SP);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            LOGGER.info("Start of test_AlarmSuppressed_Clear_Alarm testcase!!: {}", event);
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_AlarmSuppressed_Clear_Alarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.ALARM_SUPPRESSED_ALARM.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CLEARED.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        final Boolean alarmSuppressedState = (Boolean) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                ALARM_SUPPRESSED_STATE);
        assertFalse(alarmSuppressedState);
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(19)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_AlarmCorrelation_Original() {
        try {
            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "1234");
            event.setSpecificProblem("alarm_Correlation");
            LOGGER.info("Start of test_AlarmCorrelation_Original testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_AlarmCorrelation_Original {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.ALARM.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CRITICAL.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        assertTrue(1234 == event.getAlarmId());
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(20)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_AlarmCorrelation_Clear() {
        try {
            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "1234");
            event.setSpecificProblem("alarm_Correlation");
            event.setPerceivedSeverity(SEV_CLEARED);
            LOGGER.info("Start of test_AlarmCorrelation_Clear testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_AlarmCorrelation_Clear {}", e);
        }

        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.ALARM.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CLEARED.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        assertTrue(1234 == event.getAlarmId());
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(21)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_RepeatedAlarm() {
        try {
            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "5678");
            event.setSpecificProblem("repeatedAlarm");
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
            // Now Send Repeat Alarm
            final EventNotification event1 = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event1.addAdditionalAttribute(GENERATED_ALARM_ID, "5678");
            event1.addAdditionalAttribute(NOTIFY_CHANGED_ALARM, "true");
            event1.setSpecificProblem("repeatedAlarm");
            event1.setPerceivedSeverity(SEV_MAJOR);
            event1.setEventTime(event.getEventTime());
            LOGGER.info("Start of test_RepeatedAlarm testcase!!: {}", event);
            final List<EventNotification> events1 = new ArrayList<EventNotification>();
            events1.add(event1);
            this.sendJmsMessage(events1);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_RepeatedAlarm {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        //assertTrue(FMProcessedEventType.REPEATED_ALARM.equals(event.getRecordType()));
        //assertTrue(ProcessedEventSeverity.MAJOR.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(22)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_ClearAll() {
        try {
            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_CLEARALL);
            LOGGER.info("Start of test_ClearAlarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_ClearAll {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(23)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_OutOfSync() {
        try {
            final EventNotification event = this.prepareOutOfSyncAlarm(false);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "54321");
            LOGGER.info("Start of test_OutOfSync testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_OutOfSync {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.OUT_OF_SYNC.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.WARNING.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        final String currentServiceState = (String) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                FM_SUPERVISEDOBJECT_SERVICE_STATE);
        assertTrue(FmSyncStatus100.OUT_OF_SYNC.name().equals(currentServiceState));
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(24)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_OutOfSync_On_NodeAlready_OutOfSync() {
        try {
            final EventNotification event = this.prepareOutOfSyncAlarm(false);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "54321");
            LOGGER.info("Start of test_OutOfSync_On_NodeAlready_OutOfSync testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_OutOfSync {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.OUT_OF_SYNC.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.WARNING.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        assertTrue(1 == event.getRepeatCount());
        final String currentServiceState = (String) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                FM_SUPERVISEDOBJECT_SERVICE_STATE);
        assertTrue(FmSyncStatus100.OUT_OF_SYNC.name().equals(currentServiceState));
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(25)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_OutOfSync_Clear() {
        try {
            final EventNotification event = this.prepareOutOfSyncAlarm(true);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "54321");
            LOGGER.info("Start of test_OutOfSync_Clear testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_OutOfSync_Clear {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.OUT_OF_SYNC.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.CLEARED.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        final String currentServiceState = (String) this.moCreateBean.getManagedObjectAttribute(TEST_NODE.concat(FMFUNCTION_SUFFIX),
                FM_SUPERVISEDOBJECT_SERVICE_STATE);
        assertTrue(FmSyncStatus100.IN_SERVICE.name().equals(currentServiceState));
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(26)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_UnKnownSeverityEventNotification_ConvertedToIndeterminate() {
        try {
            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.setPerceivedSeverity("UNKNOWN");
            LOGGER.info("Start of test_UnKnownSeverityEventNotification_ConvertedToIndeterminate testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_UnKnownSeverityEventNotification_ConvertedToIndeterminate {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());
        final ProcessedAlarmEvent processedAlarmEvent = this.topicListener.getReceivedMessages().get(this.topicListener.getReceivedMessages().size() - 1);
        LOGGER.info("The event in  test_UnKnownSeverityEventNotification_ConvertedToIndeterminate testcase!!: {}", processedAlarmEvent);
        assertEquals(SEV_INDETERMINATE, processedAlarmEvent.getPresentSeverity().name());
        final List<ProcessedAlarmEvent> receivedMessages = this.topicListener.getReceivedMessages();
        final ProcessedAlarmEvent event = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(FMProcessedEventType.ALARM.equals(event.getRecordType()));
        assertTrue(ProcessedEventSeverity.INDETERMINATE.equals(event.getPresentSeverity()));
        assertTrue(null != event.getInsertTime());
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(27)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_NonSyncableAlarm() {
        try {
            List<ProcessedAlarmEvent> receivedMessages = null;
            ProcessedAlarmEvent processedAlarmEvent = null;
            final EventNotification event = this.prepareAlarm(FMProcessedEventType.NON_SYNCHABLE_ALARM.name());
            event.setRecordType(FMProcessedEventType.NON_SYNCHABLE_ALARM.name());
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "56789");
            event.setSpecificProblem("nonSyncableAlarm");
            event.setPerceivedSeverity(SEV_CRITICAL);
            LOGGER.info("Start of test_NonSyncableAlarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
            assertEquals(0, this.topicListener.getCountInLatch());
            receivedMessages = this.topicListener.getReceivedMessages();
            processedAlarmEvent = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
            assertTrue(FMProcessedEventType.NON_SYNCHABLE_ALARM.equals(processedAlarmEvent.getRecordType()));
            assertTrue("nonSyncableAlarm".equals(processedAlarmEvent.getSpecificProblem()));
            assertTrue(ProcessedEventSeverity.CRITICAL.equals(processedAlarmEvent.getPresentSeverity()));
            this.topicListener.resetLatch();
            // send the same alarm again and assert that record type is not changed
            event.setPerceivedSeverity(SEV_MAJOR);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
            assertEquals(0, this.topicListener.getCountInLatch());
            receivedMessages = this.topicListener.getReceivedMessages();
            processedAlarmEvent = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
            assertTrue(FMProcessedEventType.REPEATED_NON_SYNCHABLE.equals(processedAlarmEvent.getRecordType()));
            assertTrue(ProcessedEventSeverity.MAJOR.equals(processedAlarmEvent.getPresentSeverity()));
            assertTrue(null != processedAlarmEvent.getInsertTime());
            this.topicListener.resetLatch();
        } catch (final Exception e) {
            LOGGER.error("Exception in test_NonSyncableAlarm {}", e);
        }
    }

    @Test
    @InSequence(28)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_RepeatedNonSyncableAlarm() {
        try {
            List<ProcessedAlarmEvent> receivedMessages = null;
            ProcessedAlarmEvent processedAlarmEvent = null;
            final EventNotification event = this.prepareAlarm(FMProcessedEventType.REPEATED_NON_SYNCHABLE.name());
            event.setRecordType(FMProcessedEventType.REPEATED_NON_SYNCHABLE.name());
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "999999");
            event.setSpecificProblem("RepeatedNonSyncableAlarm");
            event.setPerceivedSeverity(SEV_INDETERMINATE);
            LOGGER.info("Start of test_RepeatedNonSyncableAlarm testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
            assertEquals(0, this.topicListener.getCountInLatch());
            receivedMessages = this.topicListener.getReceivedMessages();
            processedAlarmEvent = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
            assertTrue(FMProcessedEventType.REPEATED_NON_SYNCHABLE.equals(processedAlarmEvent.getRecordType()));
            assertTrue("RepeatedNonSyncableAlarm".equals(processedAlarmEvent.getSpecificProblem()));
            assertTrue(ProcessedEventSeverity.INDETERMINATE.equals(processedAlarmEvent.getPresentSeverity()));
            this.topicListener.resetLatch();
            // send the same alarm again and assert that record type is not changed
            event.setPerceivedSeverity(SEV_CRITICAL);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
            assertEquals(0, this.topicListener.getCountInLatch());
            receivedMessages = this.topicListener.getReceivedMessages();
            processedAlarmEvent = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
            assertTrue(FMProcessedEventType.REPEATED_NON_SYNCHABLE.equals(processedAlarmEvent.getRecordType()));
            assertTrue(ProcessedEventSeverity.CRITICAL.equals(processedAlarmEvent.getPresentSeverity()));
            assertTrue(null != processedAlarmEvent.getInsertTime());
            this.topicListener.resetLatch();
        } catch (final Exception e) {
            LOGGER.error("Exception in test_RepeatedNonSyncableAlarm {}", e);
        }
    }

    @Test
    @InSequence(29)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_RepeatedNonSyncableAlarm_Clear() {
        try {
            List<ProcessedAlarmEvent> receivedMessages = null;
            ProcessedAlarmEvent processedAlarmEvent = null;
            final EventNotification event = this.prepareAlarm(FMProcessedEventType.REPEATED_NON_SYNCHABLE.name());
            event.setRecordType(FMProcessedEventType.REPEATED_NON_SYNCHABLE.name());
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "999999");
            event.setSpecificProblem("RepeatedNonSyncableAlarm");
            event.setPerceivedSeverity(SEV_CLEARED);
            LOGGER.info("Start of test_RepeatedNonSyncableAlarm_Clear testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
            assertEquals(0, this.topicListener.getCountInLatch());
            receivedMessages = this.topicListener.getReceivedMessages();
            processedAlarmEvent = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
            assertTrue(FMProcessedEventType.REPEATED_NON_SYNCHABLE.equals(processedAlarmEvent.getRecordType()));
            assertTrue(event.getSpecificProblem().equals(processedAlarmEvent.getSpecificProblem()));
            assertTrue(ProcessedEventSeverity.CLEARED.equals(processedAlarmEvent.getPresentSeverity()));
            this.topicListener.resetLatch();
        } catch (final Exception e) {
            LOGGER.error("Exception in test_RepeatedNonSyncableAlarm_Clear {}", e);
        }
    }

    @InSequence(30)
    @Test
    @RunAsClient
    public void test_DownwardAckEnable() throws Exception {
        final HttpGet httpget = new HttpGet(
                "http://localhost:8680/pib/configurationService"
                        + "/updateConfigParameterValue?paramName=oscillationAlarmCorrelation&paramValue=true");
        AuthenticationHandler.addUserPassword(httpget);
        final HttpResponse response = this.httpclient.execute(httpget);
        assertNotNull("ClientResponse should not be null", response);
        assertEquals("Expecting status not equals", Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());
        Thread.sleep(2000);
    }

    @Test
    @InSequence(31)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_NonSyncableAlarm_WithOscillation() {
        try {
            List<ProcessedAlarmEvent> receivedMessages = null;
            ProcessedAlarmEvent processedAlarmEvent = null;
            final EventNotification event = this.prepareAlarm(FMProcessedEventType.NON_SYNCHABLE_ALARM.name());
            event.setRecordType(FMProcessedEventType.NON_SYNCHABLE_ALARM.name());
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "999999");
            event.setSpecificProblem("RepeatedNonSyncableAlarm");
            event.setPerceivedSeverity(SEV_CRITICAL);
            LOGGER.info("Start of test_NonSyncableAlarm_WithOscillation testcase!!: {}", event);
            final List<EventNotification> events = new ArrayList<EventNotification>();
            events.add(event);
            this.sendJmsMessage(events);
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
            assertEquals(0, this.topicListener.getCountInLatch());
            receivedMessages = this.topicListener.getReceivedMessages();
            processedAlarmEvent = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
            assertTrue(FMProcessedEventType.REPEATED_NON_SYNCHABLE.equals(processedAlarmEvent.getRecordType()));
            assertTrue(event.getSpecificProblem().equals(processedAlarmEvent.getSpecificProblem()));
            assertTrue(ProcessedEventSeverity.CLEARED.equals(processedAlarmEvent.getPreviousSeverity()));
            assertTrue(ProcessedEventSeverity.CRITICAL.equals(processedAlarmEvent.getPresentSeverity()));
            this.topicListener.resetLatch();
        } catch (final Exception e) {
            LOGGER.error("Exception in test_NonSyncableAlarm_WithOscillation {}", e);
        }
    }

    @Test
    @InSequence(32)
    @OperateOnDeployment(APS_TEST_EAR)
    public void testNullSubscription() throws Exception {
        final String fmxRequest = "{" + "\"eventDiscriminators\":[]," + "\"subscriptionId\":\"subscription1\"}";
        fetchPostAction(fmxRequest, FMX_SUBSCRIPTION_URI);

        final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
        event.addAdditionalAttribute(GENERATED_ALARM_ID, "12345");
        LOGGER.info("Sending the Alarm to fmalarmqueue: {}", event);
        final List<EventNotification> events = new ArrayList<EventNotification>();
        events.add(event);
        this.sendJmsMessage(events);
        this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        assertEquals(0, this.topicListener.getCountInLatch());
        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(33)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_Alarm_Shown_After_NormalProc_Sent_As_NewAlarm() throws Exception {
        List<ProcessedAlarmEvent> receivedMessages = null;
        ProcessedAlarmEvent processedAlarmEvent = null;
        final String fmxRequest = "{"
                + "\"eventDiscriminators\":[{\"discriminatorId\":\"Normalproc:Normalproc:1\",\"specificProblemList\":null,\"probableCauseList\":null,\"systemTypeList\":[\"MSRBS_V1\",\"RadioTNode\",\"ERBS\",\"RadioNode\"],"
                + "\"severityList\":null,\"returnSyncAlarms\":false,\"includeOORList\":null,\"excludeOORList\":null,\"normalProc\":true,\"ruleType\":null,\"moduleName\":null,\"ruleName\":null,\"eventTypeList\":null}],"
                + "\"subscriptionId\":\"Normalproc\",\"poId\":null,\"showAlarm\":null,\"poIds\":null,\"alarmAttributes\":null,\"showHiddenAlarms\":null,\"fmxToken\":\"Normalproc\",\"operator\":null,\"neType\":null,\"action\":null}\"";
        fetchPostAction(fmxRequest, FMX_SUBSCRIPTION_URI);

        final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
        event.addAdditionalAttribute(GENERATED_ALARM_ID, "9999999");
        LOGGER.info("testNormalProcSubscription::Sending the Alarm to fmalarmqueue: {}", event);
        final List<EventNotification> events = new ArrayList<EventNotification>();
        events.add(event);
        this.sendJmsMessage(events);
        this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        this.topicListener.resetLatch();
        final Long poId = this.moCreateBean.findPoId(9999999L);
        final String showRequest = "{"
                + "\"eventDiscriminators\":null,\"subscriptionId\":null,\"poId\":"
                + String.valueOf(poId)
                + ",\"showAlarm\":true,\"poIds\":null,\"alarmAttributes\":null,\"showHiddenAlarms\":null,\"fmxToken\":null,\"operator\":null,\"neType\":null,\"action\":null}\"";
        fetchPostAction(showRequest, FMX_SHOWHIDE_URI);
        this.topicListener.LATCH.await(60, TimeUnit.SECONDS);
        receivedMessages = this.topicListener.getReceivedMessages();
        processedAlarmEvent = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        //assertTrue(ProcessedLastAlarmOperation.NEW.equals(processedAlarmEvent.getLastAlarmOperation()));
        this.topicListener.resetLatch();
        fetchPostAction(fmxRequest, FMX_UNSUBSCRIPTION_URI);
        this.topicListener.LATCH.await(10, TimeUnit.SECONDS);
    }

    @Test
    @InSequence(34)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_Alarm_Hidden_After_Processing_ResultsIn_Dummy_ClearAlarm() throws Exception {
        List<ProcessedAlarmEvent> receivedMessages = null;
        ProcessedAlarmEvent processedAlarmEvent = null;
        final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
        event.addAdditionalAttribute(GENERATED_ALARM_ID, "88888888");
        LOGGER.info("testNormalProcSubscriptionWithRetunSyncTrue::Sending the Alarm to fmalarmqueue: {}", event);
        final List<EventNotification> events = new ArrayList<EventNotification>();
        events.add(event);
        this.sendJmsMessage(events);
        this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        this.topicListener.resetLatch();
        final String fmxRequest = "{"
                + "\"eventDiscriminators\":[{\"discriminatorId\":\"Normalproc:Normalproc:1\",\"specificProblemList\":null,\"probableCauseList\":null,\"systemTypeList\":[\"MSRBS_V1\",\"RadioTNode\",\"ERBS\",\"RadioNode\"],"
                + "\"severityList\":null,\"returnSyncAlarms\":true,\"includeOORList\":null,\"excludeOORList\":null,\"normalProc\":true,\"ruleType\":null,\"moduleName\":null,\"ruleName\":null,\"eventTypeList\":null}],"
                + "\"subscriptionId\":\"Normalproc\",\"poId\":null,\"showAlarm\":null,\"poIds\":null,\"alarmAttributes\":null,\"showHiddenAlarms\":null,\"fmxToken\":\"Normalproc\",\"operator\":null,\"neType\":null,\"action\":null}\"";
        fetchPostAction(fmxRequest, FMX_SUBSCRIPTION_URI);
        this.topicListener.LATCH.await(60, TimeUnit.SECONDS);
        this.topicListener.resetLatch();
        final Long poId = this.moCreateBean.findPoId(88888888L);
        final String hideRequest = "{"
                + "\"eventDiscriminators\":null,\"subscriptionId\":null,\"poId\":"
                + String.valueOf(poId)
                + ",\"showAlarm\":false,\"poIds\":null,\"alarmAttributes\":null,\"showHiddenAlarms\":null,\"fmxToken\":null,\"operator\":null,\"neType\":null,\"action\":null}\"";
        fetchPostAction(hideRequest, FMX_SHOWHIDE_URI);
        this.topicListener.LATCH.await(60, TimeUnit.SECONDS);
        receivedMessages = this.topicListener.getReceivedMessages();
        processedAlarmEvent = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        //assertTrue(ProcessedLastAlarmOperation.CLEAR.equals(processedAlarmEvent.getLastAlarmOperation()));
        fetchPostAction(fmxRequest, FMX_UNSUBSCRIPTION_URI);
        this.topicListener.LATCH.await(10, TimeUnit.SECONDS);
    }

    @Test
    @InSequence(35)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_Alarm_ShownWithUpdate_After_NormalProc_Sent_As_NewAlarm() throws Exception {
        List<ProcessedAlarmEvent> receivedMessages = null;
        ProcessedAlarmEvent processedAlarmEvent = null;
        final String fmxRequest = "{"
                + "\"eventDiscriminators\":[{\"discriminatorId\":\"Normalproc:Normalproc:1\",\"specificProblemList\":null,\"probableCauseList\":null,\"systemTypeList\":[\"MSRBS_V1\",\"RadioTNode\",\"ERBS\",\"RadioNode\"],"
                + "\"severityList\":null,\"returnSyncAlarms\":false,\"includeOORList\":null,\"excludeOORList\":null,\"normalProc\":true,\"ruleType\":null,\"moduleName\":null,\"ruleName\":null,\"eventTypeList\":null}],"
                + "\"subscriptionId\":\"Normalproc\",\"poId\":null,\"showAlarm\":null,\"poIds\":null,\"alarmAttributes\":null,\"showHiddenAlarms\":null,\"fmxToken\":\"Normalproc\",\"operator\":null,\"neType\":null,\"action\":null}\"";
        fetchPostAction(fmxRequest, FMX_SUBSCRIPTION_URI);

        final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
        event.addAdditionalAttribute(GENERATED_ALARM_ID, "77777777777");
        LOGGER.info("testNormalProcUpdateSubscription::Sending the Alarm to fmalarmqueue: {}", event);
        final List<EventNotification> events = new ArrayList<EventNotification>();
        events.add(event);
        this.sendJmsMessage(events);
        this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        this.topicListener.resetLatch();
        final Long poId = this.moCreateBean.findPoId(77777777777L);
        final Map<String, Object> alarmAttributes = this.moCreateBean.getAlarmAttributes(poId);
        alarmAttributes.put(VISIBILITY, true);
        alarmAttributes.put("TestUpdateAttribute", "TestUpdateAttributeValue");
        final FMXAdaptorRequest fmxAdaptorRequest = this.getFmxAdaptorRequest(poId, alarmAttributes);
        final String attributeMapAsJSONString = this.getJsonString(fmxAdaptorRequest);

        fetchPutActionNew(attributeMapAsJSONString, FMX_UPDATE_URI);
        this.topicListener.LATCH.await(60, TimeUnit.SECONDS);
        receivedMessages = this.topicListener.getReceivedMessages();
        processedAlarmEvent = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(ProcessedLastAlarmOperation.NEW.equals(processedAlarmEvent.getLastAlarmOperation()));
        this.topicListener.resetLatch();
        fetchPostAction(fmxRequest, FMX_UNSUBSCRIPTION_URI);
        this.topicListener.LATCH.await(10, TimeUnit.SECONDS);
    }

    @Test
    @InSequence(36)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_Alarm_HiddenWithUpdate_After_Processing_ResultsIn_Dummy_ClearAlarm() throws Exception {
        List<ProcessedAlarmEvent> receivedMessages = null;
        ProcessedAlarmEvent processedAlarmEvent = null;
        final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
        event.addAdditionalAttribute(GENERATED_ALARM_ID, "6666666666");
        LOGGER.info("testNormalProcUpdateSubscriptionWithRetunSyncTrue::Sending the Alarm to fmalarmqueue: {}", event);
        final List<EventNotification> events = new ArrayList<EventNotification>();
        events.add(event);
        this.sendJmsMessage(events);
        this.topicListener.LATCH.await(30, TimeUnit.SECONDS);
        this.topicListener.resetLatch();
        final String fmxRequest = "{"
                + "\"eventDiscriminators\":[{\"discriminatorId\":\"Normalproc:Normalproc:1\",\"specificProblemList\":null,\"probableCauseList\":null,\"systemTypeList\":[\"MSRBS_V1\",\"RadioTNode\",\"ERBS\",\"RadioNode\"],"
                + "\"severityList\":null,\"returnSyncAlarms\":true,\"includeOORList\":null,\"excludeOORList\":null,\"normalProc\":true,\"ruleType\":null,\"moduleName\":null,\"ruleName\":null,\"eventTypeList\":null}],"
                + "\"subscriptionId\":\"Normalproc\",\"poId\":null,\"showAlarm\":null,\"poIds\":null,\"alarmAttributes\":null,\"showHiddenAlarms\":null,\"fmxToken\":\"Normalproc\",\"operator\":null,\"neType\":null,\"action\":null}\"";
        fetchPostAction(fmxRequest, FMX_SUBSCRIPTION_URI);
        this.topicListener.LATCH.await(60, TimeUnit.SECONDS);
        this.topicListener.resetLatch();
        final Long poId = this.moCreateBean.findPoId(6666666666L);
        final Map<String, Object> alarmAttributes = this.moCreateBean.getAlarmAttributes(poId);
        alarmAttributes.put(VISIBILITY, false);
        alarmAttributes.put("TestUpdateAttribute", "TestUpdateAttributeValueHide");
        final FMXAdaptorRequest fmxAdaptorRequest = this.getFmxAdaptorRequest(poId, alarmAttributes);
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(Feature.WRITE_NULL_MAP_VALUES, false);
        String attributeMapAsJSONString = EMPTY_STRING;
        attributeMapAsJSONString = objectMapper.writeValueAsString(fmxAdaptorRequest);

        fetchPutActionNew(attributeMapAsJSONString, FMX_UPDATE_URI);
        this.topicListener.LATCH.await(60, TimeUnit.SECONDS);
        receivedMessages = this.topicListener.getReceivedMessages();
        processedAlarmEvent = this.topicListener.getReceivedMessages().get(receivedMessages.size() - 1);
        assertTrue(ProcessedLastAlarmOperation.CLEAR.equals(processedAlarmEvent.getLastAlarmOperation()));
        fetchPostAction(fmxRequest, FMX_UNSUBSCRIPTION_URI);
        this.topicListener.LATCH.await(10, TimeUnit.SECONDS);
    }

    @Test
    @InSequence(37)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_AlarmPending_Clear_1() throws InterruptedException {
        // sent 1 clear for one node and no raise
        try {
            // Set timerIntervalToInitiateAlarmSyncMultiplier PIB parameter to speed up the test.
            final HttpGet httpget = new HttpGet(
                "http://localhost:8680/pib/configurationService"
                    + "/updateConfigParameterValue?paramName=timerIntervalToInitiateAlarmSyncMultiplier&paramValue=5000");
            AuthenticationHandler.addUserPassword(httpget);
            final HttpResponse response = this.httpclient.execute(httpget);
            assertNotNull("ClientResponse should not be null", response);
            assertEquals("Expecting status not equals", Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());

            this.testChecker.clearSyncCalls();
            this.testChecker.resetLatch(1);
            this.topicListener.resetLatch(0);

            final List<EventNotification> events = new ArrayList<EventNotification>();

            final EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "12341");
            event.setSpecificProblem("alarm_PendingClear_01");
            event.setPerceivedSeverity(SEV_CLEARED);
            events.add(event);
            LOGGER.info("Start of test_AlarmPending_Clear_1 testcase!!: CLEAR {}", events);
            this.sendJmsMessage(events);

            //wait for the alarm has been processed
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_AlarmPending_Clear_1 {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());

        //wait for the sync
        this.testChecker.await(10, TimeUnit.SECONDS);
        LOGGER.info("SyncCalls = {}", this.testChecker.getSyncCalls());
        if (!this.testChecker.getSyncCalls().contains(TEST_NODE.concat(FMFUNCTION_SUFFIX))) {
            fail("Sync not called on " + TEST_NODE);
        }

        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(38)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_AlarmPending_Clear_2() throws InterruptedException {
        // sent the same clear twice for one node and no raise
        try {
            // Set timerIntervalToInitiateAlarmSyncMultiplier PIB parameter to speed up the test.
            final HttpGet httpget = new HttpGet(
                "http://localhost:8680/pib/configurationService"
                    + "/updateConfigParameterValue?paramName=timerIntervalToInitiateAlarmSyncMultiplier&paramValue=5000");
            AuthenticationHandler.addUserPassword(httpget);
            final HttpResponse response = this.httpclient.execute(httpget);
            assertNotNull("ClientResponse should not be null", response);
            assertEquals("Expecting status not equals", Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());

            this.testChecker.clearSyncCalls();
            this.testChecker.resetLatch(1);
            this.topicListener.resetLatch(0);

            final List<EventNotification> events = new ArrayList<EventNotification>();

            EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "12342");
            event.setSpecificProblem("alarm_PendingClear_01");
            event.setPerceivedSeverity(SEV_CLEARED);
            events.add(event);
            event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "12342");
            event.setSpecificProblem("alarm_PendingClear_02");
            event.setPerceivedSeverity(SEV_CLEARED);
            events.add(event);
            LOGGER.info("Start of test_AlarmPending_Clear_2 testcase!!: CLEAR {}", events);
            this.sendJmsMessage(events);

            //wait for the alarm has been processed
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_AlarmPending_Clear_2 {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());

        //wait for the sync
        this.testChecker.await(10, TimeUnit.SECONDS);
        LOGGER.info("SyncCalls = {}", this.testChecker.getSyncCalls());
        if (!this.testChecker.getSyncCalls().contains(TEST_NODE.concat(FMFUNCTION_SUFFIX))) {
            fail("Sync not called on " + TEST_NODE);
        }

        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(39)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_AlarmPending_Clear_3() throws InterruptedException {
        // sent 1 clear for one node and then 1 raise
        try {
            // Set timerIntervalToInitiateAlarmSyncMultiplier PIB parameter to speed up the test.
            final HttpGet httpget = new HttpGet(
                "http://localhost:8680/pib/configurationService"
                    + "/updateConfigParameterValue?paramName=timerIntervalToInitiateAlarmSyncMultiplier&paramValue=5000");
            AuthenticationHandler.addUserPassword(httpget);
            final HttpResponse response = this.httpclient.execute(httpget);
            assertNotNull("ClientResponse should not be null", response);
            assertEquals("Expecting status not equals", Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());

            this.testChecker.clearSyncCalls();
            this.testChecker.resetLatch(0);
            this.topicListener.resetLatch(2);

            final List<EventNotification> events = new ArrayList<EventNotification>();

            Date beforeDate = new Date();
            Date afterDate = new Date();
            afterDate.setMinutes(afterDate.getMinutes()+1);

            EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "12343");
            event.setSpecificProblem("alarm_PendingClear_01");
            event.setPerceivedSeverity(SEV_CLEARED);
            event.setEventTime(getEventTime(afterDate));
            events.add(event);
            LOGGER.info("Start of test_AlarmPending_Clear_3 testcase!!: CLEAR {}", events);
            this.sendJmsMessage(events);
            Thread.sleep(3000L);
            events.clear();
            event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "12343");
            event.setSpecificProblem("alarm_PendingClear_01");
            event.setPerceivedSeverity(SEV_MAJOR);
            event.setEventTime(getEventTime(beforeDate));
            events.add(event);
            LOGGER.info("Start of test_AlarmPending_Clear_3 testcase!!: RAISE {}", events);
            this.sendJmsMessage(events);

            //wait for the alarm has been processed
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_AlarmPending_Clear_3 {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());

        //wait for the sync
        this.testChecker.await(10, TimeUnit.SECONDS);
        LOGGER.info("SyncCalls = {}", this.testChecker.getSyncCalls());
        if (this.testChecker.getSyncCalls().contains(TEST_NODE.concat(FMFUNCTION_SUFFIX))) {
            fail("Sync called on " + TEST_NODE);
        }

        this.topicListener.resetLatch();
    }

    @Test
    @InSequence(40)
    @OperateOnDeployment(APS_TEST_EAR)
    public void test_AlarmPending_Clear_4() throws InterruptedException {
        // sent 3 clear for one node and then 1 raise
        try {
            // Set timerIntervalToInitiateAlarmSyncMultiplier PIB parameter to speed up the test.
            final HttpGet httpget = new HttpGet(
                "http://localhost:8680/pib/configurationService"
                    + "/updateConfigParameterValue?paramName=timerIntervalToInitiateAlarmSyncMultiplier&paramValue=5000");
            AuthenticationHandler.addUserPassword(httpget);
            final HttpResponse response = this.httpclient.execute(httpget);
            assertNotNull("ClientResponse should not be null", response);
            assertEquals("Expecting status not equals", Status.OK.getStatusCode(),
                response.getStatusLine().getStatusCode());

            this.testChecker.clearSyncCalls();
            this.testChecker.resetLatch(1);
            this.topicListener.resetLatch(2);

            final List<EventNotification> events = new ArrayList<EventNotification>();

            Date beforeDate = new Date();
            Date afterDate = new Date();
            afterDate.setMinutes(afterDate.getMinutes()+1);

            EventNotification event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "123441");
            event.setSpecificProblem("alarm_PendingClear_01");
            event.setPerceivedSeverity(SEV_CLEARED);
            event.setEventTime(getEventTime(afterDate));
            events.add(event);
            event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "123442");
            event.setSpecificProblem("alarm_PendingClear_02");
            event.setPerceivedSeverity(SEV_CLEARED);
            event.setEventTime(getEventTime(afterDate));
            events.add(event);
            event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "123443");
            event.setSpecificProblem("alarm_PendingClear_03");
            event.setPerceivedSeverity(SEV_CLEARED);
            event.setEventTime(getEventTime(afterDate));
            events.add(event);
            LOGGER.info("Start of test_AlarmPending_Clear_4 testcase!!: CLEAR {}", events);
            this.sendJmsMessage(events);
            Thread.sleep(3000L);
            events.clear();
            event = this.prepareAlarm(NOTIF_TYPE_ALARM);
            event.addAdditionalAttribute(GENERATED_ALARM_ID, "123441");
            event.setSpecificProblem("alarm_PendingClear_01");
            event.setPerceivedSeverity(SEV_MAJOR);
            event.setEventTime(getEventTime(beforeDate));
            events.add(event);
            LOGGER.info("Start of test_AlarmPending_Clear_4 testcase!!: RAISE {}", events);
            this.sendJmsMessage(events);

            //wait for the alarm has been processed
            this.topicListener.LATCH.await(20, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOGGER.error("Exception in test_AlarmPending_Clear_4 {}", e);
        }
        assertEquals(0, this.topicListener.getCountInLatch());

        //wait for the sync
        this.testChecker.await(10, TimeUnit.SECONDS);
        LOGGER.info("SyncCalls = {}", this.testChecker.getSyncCalls());
        if (!this.testChecker.getSyncCalls().contains(TEST_NODE.concat(FMFUNCTION_SUFFIX))) {
            fail("Sync not called on " + TEST_NODE);
        }

        this.topicListener.resetLatch();
    }

    private Map<String, Object> getCorrelationInformation(final long generated_alarm_id) {
        final Map<String, Object> correlationInformation = new HashMap<>();

        final Long poId = this.moCreateBean.findPoId(generated_alarm_id);
        final Map<String, Object> alarmAttributes = this.moCreateBean.getAlarmAttributes(poId);

        final String additionalInformation = alarmAttributes.get(ADDITIONAL_INFORMATION).toString();
        final String[] token = additionalInformation.split(TARGET_ADDITIONAL_INFORMATION + COLON_DELIMITER);

        final String[] targetAdditionalInformationToken = token[1].split(HASH_DELIMITER);
        LOGGER.info("targetAdditionalInformationToken = {}", targetAdditionalInformationToken[0]);

        correlationInformation.put(TARGET_ADDITIONAL_INFORMATION, targetAdditionalInformationToken[0]);
        correlationInformation.put(ROOT, alarmAttributes.get(ROOT).toString());
        correlationInformation.put(CI_GROUP_1, alarmAttributes.get(CI_GROUP_1));
        correlationInformation.put(CI_GROUP_2, alarmAttributes.get(CI_GROUP_2));


        return correlationInformation;
    };

    private FMXAdaptorRequest getFmxAdaptorRequest(final Long poId, final Map<String, Object> alarmAttributes) {
        final FMXAdaptorRequest fmxAdaptorRequest = new FMXAdaptorRequest();
        fmxAdaptorRequest.setAlarmAttributes(alarmAttributes);
        fmxAdaptorRequest.setPoId(poId);
        return fmxAdaptorRequest;
    }

    private void sendJmsMessage(final List<EventNotification> event) {
        this.sender.sendEventNotificationBatch(this.serializeObject(event));
    }

    private EventNotification prepareAlarm(final String recordType) {
        final EventNotification testEvent = new EventNotification();
        testEvent.addAdditionalAttribute(GENERATED_ALARM_ID, String.valueOf(new Random().nextInt(500)));
        testEvent.addAdditionalAttribute(ADDITIONAL_TEXT, ADDITIONAL_TEXT);
        testEvent.addAdditionalAttribute(BACKUP_STATUS, "0");
        testEvent.addAdditionalAttribute(TREND_INDICATION, "0");
        testEvent.addAdditionalAttribute("backUpObject", null);
        testEvent.addAdditionalAttribute("managedObjectClass", "managedObjectClass");
        testEvent.addAdditionalAttribute(FDN, TEST_NODE);
        testEvent.setManagedObjectInstance(TEST_NODE);
        testEvent.setEventType("test");
        testEvent.setProbableCause("Test_Probable_Cause" + String.valueOf(new Random().nextInt(500)));
        testEvent.setSpecificProblem("Test_Specific_Problem" + String.valueOf(new Random().nextInt(500)));
        testEvent.setPerceivedSeverity(SEV_CRITICAL);
        testEvent.setSourceType("ERBS");
        testEvent.setEventTime(getEventTime(new Date()));
        testEvent.setTimeZone("UTC");
        testEvent.setAcknowledged(false);
        switch (recordType) {
        case NOTIF_TYPE_ALARM:
            testEvent.setRecordType(NOTIF_TYPE_ALARM);
            break;
        case NOTIF_TYPE_ERROR:
            testEvent.setRecordType(NOTIF_TYPE_ERROR);
            break;
        case NOTIF_TYPE_HB_ALARM:
            testEvent.setRecordType(NOTIF_TYPE_HB_ALARM);
            break;
        case "NON_SYNCHABLE_ALARM":
            testEvent.setRecordType(FMProcessedEventType.NON_SYNCHABLE_ALARM.name());
        case NOTIF_TYPE_CLEARALL:
            testEvent.setRecordType(NOTIF_TYPE_CLEARALL);
            testEvent.setEventType(CLEAR_ALL_EVENT_TYPE);
            testEvent.setPerceivedSeverity(SEV_INDETERMINATE);
            testEvent.setProbableCause(CLEAR_ALL_PROBABLE_CAUSE);
            testEvent.setSpecificProblem(CLEAR_ALL_SPECIFIC_PROBLEM);
            break;
        case "OUT_OF_SYNC":
            testEvent.setRecordType(FMProcessedEventType.OUT_OF_SYNC.name());
            break;
        default:
            testEvent.setRecordType(NOTIF_TYPE_ALARM);
            break;
        }
        return testEvent;
    }

    private EventNotification prepareNodeSuspendedAlarm(final boolean clearFlag) {
        final EventNotification testEvent = new EventNotification();
        testEvent.setManagedObjectInstance(TEST_NODE);
        testEvent.setRecordType("NODE_SUSPENDED");
        Date theDate = new Date();
        testEvent.setEventTime(getEventTime(theDate));
        testEvent.setTimeZone("UTC");
        testEvent.setSourceType("URAN");
        testEvent.addAdditionalAttribute(FDN, TEST_NODE);
        testEvent.setPerceivedSeverity(SEV_CRITICAL);
        if (clearFlag) {
            testEvent.setPerceivedSeverity(SEV_CLEARED);
            theDate.setMinutes(theDate.getMinutes()+1);
            testEvent.setEventTime(getEventTime(theDate));
        }

        testEvent.setSpecificProblem("Node suspended alarm");
        testEvent.setEventType("ET_COMMUNICATIONS_ALARM");
        testEvent.setProbableCause("123");
        return testEvent;
    }

    private EventNotification prepareTPOrASAAlarm(final boolean clearFlag, final String specificProblem) {
        final EventNotification testEvent = new EventNotification();
        testEvent.setManagedObjectInstance(TEST_NODE);
        testEvent.setRecordType(NOTIF_TYPE_ALARM);
        Date theDate = new Date();
        testEvent.setEventTime(getEventTime(theDate));
        testEvent.setTimeZone("UTC");
        testEvent.setSourceType("URAN");
        testEvent.addAdditionalAttribute(FDN, TEST_NODE);
        testEvent.setPerceivedSeverity(SEV_CRITICAL);
        if (clearFlag) {
            testEvent.setPerceivedSeverity(SEV_CLEARED);
            theDate.setMinutes(theDate.getMinutes()+1);
            testEvent.setEventTime(getEventTime(theDate));
        }
        testEvent.setSpecificProblem(specificProblem);
        testEvent.setEventType("ET_COMMUNICATIONS_ALARM");
        testEvent.setProbableCause("123");
        return testEvent;
    }

    private EventNotification prepareHeartBeatAlarm(final boolean clearFlag) {
        final EventNotification testEvent = new EventNotification();
        testEvent.setManagedObjectInstance(TEST_NODE);
        testEvent.setRecordType(NOTIF_TYPE_HB_ALARM);
        Date theDate = new Date();
        testEvent.setEventTime(getEventTime(theDate));
        testEvent.setSourceType("URAN");
        testEvent.setSpecificProblem("Heartbeat Failure");
        testEvent.setProbableCause("LAN Communications Failure");
        testEvent.setEventType("ET_COMMUNICATIONS_ALARM");
        testEvent.addAdditionalAttribute(FDN, TEST_NODE);
        testEvent.setPerceivedSeverity(SEV_CRITICAL);
        if (clearFlag) {
            testEvent.setPerceivedSeverity(SEV_CLEARED);
            theDate.setMinutes(theDate.getMinutes()+1);
            testEvent.setEventTime(getEventTime(theDate));
        }
        return testEvent;

    }

    private EventNotification prepareOutOfSyncAlarm(final boolean clearFlag) {
        final EventNotification testEvent = new EventNotification();
        final String fdn = TEST_NODE;
        testEvent.setManagedObjectInstance(fdn);
        testEvent.addAdditionalAttribute(GENERATED_ALARM_ID, String.valueOf(new Random().nextInt(500)));
        testEvent.setRecordType(FMProcessedEventType.OUT_OF_SYNC.name());
        Date theDate = new Date();
        testEvent.setEventTime(getEventTime(theDate));
        testEvent.setSourceType("URAN");
        testEvent.addAdditionalAttribute(FDN, fdn);
        testEvent.setPerceivedSeverity(SEV_CRITICAL);
        testEvent.setSpecificProblem("Out Of Sync");
        testEvent.setProbableCause("Loss of SNMP trap");
        testEvent.setEventType("ET_PROCESSING_ALARM");
        if (clearFlag) {
            testEvent.setPerceivedSeverity(SEV_CLEARED);
            theDate.setMinutes(theDate.getMinutes()+1);
            testEvent.setEventTime(getEventTime(theDate));
        }
        return testEvent;

    }

    private EventNotificationBatch serializeObject(final List<EventNotification> event) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final EventNotificationBatch serializableEventnotification = new EventNotificationBatch();
        ObjectOutput out = null;
        byte[] yourBytes = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(event);
            yourBytes = bos.toByteArray();
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                bos.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        serializableEventnotification.setSerializedData(yourBytes);
        return serializableEventnotification;
    }

    private String getJsonString(final FMXAdaptorRequest fmxAdaptorRequest) throws IOException, JsonGenerationException, JsonMappingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        String attributeMapAsJSONString = EMPTY_STRING;
        objectMapper.configure(Feature.WRITE_NULL_MAP_VALUES, false);

        attributeMapAsJSONString = objectMapper.writeValueAsString(fmxAdaptorRequest);
        return attributeMapAsJSONString;
    }

    public static Boolean fetchPostAction(final String request, final String uri) throws Exception {
        final HttpPost postRequest = new HttpPost(uri);
        final StringEntity parameters = new StringEntity(request, ContentType.APPLICATION_JSON);
        Boolean value = null;
        final int finalResponse = sendPostRequest(postRequest, parameters);
        if (finalResponse == 200) {
            value = true;
        }
        return value;
    }

    private static int sendPostRequest(final HttpPost postRequest, final HttpEntity httpEntity) throws IOException, ClientProtocolException {
        final HttpClient client = new DefaultHttpClient();
        postRequest.setEntity(httpEntity);
        final HttpResponse response = client.execute(postRequest);
        LOGGER.info("response is : {}", response);
        final int finalResponse = response.getStatusLine().getStatusCode();
        return finalResponse;
    }

    private static int fetchPutAction(final String uri) throws Exception {
        final HttpPost postRequest = new HttpPost(uri);
        final int response = sendPostRequest(postRequest);
        LOGGER.info("The response for http post request is:{}", response);
        return response;
    }

    @SuppressWarnings({ "resource", "deprecation" })
    private static int sendPostRequest(final HttpPost postRequest) throws IOException, ClientProtocolException {
        final HttpClient client = new DefaultHttpClient();
        final HttpResponse response = client.execute(postRequest);
        LOGGER.info("Response is : {}", response);
        final int finalResponse = response.getStatusLine().getStatusCode();
        return finalResponse;
    }

    private static int fetchPutActionNew(final String request, final String uri) throws Exception {
        final HttpPut put = new HttpPut(uri);
        final StringEntity parameters = new StringEntity(request, ContentType.APPLICATION_JSON);
        final int response = sendPutRequest(put, parameters);
        LOGGER.info("The response for http put request is:{}", response);
        return response;
    }

    @SuppressWarnings({ "resource", "deprecation" })
    private static int sendPutRequest(final HttpPut put, final HttpEntity httpEntity) throws IOException, ClientProtocolException {
        final HttpClient client = new DefaultHttpClient();
        put.setEntity(httpEntity);
        final HttpResponse response = client.execute(put);
        LOGGER.info("Response for put is : {}", response);
        final int finalResponse = response.getStatusLine().getStatusCode();
        return finalResponse;
    }

    String getEventTime(Date date) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));
        return simpleDateFormat.format(date);
    }

}

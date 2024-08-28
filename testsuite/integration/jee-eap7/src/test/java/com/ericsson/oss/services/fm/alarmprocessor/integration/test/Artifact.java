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

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.AlarmSupervisionControllerImpl;
import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.AuthenticationHandler;
import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.EventNotificationBatchSender;
import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.FMAlarmOutBusTopicListener;
import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.JmsMessageListener;
import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.MessagingTestListener;
import com.ericsson.oss.services.fm.alarmprocessor.integration.test.util.TestChecker;
import com.ericsson.oss.services.fm.common.addinfo.CorrelationType;

/**
 * Maven artifact constants
 */
public class Artifact {

    // TESTING ALARM PROCESSOR
    public static final String COM_ERICSSON_NMS_SERVICES_APS_EAR = "com.ericsson.nms.services:AlarmProcessor-ear:ear:?";
    // MODELS
    public static final String COM_ERICSSON_OSS_MEDIATION_SDK_EVENT_MODELS_JAR = "com.ericsson.oss.mediation:mediation-sdk-event-models-jar:jar:?";
    public static final String COM_ERICSSON_OSS_MEDIATION_MODEL = "com.ericsson.oss.mediation.model:mediation-service-model-jar:jar:?";
    public static final String ALARM_PERSISTENCE_MODEL_JAR = "com.ericsson.oss.services.fm.models:alarmpersistencemodel-jar:jar:?";
    public static final String FM_PROCESSED_EVENT_MODEL_JAR = "com.ericsson.oss.services.fm.models:fmprocessedeventmodel-jar:jar:?";
    public static final String FM_MEDIATION_EVENT_MODEL_JAR = "com.ericsson.oss.services.fm.models:fmmediationeventmodel-jar:jar:?";
    public static final String FM_FMX_MODEL_JAR = "com.ericsson.oss.services.fm.models:fm-fmx-model-jar:jar:?";
    // SERVICES
    public static final String COM_ERICSSON_OSS_SERVICES_ALARM_SUPERVISION_CONTROLLER_API = "com.ericsson.oss.services.fm"
            + ".services:alarmsupervisioncontroller-api:jar:?";
    public static final String COM_ERICSSON_OSS_SERVICES_RATE_DETECTION_ENGINE_EAR = "com.ericsson.oss.services.fm:rate-detection-engine-ear:ear:?";
    public static final String COM_ERICSSON_OSS_SERVICES_ALARM_FMX_ADAPTOR_EAR = "com.ericsson.oss.services.fm:fm-fmx-adaptor-ear:ear:?";
    public static final String ALARM_ACTION_SERVICES_API_JAR = "com.ericsson.nms.services:AlarmActionService-api:jar:?";
    // HTTP
    public static final String HTTP_OSGI_JAR = "org.apache.httpcomponents:httpclient-osgi:jar:?";
    public static final String HTTP_CORE_JAR = "org.apache.httpcomponents:httpcore:jar:?";
    public static final String HTTP_MIME_JAR = "org.apache.httpcomponents:httpmime:jar:?";
    public static final String REST_JAXRS = "org.jboss.resteasy:resteasy-jaxrs:jar:?";
    // JSON
    public static final String JACKSON_MAPPER_JAR = "org.codehaus.jackson:jackson-mapper-asl:jar:?";
    private static final Logger LOG = LoggerFactory.getLogger(Artifact.class);

    public static EnterpriseArchive createEnterpriseArchiveDeployment(final String artifactName) {
        final EnterpriseArchive ear = ShrinkWrap.createFromZipFile(EnterpriseArchive.class, resolveArtifactWithoutDependencies(artifactName));
        return ear;
    }

    public static File resolveArtifactWithoutDependencies(final String artifactCoordinates) {
        final File[] artifacts = Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates).withoutTransitivity().asFile();
        if (artifacts == null) {
            throw new IllegalStateException("Artifact with coordinates " + artifactCoordinates + " was not resolved");
        }
        if (artifacts.length != 1) {
            throw new IllegalStateException("Resolved more then one artifact with coordinates " + artifactCoordinates);
        }
        LOG.debug("Found file artifact {}", artifacts[0]);
        return artifacts[0];
    }

    public static File[] resolveArtifactWithDependencies(final String artifactCoordinates) {
        final File[] artifacts = Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates).withTransitivity().asFile();
        return artifacts;
    }

    public final static WebArchive createApsTestArchive() throws IOException, SAXException, ParserConfigurationException {

        final WebArchive testArchive = ShrinkWrap.create(WebArchive.class, "AlarmProcessorTestWAR.war");

        final JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "AlarmProcessorTest.jar")
                .add(new StringAsset("sdk_service_identifier=AlarmProcessorTestWAR\nsdk_service_version=1.2.3"),
                     "ServiceFrameworkConfiguration.properties");

        testArchive.addClasses(AlarmFlowIT.class,
                               JmsMessageListener.class,
                               MoCreateBean.class,
                               AuthenticationHandler.class,
                               FMAlarmOutBusTopicListener.class,
                               MessagingTestListener.class,
                               EventNotificationBatchSender.class,
                               CorrelationType.class,
                               AlarmSupervisionControllerImpl.class,
                               TestChecker.class);

        testArchive.addAsLibraries(Artifact.resolveArtifactWithoutDependencies(HTTP_MIME_JAR));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithoutDependencies(HTTP_CORE_JAR));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithoutDependencies(HTTP_OSGI_JAR));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithoutDependencies(REST_JAXRS));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithDependencies(JACKSON_MAPPER_JAR));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithoutDependencies(COM_ERICSSON_OSS_SERVICES_ALARM_SUPERVISION_CONTROLLER_API));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithDependencies(FM_PROCESSED_EVENT_MODEL_JAR));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithDependencies(FM_MEDIATION_EVENT_MODEL_JAR));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithDependencies(ALARM_PERSISTENCE_MODEL_JAR));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithDependencies(COM_ERICSSON_OSS_MEDIATION_MODEL));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithDependencies(COM_ERICSSON_OSS_MEDIATION_SDK_EVENT_MODELS_JAR));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithoutDependencies(ALARM_ACTION_SERVICES_API_JAR));
        testArchive.addAsLibraries(Artifact.resolveArtifactWithDependencies(FM_FMX_MODEL_JAR));
        testArchive.addAsLibrary(testJar);

        testArchive.addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        testArchive.setManifest(new StringAsset("Dependencies: com.ericsson.oss.itpf.datalayer.dps.api export\n"));

        return testArchive;
    }

    public static EnterpriseArchive getRateDetectionEngineEAR() {
        final File archiveFile = getEARFileFromMaven(COM_ERICSSON_OSS_SERVICES_RATE_DETECTION_ENGINE_EAR);
        final EnterpriseArchive ear = ShrinkWrap.createFromZipFile(EnterpriseArchive.class, archiveFile);
        LOG.info("EAR Contents {}", ear.toString(true));
        return ear;
    }

    private static File getEARFileFromMaven(final String artifact) {
        final File[] resolved = resolveArtifactWithDependencies(artifact);
        if (resolved.length != 1) {
            throw new RuntimeException("Can't resolve single EAR file; actual resolved file(s): " + resolved.length);
        }
        final File archiveFile = resolved[0];
        return archiveFile;
    }

    public static Archive<?> getFmxEar() {
        final File archiveFile = getEARFileFromMaven(COM_ERICSSON_OSS_SERVICES_ALARM_FMX_ADAPTOR_EAR);
        final EnterpriseArchive ear = ShrinkWrap.createFromZipFile(EnterpriseArchive.class, archiveFile);
        LOG.trace("EAR Contents {}", ear.toString(true));
        return ear;
    }
}

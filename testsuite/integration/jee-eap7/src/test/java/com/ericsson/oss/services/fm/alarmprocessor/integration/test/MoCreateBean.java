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

import static com.ericsson.oss.services.fm.alarmprocessor.integration.test.TestConstants.CPP_MED;
import static com.ericsson.oss.services.fm.alarmprocessor.integration.test.TestConstants.CPP_MED_VERSION;
import static com.ericsson.oss.services.fm.alarmprocessor.integration.test.TestConstants.FM;
import static com.ericsson.oss.services.fm.alarmprocessor.integration.test.TestConstants.MEDIATION;
import static com.ericsson.oss.services.fm.alarmprocessor.integration.test.TestConstants.MEDIATION_VERSION;
import static com.ericsson.oss.services.fm.alarmprocessor.integration.test.TestConstants.OSS_NE_DEF;
import static com.ericsson.oss.services.fm.alarmprocessor.integration.test.TestConstants.OSS_NE_DEF_VERSION;
import static com.ericsson.oss.services.fm.alarmprocessor.integration.test.TestConstants.OSS_TOP;
import static com.ericsson.oss.services.fm.alarmprocessor.integration.test.TestConstants.OSS_TOP_VERSION;
import static com.ericsson.oss.services.fm.common.constants.AlarmAttrConstants.ALARM_NUMBER;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OPEN_ALARM;
import static com.ericsson.oss.services.fm.common.constants.MetaDataConstants.OSS_FM;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.BucketProperties;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;

@Singleton
@Startup
public class MoCreateBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoCreateBean.class);

    @EServiceRef
    private DataPersistenceService service;

    Map<String, Object> aFMSupervisionMO = new HashMap<String, Object>();
    Map<String, Object> aFMFunctionMO = new HashMap<String, Object>();

    final String NE = "LTE01ERBS11";
    final String NE_IP = "1.2.3.4";

    @PostConstruct
    public void CreateDummyData() {
        createTestObjects(NE, NE_IP);
    }

    public Object getManagedObjectAttribute(final String fdn, final String attributeName) {
        LOGGER.info("getManagedObjectAttribute invoked for fdn: {} with attributeName: {}", fdn, attributeName);
        final DataBucket liveBucket = service.getLiveBucket();
        final ManagedObject managedObject = liveBucket.findMoByFdn(fdn);
        final Object value = managedObject.getAttribute(attributeName);
        LOGGER.info("Returning the attribute value : {} for fdn: {}", value.toString(), fdn);
        return managedObject.getAttribute(attributeName);
    }

    public void createTestObjects(final String nodeName, final String netsimIpAddress) {
        final DataBucket liveBucket = service.getLiveBucket();
        final ManagedObject neMO = liveBucket.findMoByFdn("NetworkElement=" + nodeName);
        if (neMO == null) {
            LOGGER.debug("Creating test objects...");

            try {
                final DataBucket dataBucket = service.getDataBucket("Live", BucketProperties.SUPPRESS_MEDIATION,
                        BucketProperties.SUPPRESS_CONSTRAINTS);

                final PersistenceObject entityAddressInfo = createEai(dataBucket);

                // NetworkElement
                final ManagedObject networkElement = createNetworkElementMo(dataBucket, nodeName);
                networkElement.setEntityAddressInfo(entityAddressInfo);
                // MeContext
                createMeContextMO(dataBucket, nodeName);
                // CppConnectivityInformation
                final ManagedObject cppConnectivityInformation = createCppConnectivityInformationMO(dataBucket, networkElement, netsimIpAddress);

                entityAddressInfo.addAssociation("ciRef", cppConnectivityInformation);
                LOGGER.debug("Created ciAssociation in entityAddressInfo.");
                Thread.sleep(100);
                createFmSupervisionChild(dataBucket, networkElement);
                createFmFunctionMoChild(dataBucket, networkElement);

            } catch (final Exception e) {
                LOGGER.error("Exception thrown when creating test objects in db!", e);
            }

            LOGGER.info("--> Created test objects.");
        }
    }

    @PreDestroy
    public void deleteDummyData() {
        LOGGER.debug("Deleting Open alarms inserted for test cases..");
        try {
            cleanDps(NE);
        } catch (final Exception e) {
            LOGGER.info("Exception while cleaning data in DPS: {}", e.toString());
        }
    }

    public void cleanDps(final String nodeName) throws Exception {
        LOGGER.debug("--> Cleaning DPS...");

        final DataBucket dataBucket = service.getDataBucket("Live", BucketProperties.SUPPRESS_MEDIATION, BucketProperties.SUPPRESS_CONSTRAINTS);

        final String meContextFdn = "MeContext=" + nodeName;
        final String neElementFdn = "NetworkElement=" + nodeName;
        final String cppConnMo = "NetworkElement=" + nodeName + ",CppConnectivityInformation=1";
        final String fmSupMo = "NetworkElement=" + nodeName + ",FmAlarmSupervision=1";
        final String fmFunMo = "NetworkElement=" + nodeName + ",FmFunction=1";
        deleteRootMo(dataBucket, fmFunMo);
        deleteRootMo(dataBucket, fmSupMo);
        deleteRootMo(dataBucket, cppConnMo);
        deleteRootMo(dataBucket, meContextFdn);
        deleteRootMo(dataBucket, neElementFdn);
        LOGGER.info("Deleted root MOs MeContext, NetworkElement and child MO/POs for node {}", nodeName);
        deleteOpenAlarmsFromDb();
        LOGGER.info("--> Cleaned DPS.");
    }

    public Long findPoId(final Long alarmNumber) {
        Long poId = null;
        final DataBucket liveBucket = service.getLiveBucket();

        final QueryBuilder queryBuilder = service.getQueryBuilder();

        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(OSS_FM, OPEN_ALARM);
        final QueryExecutor queryExecutor = liveBucket.getQueryExecutor();

        final Restriction alarmNumRestriction = typeQuery.getRestrictionBuilder().equalTo(ALARM_NUMBER, alarmNumber);

        typeQuery.setRestriction(alarmNumRestriction);

        final Iterator<PersistenceObject> correlationKeyList = queryExecutor.execute(typeQuery);
        int counter = 0;
        while (counter < 1 && correlationKeyList.hasNext()) {
            final PersistenceObject po = correlationKeyList.next();
            poId = po.getPoId();
            counter++;
        }
        return poId;
    }

    public Map<String, Object> getAlarmAttributes(final Long poId) {
        final DataBucket liveBucket = service.getLiveBucket();
        final PersistenceObject persistenceObject = liveBucket.findPoById(poId);
        final Map<String, Object> alarmAttributes = persistenceObject.getAllAttributes();
        alarmAttributes.put("eventPoId", poId);
        return alarmAttributes;
    }

    private void deleteOpenAlarmsFromDb() {
        final DataBucket liveBucket = service.getLiveBucket();
        final Query<TypeRestrictionBuilder> typeQuery = service.getQueryBuilder().createTypeQuery(FM, "OpenAlarm");
        final Iterator<PersistenceObject> iterator = liveBucket.getQueryExecutor().execute(typeQuery);
        while (iterator.hasNext()) {
            final PersistenceObject objectToDelete = iterator.next();
            LOGGER.info("Deleting open alarm with attributes: {}", objectToDelete.getAllAttributes());
            liveBucket.deletePo(objectToDelete);
        }
    }

    private PersistenceObject createEai(final DataBucket dataBucket) {
        final List<String> targetNamespaceKeys = Arrays.asList("CPP");
        LOGGER.debug("Creating EAI PO ...");
        final PersistenceObject entityAddressInfo = dataBucket.getPersistenceObjectBuilder().namespace(MEDIATION).type("EntityAddressingInformation")
                .version(MEDIATION_VERSION).addAttribute("targetNamespaceKeys", targetNamespaceKeys).create();
        LOGGER.debug("Created EAI PO ...");
        return entityAddressInfo;
    }

    private ManagedObject createMeContextMO(final DataBucket dataBucket, final String nodeName) {
        LOGGER.debug("Creating MeContext MO (root MO)...");

        final ManagedObject meContextMO = dataBucket.getMibRootBuilder().namespace(OSS_TOP).type("MeContext").version(OSS_TOP_VERSION).name(nodeName)
                .addAttribute("MeContextId", nodeName).create();
        LOGGER.info("Created MeContext MO (root MO).");

        return meContextMO;
    }

    private ManagedObject createNetworkElementMo(final DataBucket dataBucket, final String nodeName) {
        LOGGER.debug("Creating NetworkElement MO...");

        final Map<String, Object> moAttributes = new HashMap<String, Object>();
        moAttributes.put("networkElementId", nodeName);
        moAttributes.put("neType", "ERBS");
        moAttributes.put("platformType", "CPP");
        moAttributes.put("ossPrefix", "MeContext=" + nodeName);
        moAttributes.put("ossModelIdentity", "1294-439-662");

        final ManagedObject networkElement = dataBucket.getMibRootBuilder().type("NetworkElement").namespace(OSS_NE_DEF).version(OSS_NE_DEF_VERSION)
                .name(nodeName).addAttributes(moAttributes).create();

        final Map<String, Object> targetAttributes = new HashMap<String, Object>();
        targetAttributes.put("category", "NODE");
        targetAttributes.put("type", "ERBS");
        targetAttributes.put("name", nodeName);
        targetAttributes.put("modelIdentity", "1294-439-662");

        final PersistenceObject target = dataBucket.getPersistenceObjectBuilder().namespace("DPS").type("Target").version("1.0.0")
                .addAttributes(targetAttributes).create();

        networkElement.setTarget(target);

        LOGGER.info("Created NetworkElement MO with target PO : {}", target.getAllAttributes());

        return networkElement;
    }

    private ManagedObject createCppConnectivityInformationMO(final DataBucket dataBucket, final ManagedObject parentMO, final String netsimIpAddress) {
        LOGGER.debug("Creating CppConnectivityInformation MO...");

        final Map<String, Object> moAttributes = new HashMap<String, Object>();
        moAttributes.put("ipAddress", netsimIpAddress);
        moAttributes.put("CppConnectivityInformationId", "1");
        moAttributes.put("port", 80);

        final ManagedObject cppConnectivityInformation = dataBucket.getMibRootBuilder().parent(parentMO).name("1").namespace(CPP_MED)
                .version(CPP_MED_VERSION).type("CppConnectivityInformation").addAttributes(moAttributes).create();
        LOGGER.info("Created CppConnectivityInformation MO.");

        return cppConnectivityInformation;
    }

    private ManagedObject createFmSupervisionChild(final DataBucket liveBucket, final ManagedObject parentMO) {
        LOGGER.info("createSupervisionChild called: {}", parentMO.toString());
        final Map<String, Object> aFMSupervisionMO = new HashMap<String, Object>();
        aFMSupervisionMO.put("active", true);
        aFMSupervisionMO.put("automaticSynchronization", true);
        aFMSupervisionMO.put("FmAlarmSupervisionId", "1");
        aFMSupervisionMO.put("heartbeatinterval", 300);
        aFMSupervisionMO.put("heartbeatTimeout", 101);
        final ManagedObject fmSupervision = liveBucket.getMibRootBuilder().parent(parentMO).type("FmAlarmSupervision").namespace("OSS_NE_FM_DEF")
                .name("1").addAttributes(aFMSupervisionMO).version("1.1.0").create();
        LOGGER.info("Created the fmsupervision MO.");
        return fmSupervision;
    }

    private ManagedObject createFmFunctionMoChild(final DataBucket liveBucket, final ManagedObject networkElement) {
        LOGGER.info("createFmFunctionMoChild : {} ", networkElement.toString());
        final Map<String, Object> aFMFunctionMO = new HashMap<String, Object>();

        aFMFunctionMO.put("currentServiceState", FmSyncStatus100.IN_SERVICE.toString());
        final ManagedObject fmFunctionMO = liveBucket.getMibRootBuilder().parent(networkElement).type("FmFunction").namespace("OSS_NE_FM_DEF")
                .name("1").addAttributes(aFMFunctionMO).version("1.0.0").create();
        LOGGER.info("Created the fmFunction MO.");
        return fmFunctionMO;
    }

    private void deleteRootMo(final DataBucket dataBucket, final String fdn) {
        final ManagedObject moToDelete = dataBucket.findMoByFdn(fdn);
        if (moToDelete != null) {
            LOGGER.debug("Deleting '{}' MO...", moToDelete);
            dataBucket.deletePo(moToDelete);
            LOGGER.debug("Deleted '{}' MO.", moToDelete);
        }
    }
}

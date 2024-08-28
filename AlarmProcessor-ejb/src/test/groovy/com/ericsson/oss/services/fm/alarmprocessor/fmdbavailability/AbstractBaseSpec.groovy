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

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.FM_DB_AVAILABILITY_CACHE

import javax.cache.Cache
import javax.inject.Inject

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.model.ClasspathModelServiceProvider
import com.ericsson.cds.cdi.support.providers.custom.model.ModelPattern
import com.ericsson.cds.cdi.support.rule.ImplementationInstance
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
import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache
import com.ericsson.oss.itpf.sdk.cache.classic.StubMemoryCache
import com.ericsson.oss.services.fm.common.targettype.handler.DataPersistenceServiceProxy
import com.ericsson.oss.services.fm.common.targettype.handler.TargetTypeHandler

/**
 * Class is responsible initializing DPS, Model service and cache etc.
 */
class AbstractBaseSpec extends CdiSpecification {

    @Inject
    @NamedCache(FM_DB_AVAILABILITY_CACHE)
    private Cache<String, String> fmDatabaseAvailabilityCache

    @Inject
    TargetTypeHandler targetTypeHandler

    @ImplementationInstance
    private DataPersistenceServiceProxy dpsProxy = new DataPersistenceServiceProxy() {
        @Override
        public DataBucket getLiveBucket() {
            runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
            DataBucket liveBucket = runtimeDps.build().getLiveBucket()
            return liveBucket
        }
    }

    static loadedModels = [
        new ModelPattern('.*', 'ECM', 'FMFunctions', '1.0.0'),
        new ModelPattern('.*', 'BSC', 'FMFunctions', '1.0.0'),
        new ModelPattern('.*', 'global', 'FMFunctions', '1.0.0'),
    ]

    static ClasspathModelServiceProvider modelServiceProvider = new ClasspathModelServiceProvider(loadedModels)

    RuntimeConfigurableDps runtimeDps

    /**
     * Customize the injection provider
     **/
    @Override
    public Object addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        injectionProperties.addInjectionProvider(modelServiceProvider)
        injectionProperties.autoLocateFrom('com.ericsson.oss.services.fm.alarmprocessor')
        injectionProperties.autoLocateFrom('com.ericsson.oss.itpf.sdk.eventbus.classic')
    }

    def setup(){
        persistNodes()
        fmDatabaseAvailabilityCache.put("GROOVY_CPP_01","fdn")
        targetTypeHandler.fdnTargetTypeCache = new StubMemoryCache<String, String>()
    }

    public void persistNodes( ) {
        runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        DataBucket liveBucket = runtimeDps.build().getLiveBucket()
        liveBucket = runtimeDps.build().getLiveBucket()

        final ManagedObject networkElement = liveBucket.getMibRootBuilder().namespace("OSS_NE_DEF").version("2.0.0").name("APS_Groovy_003").type("NetworkElement").create()
        final Map<String, Object> targetAttributes = new HashMap<String, Object>()
        targetAttributes.put("category", "NODE")
        targetAttributes.put("type", "ERBS")
        targetAttributes.put("name", "NetworkElement=APS_Groovy_003")
        targetAttributes.put("modelIdentity", "1294-439-662");
        final PersistenceObject targetPo = runtimeDps.addPersistenceObject().namespace("DPS").type("Target").addAttributes(targetAttributes).build();
        networkElement.setTarget(targetPo)
        final ManagedObject managedElement = liveBucket.getMibRootBuilder().namespace("OSS_NE_FM_DEF").type("FmFunction").parent(networkElement).addAttribute("currentServiceState", "IN_SERVICE").create()
        final Map<String, Object> supervisionAttributes = new HashMap<String, Object>()
        supervisionAttributes.put("active", true)
        supervisionAttributes.put("automaticSynchronization", true)
        final ManagedObject managedElement1 = liveBucket.getMibRootBuilder().namespace("OSS_NE_FM_DEF").name("1").type("FmAlarmSupervision").parent(networkElement).addAttributes(supervisionAttributes).create()

        final ManagedObject ecmNode = liveBucket.getMibRootBuilder().namespace("OSS_NE_DEF").version("2.0.0").name("ECM").type("VirtualNetworkFunctionManager").create()
        final Map<String, Object> targetAttributesECM = new HashMap<String, Object>()
        targetAttributesECM.put("category", "VNFM")
        targetAttributesECM.put("type", "ECM")
        targetAttributesECM.put("name", "VirtualNetworkFunctionManager=ECM01")
        targetAttributesECM.put("modelIdentity", "1294-439-662");
        final PersistenceObject targetPoECM = runtimeDps.addPersistenceObject().namespace("DPS").type("Target").addAttributes(targetAttributesECM).build();
        ecmNode.setTarget(targetPoECM)

        final ManagedObject radioNode = liveBucket.getMibRootBuilder().namespace("OSS_NE_DEF").version("2.0.0").name("RadioNode").type("NetworkElement").create()
        final Map<String, Object> targetAttributesRadioNode = new HashMap<String, Object>()
        targetAttributesRadioNode.put("category", "NODE")
        targetAttributesRadioNode.put("type", "RadioNode")
        targetAttributesRadioNode.put("name", "NetworkElement=RadioNode")
        targetAttributesRadioNode.put("modelIdentity", "1294-439-662");
        final PersistenceObject targetPoRadioNode = runtimeDps.addPersistenceObject().namespace("DPS").type("Target").addAttributes(targetAttributesRadioNode).build();
        radioNode.setTarget(targetPoRadioNode)

        ManagedObject radioBSC = liveBucket.getMibRootBuilder().namespace("OSS_NE_DEF").version("2.0.0").name("BSC").type("NetworkElement").create()
        final Map<String, Object> targetAttributesBSC = new HashMap<String, Object>()
        targetAttributesBSC.put("category", "NODE")
        targetAttributesBSC.put("type", "BSC")
        targetAttributesBSC.put("name", "NetworkElement=BSC")
        targetAttributesBSC.put("modelIdentity", "1294-439-662");
        final PersistenceObject targetPoBSC = runtimeDps.addPersistenceObject().namespace("DPS").type("Target").addAttributes(targetAttributesBSC).build();
        radioBSC.setTarget(targetPoBSC)
    }

    public Map<String, Object> readAlarm(final String sp, final String pc, final String et, final String oor) {
        final String reg_expresion = "[^a-zA-Z0-9@#%~_+\\-=\\[\\]{}*\$^\\s,.:;!&()<>!`]";

        runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        DataBucket liveBucket = runtimeDps.build().getLiveBucket()
        final QueryBuilder queryBuilder = runtimeDps.build().getQueryBuilder();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery("FM", "OpenAlarm");
        final RestrictionBuilder restrictionBuilder = typeQuery.getRestrictionBuilder();
        final Restriction restrictionSP = restrictionBuilder.equalTo("specificProblem", sp);
        final Restriction restrictionPC = restrictionBuilder.equalTo("probableCause", pc);
        final Restriction restrictionET = restrictionBuilder.equalTo("eventType", et);
        final Restriction restrictionOOR = restrictionBuilder.equalTo("objectOfReference", oor);
        final Restriction finalRestriction = restrictionBuilder.allOf(restrictionSP,restrictionPC, restrictionET, restrictionOOR,)
        typeQuery.setRestriction(finalRestriction);
        final Iterator<StubbedPersistenceObject> correlatedAlarmIterator = liveBucket.getQueryExecutor().execute(typeQuery);
        Map<String, Object> poAttributes = new HashMap<String, Object>();
        if (correlatedAlarmIterator != null) {
            while (correlatedAlarmIterator.hasNext()) {
                final StubbedPersistenceObject po = correlatedAlarmIterator.next();
                //Acquire lock
                poAttributes = po.getAllAttributes();
                poAttributes.put("eventPoId", po.getPoId())
                final String alarmState = poAttributes.get("alarmState")
                final String recordType = poAttributes.get("recordType")
            }
        }
        return poAttributes;
    }
}


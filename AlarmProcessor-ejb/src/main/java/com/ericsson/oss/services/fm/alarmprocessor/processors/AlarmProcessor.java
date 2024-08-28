/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.fm.alarmprocessor.processors;

import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmAttributesPopulator.populateNewAlarm;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.FMX_CREATED;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.fm.alarmprocessor.api.custom.attributes.CustomAttributesHandler;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.instrumentation.APSInstrumentedBean;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessingResponse;
import com.ericsson.oss.services.fm.alarmprocessor.util.ClearAlarmsCacheManager;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;
import com.ericsson.oss.services.fm.alarmprocessor.util.AlarmsCountOnNodesMapManager;

/**
 * Class processes new alarm, i.e when there is no correlated alarm in DB.
 */
public class AlarmProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmProcessor.class);

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private ClearAlarmsCacheManager clearAlarmsCacheManager;

    @EServiceRef
    private CustomAttributesHandler customAttributesHandler;

    @Inject
    private APSInstrumentedBean apsInstrumentedBean;

    @Inject
    AlarmsCountOnNodesMapManager alarmsCountOnNodesMapManager;

    /**
     * Method inserts alarm in Database. If the severity of alarm is clear, it updates in clearAlarm cache.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @return {@link AlarmProcessingResponse}
     */
    public AlarmProcessingResponse processAlarm(final ProcessedAlarmEvent alarmRecord) {
        final AlarmProcessingResponse alarmProcessingResponse = new AlarmProcessingResponse();
        if (ProcessedEventSeverity.CLEARED.equals(alarmRecord.getPresentSeverity())) {
            clearAlarmsCacheManager.addClearAlarm(alarmRecord);
        } else {
            final Map<String, Object> alarmAttributes = populateNewAlarm(alarmRecord);
            final Long eventPoId = openAlarmService.insertAlarmRecord(alarmAttributes);
            alarmRecord.setEventPOId(eventPoId);
            alarmRecord.setActionState(ProcessedLastAlarmOperation.NEW);
            apsInstrumentedBean.incrementAlarmRootCounters(alarmAttributes);

            LOGGER.debug("Inserted Alarm in Database: {}", alarmRecord);

            final Map<String, String> additionalInformationOfAlarms = alarmRecord.getAdditionalInformation();
            if((alarmRecord.getFdn() != null) && (additionalInformationOfAlarms.get("sourceType") != null)) {
                final String nodeName = (alarmRecord.getFdn()).split("=")[1];
                final String nodeType = additionalInformationOfAlarms.get("sourceType");
                alarmsCountOnNodesMapManager.incrementAlarmsCountRequest(nodeName, nodeType);
            }
            if (FMX_CREATED.equals(alarmRecord.getFmxGenerated())) {
                final Map<String, String> newAdditionalAttributes = alarmRecord.getAdditionalInformation();
                LOGGER.debug("FMX CREATED alarm is inserted in DB with PoId {}. Processing the custom attributes for this alarm {}", eventPoId, newAdditionalAttributes);
                customAttributesHandler.updateCustomAttributes(newAdditionalAttributes, eventPoId, new HashMap<String, String>());
            }

            alarmProcessingResponse.getProcessedAlarms().add(alarmRecord);
            clearAlarmsCacheManager.checkAndProcessForClearAlarm(alarmRecord, alarmProcessingResponse);
        }
        apsInstrumentedBean.incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
        return alarmProcessingResponse;
    }

    /**
     * Method inserts alarms with clear_all record type in Database.
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     */
    public void processClearAllAlarm(final ProcessedAlarmEvent alarmRecord) {
        alarmRecord.setActionState(ProcessedLastAlarmOperation.NEW);
        final Map<String, Object> alarmAttributes = populateNewAlarm(alarmRecord);
        alarmRecord.setEventPOId(openAlarmService.insertAlarmRecord(alarmAttributes));
        LOGGER.debug("Inserted Alarm in Database with Clear_All record type for node {}", alarmRecord.getFdn());
        apsInstrumentedBean.incrementNewlyProcessedAlarmCount(alarmRecord.getPresentSeverity());
    }
}

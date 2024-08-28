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

package com.ericsson.oss.services.fm.alarmprocessor.util;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ALARMSUPPRESSED_SP;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.CLEAR_ALARMS_CACHE;
import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.TECHNICIANPRESENT_SP;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.getKeyFromAlarm;
import static com.ericsson.oss.services.fm.common.constants.FmxConstants.NOT_SET;
import static com.ericsson.oss.services.fm.common.constants.ManagedObjectConstants.INTERNAL_ALARM_FDN;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.ALARM_SUPPRESSED_ALARM;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.HEARTBEAT_ALARM;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.NODE_SUSPENDED;
import static com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType.TECHNICIAN_PRESENT;
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState.ACTIVE_ACKNOWLEDGED;
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState.ACTIVE_UNACKNOWLEDGED;
import static com.ericsson.oss.services.fm.models.processedevent.ProcessedEventState.CLEARED_ACKNOWLEDGED;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.fm.alarmprocessor.orphanclear.ClearAlarmExpirable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cache.annotation.NamedCache;
import com.ericsson.oss.services.fm.alarmprocessor.alarm.staging.AlarmStagingHandler;
import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.FmFunctionMoService;
import com.ericsson.oss.services.fm.alarmprocessor.dps.util.OpenAlarmService;
import com.ericsson.oss.services.fm.alarmprocessor.processors.ClearAlarmProcessor;
import com.ericsson.oss.services.fm.common.builder.ProcessedAlarmEventBuilder;
import com.ericsson.oss.services.fm.models.processedevent.FMProcessedEventType;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedLastAlarmOperation;
import com.ericsson.oss.services.models.alarm.cache.ClearAlarmsListWrapper;

/**
 * Class manages actions related to cache. Cache holding the entries of alarms with severity CLEARED and its corresponding original alarm is not found
 * in database.
 */
@ApplicationScoped
public class ClearAlarmsCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClearAlarmsCacheManager.class);

    @Inject
    @NamedCache(CLEAR_ALARMS_CACHE)
    private Cache<String, ClearAlarmsListWrapper> clearAlarmsCache;

    @Inject
    private AlarmCorrelator alarmCorrelator;

    @Inject
    private OpenAlarmService openAlarmService;

    @Inject
    private AlarmAttributesPopulator alarmAttributesPopulator;

    @Inject
    private ClearAlarmProcessor clearAlarmProcessor;

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private AlarmStagingHandler alarmStagingHandler;

    @Inject
    private CurrentServiceStateUpdator currentServiceStateUpdator;

    @Inject
    private FmFunctionMoService fmFunctionMoService;

    /**
     * Add clear alarm which has no correlated original alarm to a Cache to be processed later.
     *
     * @param alarmRecord
     *            The processed alarm event to be added to cache.
     */
    public void addClearAlarm(final ProcessedAlarmEvent alarmRecord) {
        // Ignore Internal Alarms for alarm synchronization
        if (!INTERNAL_ALARM_FDN.equals(alarmRecord.getFdn())) {
            final String key = getKeyFromAlarm(alarmRecord);
            ClearAlarmsListWrapper clearAlarmWrapper = this.clearAlarmsCache.get(key);
            if (clearAlarmWrapper == null) {
                clearAlarmWrapper = new ClearAlarmsListWrapper(alarmRecord.getFdn());
            }
            clearAlarmWrapper.getAlarmsList().add(alarmRecord);
            this.clearAlarmsCache.put(key, clearAlarmWrapper);
            LOGGER.info("Clear Alarm Record added to Cache with key : {} and value : {}", key, alarmRecord.toString());
        }
    }

    /**
     * Method which checks if clear event exists in cache and processes it when present.
     *
     * @param alarmRecord
     *            {@link ProcessedAlarmEvent}
     * @param alarmProcessingResponse
     *            response object containing the processed alarm.
     * @return {@link AlarmProcessingResponse}
     */
    public void checkAndProcessForClearAlarm(final ProcessedAlarmEvent alarmRecord, final AlarmProcessingResponse alarmProcessingResponse) {
        final String key = getKeyFromAlarm(alarmRecord);
        final ClearAlarmsListWrapper clearAlarmListWrapper = this.clearAlarmsCache.get(key);
        if (clearAlarmListWrapper != null && !clearAlarmListWrapper.getAlarmsList().isEmpty()) {
            final ProcessedAlarmEvent clearEvent = this.processAlarm(clearAlarmListWrapper, key, alarmRecord);
            if (null != clearEvent && clearEvent.getCorrelatedPOId() > 0) {
                clearEvent.setEventPOId(clearEvent.getCorrelatedPOId());
                // To notify Clear alarm
                alarmProcessingResponse.getProcessedAlarms().add(clearEvent);
            }
        }
    }

    /**
     * Returns set of NetworkElement FDNs from cache.
     *
     * @return the fdn set from cache.
     */
    public Set<String> getFdnsFromCache() {
        final Set<String> networkElementFdns = new HashSet<>();
        try {
            final Iterator<Entry<String, ClearAlarmsListWrapper>> iterator = this.clearAlarmsCache.iterator();
            while (iterator.hasNext()) {
                final Entry<String, ClearAlarmsListWrapper> entry = iterator.next();
                if (entry != null) {
                    final ClearAlarmsListWrapper clearAlarmListWrapper = entry.getValue();
                    if (!clearAlarmListWrapper.getAlarmsList().isEmpty()) {
                        // FDN for any element of the list will be sufficient.
                        final String fdn = entry.getValue().getAlarmsList().get(0).getFdn();
                        // Ignore Internal Alarms for alarm synchronization
                        if (fdn != null && !fdn.equals(INTERNAL_ALARM_FDN)) {
                            networkElementFdns.add(fdn);
                        }
                    }
                }
            }
        } catch (final Exception exception) {
            LOGGER.error("Exception in getFdnsFromCache {}", exception.getMessage());
            LOGGER.debug("Exception in getFdnsFromCache : ", exception);
        }
        return networkElementFdns;
    }

    /**
     * Remove all entries related to a list of Fdns.
     *
     * @param networkElementFdns the fdn set.
     */
    public void removeFdnFromCache(final Set<String> networkElementFdns) {
        final Iterator<Entry<String, ClearAlarmsListWrapper>> iterator = this.clearAlarmsCache.iterator();
        final Stream<Entry<String, ClearAlarmsListWrapper>> cacheStrem = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL), false);
        final Set<String> toRemove = new HashSet<>();
        networkElementFdns.forEach(
                fdn -> cacheStrem.filter(entry -> entry.getValue().getFdn().equals(fdn)).map(Entry::getKey).forEach(toRemove::add));
        this.clearAlarmsCache.removeAll(toRemove);
    }

    /**
     * Method performs alarm correlation on clear alarms present in ClearAlarmsCache.
     * <p>
     * If there is a corresponding clear alarm found in cache for the received original alarm then the clear alarm is updated with few attributes from
     * the received alarm ,removed from Cache and returned .
     * <p>
     * If there is no corresponding clear alarm for the received originalAlarm then null is returned.
     *
     * @param clearAlarmListWrapper
     *            {link ClearAlarmsListWrapper}
     * @param key
     *            a {@link String} containing alarmAttributes separated by @@@ delimiter.
     * @param originalAlarmRecord
     *            {@link ProcessedAlarmEvent}
     * @return {@link ProcessedAlarmEvent} clearAlarm
     */
    private ProcessedAlarmEvent processAlarm(final ClearAlarmsListWrapper clearAlarmListWrapper, final String key,
                                             final ProcessedAlarmEvent originalAlarmRecord) {
        ProcessedAlarmEvent clearAlarm = null;
        boolean correlatedFlag = false;
        LOGGER.debug("Alarm received to ClearAlarmsCacheManager: {}", originalAlarmRecord.toString());
        final List<ProcessedAlarmEvent> clearAlarms = clearAlarmListWrapper.getAlarmsList();
        LOGGER.debug("Clear alarms present in the Cache for oor: {} are: {}", key, clearAlarms);
        final Iterator<ProcessedAlarmEvent> iterator = clearAlarms.iterator();
        while (iterator.hasNext()) {
            clearAlarm = iterator.next();
            if (this.alarmCorrelator.correlateAlarm(clearAlarm, originalAlarmRecord)) {
                correlatedFlag = true;
                break;
            }
        }
        if (correlatedFlag) {
            if (this.configParametersListener.getTransientAlarmStaging() && NOT_SET.equals(originalAlarmRecord.getFmxGenerated())) {
                LOGGER.debug("Staging PIB parameter is enabled! Check and Stage the alarm if needed");
                if (this.alarmStagingHandler.checkAndStageAlarm(clearAlarm, originalAlarmRecord)) {
                    // Transient alarm is staged!
                    // Will return here and the alarm will be re-processed once the stage timer expires.
                    return null;
                }
            } else {
                this.updateCorrelatedClearAlarm(clearAlarm, originalAlarmRecord);
                clearAlarms.remove(clearAlarm);
                clearAlarmListWrapper.setAlarmsList(clearAlarms);
                this.clearAlarmsCache.put(key, clearAlarmListWrapper);
                LOGGER.debug("ClearAlarm's OOR is : {} , AlarmNumber is: {} , CorrelatedPOId: {} , PresentSeverity: {} , AlarmState: {} ",
                        clearAlarm.getObjectOfReference(), clearAlarm.getAlarmNumber(), clearAlarm.getCorrelatedPOId(),
                        clearAlarm.getPresentSeverity(), clearAlarm.getAlarmState());

                if (clearAlarm.getPresentSeverity().equals(ProcessedEventSeverity.CLEARED)) {
                    clearAlarm.setLastAlarmOperation(ProcessedLastAlarmOperation.CLEAR);
                    clearAlarm = this.updateClearAlarm(clearAlarm, originalAlarmRecord);
                }
                this.updateCurrentServiceStateAfterProcessingClearFromCache(clearAlarm, clearAlarm.getRecordType());
            }
            return clearAlarm;
        }
        return null;
    }

    private void updateCurrentServiceStateAfterProcessingClearFromCache(final ProcessedAlarmEvent clearAlarm, final FMProcessedEventType recordType) {
        LOGGER.debug("Processed Clear alarm with recordType {} from ClearALarmsCache. Setting currentServiceState back to IN_SERVICE.", recordType);
        if (HEARTBEAT_ALARM.equals(recordType)) {
            this.currentServiceStateUpdator.updateForHeartBeatAlarm(clearAlarm);
        } else if (NODE_SUSPENDED.equals(recordType)) {
            this.currentServiceStateUpdator.updateForNodeSuspendedAlarm(clearAlarm);
        } else if (ALARM_SUPPRESSED_ALARM.equals(clearAlarm.getRecordType())
                || ALARMSUPPRESSED_SP.equals(clearAlarm.getSpecificProblem())) {
            this.fmFunctionMoService.update(clearAlarm.getFdn(), AlarmProcessorConstants.ALARM_SUPPRESSED_STATE, false);
        } else if (TECHNICIAN_PRESENT.equals(clearAlarm.getRecordType())
                || TECHNICIANPRESENT_SP.equals(clearAlarm.getSpecificProblem())) {
            this.fmFunctionMoService.update(clearAlarm.getFdn(), AlarmProcessorConstants.TECHNICIAN_PRESENT_STATE, false);
        }
    }

    /**
     * Method updates correlated clear alarm found in ClearAlarmsCache with the attributes of received original Alarm.
     * <p>
     * Alarm Attributes like PreviousSeverity,CeaseTime,CeaseOperator,RepeatCount,OscillationCount, EventPoId are retrieved from original alarm and
     * set to clear alarm.
     *
     * @param clearAlarm
     *            {@link ProcessedAlarmEvent}
     * @param originalAlarm
     *            {@link ProcessedAlarmEvent}
     */
    private void updateCorrelatedClearAlarm(final ProcessedAlarmEvent clearAlarm, final ProcessedAlarmEvent originalAlarm) {
        clearAlarm.setPreviousSeverity(originalAlarm.getPresentSeverity());
        clearAlarm.setCeaseTime(clearAlarm.getEventTime());
        clearAlarm.setCeaseOperator(originalAlarm.getCeaseOperator());
        clearAlarm.setCorrelatedPOId(originalAlarm.getEventPOId());
        // Set EventPOId with CorrelatedEventPoId=EventPOId of Original Alarm
        clearAlarm.setEventPOId(originalAlarm.getCorrelatedPOId());
        // Retrieve repeatCount,oscillationCount from CorrelatedAlarm
        clearAlarm.setRepeatCount(originalAlarm.getRepeatCount());
        clearAlarm.setOscillationCount(originalAlarm.getOscillationCount());
        clearAlarm.setVisibility(originalAlarm.getVisibility());
        clearAlarm.setProcessingType(originalAlarm.getProcessingType());
        clearAlarm.setFmxGenerated(originalAlarm.getFmxGenerated());
    }

    /**
     * Method updates an already existing Alarm PersistenceObject when a clear is received.
     *
     * @param clearAlarm
     *            {@link ProcessedAlarmEvent}
     * @param originalAlarmRecord
     *            {@link ProcessedAlarmEvent}
     */
    private ProcessedAlarmEvent updateClearAlarm(final ProcessedAlarmEvent clearAlarm, final ProcessedAlarmEvent originalAlarmRecord) {
        ProcessedAlarmEvent updatedClearAlarm = clearAlarm;
        if (ACTIVE_ACKNOWLEDGED.equals(originalAlarmRecord.getAlarmState())) {
            updatedClearAlarm.setAlarmState(CLEARED_ACKNOWLEDGED);
            updatedClearAlarm.setAckOperator(originalAlarmRecord.getAckOperator());
            updatedClearAlarm.setAckTime(originalAlarmRecord.getAckTime());
            updatedClearAlarm.setCorrelatedPOId(originalAlarmRecord.getEventPOId());
            final Map<String, Object> alarmAttributes = this.alarmAttributesPopulator.populateDeleteAlarm(updatedClearAlarm, originalAlarmRecord);
            this.alarmAttributesPopulator.updateLastDeliveredTime(updatedClearAlarm, originalAlarmRecord, alarmAttributes);
            this.openAlarmService.removeAlarm(updatedClearAlarm.getCorrelatedPOId(), alarmAttributes);
            LOGGER.debug("Alarm with this PO Id: {} removed from the list ", updatedClearAlarm.getCorrelatedPOId());
        } else if (ACTIVE_UNACKNOWLEDGED.equals(originalAlarmRecord.getAlarmState())) {
            final Map<String, Object> clearAlarmAttributes = this.alarmAttributesPopulator.populateClearAlarm(updatedClearAlarm);
            updatedClearAlarm = ProcessedAlarmEventBuilder.buildProcessedAlarm(clearAlarmAttributes);
            updatedClearAlarm.setEventPOId(updatedClearAlarm.getCorrelatedPOId());
            this.alarmAttributesPopulator.updateLastDeliveredTime(updatedClearAlarm, originalAlarmRecord, clearAlarmAttributes);
            this.openAlarmService.updateAlarm(updatedClearAlarm.getCorrelatedPOId(), clearAlarmAttributes);
            LOGGER.debug("Alarm with this PO Id: {} received Clear ", updatedClearAlarm.getCorrelatedPOId());
            // check if this alarm has the visibility false and delete it from DB if yes as it is a Clear on Hidden alarm.
            if (!originalAlarmRecord.getVisibility()) {
                this.clearAlarmProcessor.updateAndDeleteHiddenClearAlarm(updatedClearAlarm, originalAlarmRecord);
            }
        }
        return updatedClearAlarm;
    }

    /**
     * The Remove Clear Alarm method.
     * @param clearAlarm the clear alarm event.
     * @return true on success, false on failure.
     */
    public boolean removeClearAlarm(final ClearAlarmExpirable clearAlarm) {

        final ProcessedAlarmEvent clearAlarmEvent = clearAlarm.getAlarm();
        final ProcessedAlarmEvent correlatedAlarm = clearAlarm.getCorrelatedAlarm();
        final String key = getKeyFromAlarm(clearAlarmEvent);
        final ClearAlarmsListWrapper clearAlarmListWrapper = this.clearAlarmsCache.get(key);
        final List<ProcessedAlarmEvent> clearAlarms = clearAlarmListWrapper.getAlarmsList();

        if (this.configParametersListener.getTransientAlarmStaging() && NOT_SET.equals(correlatedAlarm.getFmxGenerated())) {
            LOGGER.debug("Staging PIB parameter is enabled! Check and Stage the alarm if needed");
            if (this.alarmStagingHandler.checkAndStageAlarm(clearAlarmEvent, correlatedAlarm)) {
                // Transient alarm is staged!
                // Will return here and the alarm will be re-processed once the stage timer expires.
                return false;
            }
        } else {
            this.updateCorrelatedClearAlarm(clearAlarmEvent, correlatedAlarm);
            if (!clearAlarms.removeIf(alarm -> (alarm.getEventTime() != null && alarm.getEventTime().equals(clearAlarm.getAlarm().getEventTime())))) {
                LOGGER.error("Cannot remove element from cache! ProcessedAlarmEvent: {}", clearAlarmEvent.toString());
                // This should never happens! In case we fall here, a new sync will be triggered.
            }
            clearAlarmListWrapper.setAlarmsList(clearAlarms);
            this.clearAlarmsCache.put(key, clearAlarmListWrapper);
        }

        return true;
    }
}

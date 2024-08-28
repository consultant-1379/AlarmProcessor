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

package com.ericsson.oss.services.fm.alarmprocessor.orphanclear;

import static com.ericsson.oss.services.fm.alarmprocessor.constants.AlarmProcessorConstants.ORPHANCLEAR_TAKE_EXPIRED_TIMEOUT_MSEC;
import static com.ericsson.oss.services.fm.alarmprocessor.util.AlarmProcessorUtility.getKeyFromAlarm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.fm.alarmprocessor.configuration.ConfigParametersListener;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedAlarmEvent;
import com.ericsson.oss.services.fm.models.processedevent.ProcessedEventSeverity;

/**
 * Class to mange the pending clear alarms using delayed queue:
 * the sync operation by FDN has to be done if no correlated raise is received for the pending clear (till the timeout).
 * Every single received clear alarm has to be manged with its timeout instead of using a global polling method as in
 * the first version.
 */
@ApplicationScoped
public class ExpireClearAlarmManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpireClearAlarmManager.class);

    @Inject
    private ConfigParametersListener configParametersListener;

    @Inject
    private CorrelatedExpiredClearAlarmHandler correlatedExpiredClearAlarmHandler;

    // The clear alarms collection (the key is the same used on distributed cache).
    private final Map<String, List<ClearAlarmExpirable>> clearAlarmMap = new HashMap<>();
    // The delayed queue.
    private final DelayQueue<ClearAlarmExpirable> clearAlarmDelayQueue = new DelayQueue<>();

    /**
     * Add the received clear alarm as expirable item on the delayed queue and utility map.
     *
     * @param clearAlarmEvent
     *         the event to be added
     */
    public void add(final ProcessedAlarmEvent clearAlarmEvent) {
        int timerDuration = this.configParametersListener.getTimerIntervalToInitiateAlarmSync();
        timerDuration = timerDuration * this.configParametersListener.getTimerIntervalToInitiateAlarmSyncMultiplier();
        final ClearAlarmExpirable clearAlarm = new ClearAlarmExpirable(clearAlarmEvent, System.currentTimeMillis() + timerDuration);
        synchronized (this.clearAlarmMap) {
            // Add the clear alarm to the delayed queue and to the utility maps.
            if (this.clearAlarmDelayQueue.add(clearAlarm)) {
                // Add the clear alarm to the clear alarm list.
                this.addClearAlarmToMap(this.clearAlarmMap, getKeyFromAlarm(clearAlarmEvent), clearAlarm);
            }
        }
        LOGGER.debug("add {} to delayed queue", clearAlarmEvent);
    }

    /**
     * Removes the clear alarm when the correlated raise received.
     *
     * @param alarm
     *         the clear alarm to remove
     * @return the return value: true if an element was removed from expirable
     */
    public boolean remove(final ProcessedAlarmEvent alarm) {
        synchronized (this.clearAlarmMap) {
            // Remove the clear alarm from the clear alarm list.
            final ClearAlarmExpirable clearAlarm = this.removeClearAlarmFromMap(this.clearAlarmMap, getKeyFromAlarm(alarm));
            if (clearAlarm == null) {
                // If we're here, the current expirable clear alarm has been already expired and processed by APS.
                // Nothing to do here...
                return false;
            }
            // Remove the clear alarm from the delayed queue.
            LOGGER.debug("ExpirableClearAlarmQueue removing expirable {}", clearAlarm);
            return this.clearAlarmDelayQueue.remove(clearAlarm);
        }
    }

    /**
     * Update the two storage based on the received list.
     *
     * @param alarms
     *         the new updated list.
     * @return true in case of success.
     */
    public boolean update(final String key, final List<ProcessedAlarmEvent> alarms) {
        synchronized (this.clearAlarmMap) {
            // Retrieve the new and the old clear alarm lists.
            final List<ClearAlarmExpirable> computedList = this.clearAlarmMap.computeIfAbsent(key, k -> new ArrayList<>());
            final Iterator<ClearAlarmExpirable> oldClearListIt = computedList.iterator();
            final Iterator<ProcessedAlarmEvent> newClearListIt = alarms.iterator();
            // Iterate the old list:
            while (oldClearListIt.hasNext()) {
                final ClearAlarmExpirable oldElement = oldClearListIt.next();
                // If the new list has the element, update old value, otherwise remove it from old list.
                if (newClearListIt.hasNext()) {
                    oldElement.setProcessedAlarm(newClearListIt.next());
                } else {
                    oldClearListIt.remove();
                    if (!this.clearAlarmDelayQueue.remove(oldElement)) {
                        LOGGER.debug("Cannot remove {} from clearAlarmDelayQueue", oldElement);
                    }
                }
            }
            // If the new list has new elements, add them on both the old list and delayed queue.
            while (newClearListIt.hasNext()) {
                this.add(newClearListIt.next());
            }
            final List<ClearAlarmExpirable> newList = this.clearAlarmMap.get(key);
            if (newList.isEmpty()) {
                this.clearAlarmMap.remove(key);
            }
        }
        return true;
    }

    /**
     * Manage expiration element.
     *
     * @return the expired clear alarm object.
     * @throws InterruptedException
     *         the possible outcome of calling the method.
     */
    ClearAlarmExpirable takeExpired() throws InterruptedException {
        final ClearAlarmExpirable clearAlarm = this.clearAlarmDelayQueue.poll(ORPHANCLEAR_TAKE_EXPIRED_TIMEOUT_MSEC, TimeUnit.MILLISECONDS);
        if (clearAlarm != null) {
            // Check if the correlated alarm is already present on the DB: if yes process the clear and prevent sync.
            // That may happens if raise and clear comes in a very short time (processed by different instances) or delay
            // on distributed cache alignment.
            // In that case both the raise and clear event can't detect the presence of each other so the raise will be
            // added on the DB and the clear will be put on the delayed queue as orphanage.
            final ProcessedAlarmEvent alarmRecord = clearAlarm.getAlarm();
            final ProcessedAlarmEvent correlatedAlarm = this.correlatedExpiredClearAlarmHandler.getCorrelatedAlarm(alarmRecord);
            if (correlatedAlarm != null && correlatedAlarm.getEventPOId() > 0 && !correlatedAlarm.getPresentSeverity()
                    .equals(ProcessedEventSeverity.CLEARED)) {
                // Remove just the expired clear and set the correlated flag to prevent syncronization from the caller.
                this.remove(alarmRecord);
                clearAlarm.setCorrelatedAlarm(correlatedAlarm);
                return clearAlarm;
            }
            synchronized (this.clearAlarmMap) {
                // Retrieve all the clear alarms for the given FDN.
                final List<ClearAlarmExpirable> removeList = this.clearAlarmMap.values().stream().flatMap(Collection::stream)
                        .filter(each -> each.getAlarm().getFdn().equals(clearAlarm.getAlarm().getFdn())).collect(Collectors.toList());
                // Cleanup all the elements.
                removeList.forEach(each -> {
                    // Remove each clear alarms from the delayed queue.
                    if (this.clearAlarmDelayQueue.remove(each)) {
                        // Remove each clear alarms from the clear alarm map.
                        this.removeClearAlarmFromMap(this.clearAlarmMap, getKeyFromAlarm(each.getAlarm()));
                    }
                });
            }
        }
        return clearAlarm;
    }

    /**
     * Add the given clear alarm object to the given map.
     *
     * @param theMap
     *         where the element has to be added.
     * @param key
     *         the key of the element to be added.
     * @param clearAlarm
     *         the clear alarm object element.
     */
    private void addClearAlarmToMap(final Map<String, List<ClearAlarmExpirable>> theMap, final String key, final ClearAlarmExpirable clearAlarm) {
        List<ClearAlarmExpirable> clearAlarmList = theMap.get(key);
        if (clearAlarmList == null) {
            clearAlarmList = new ArrayList<>();
        }
        clearAlarmList.add(clearAlarm);
        theMap.put(key, clearAlarmList);
    }

    /**
     * Remove the given clear alarm object from the given map.
     *
     * @param theMap
     *         from where the element has to be removed.
     * @param key
     *         the key of the element to be removed.
     * @return null if no element has been removed or the removed element.
     */
    private ClearAlarmExpirable removeClearAlarmFromMap(final Map<String, List<ClearAlarmExpirable>> theMap, final String key) {
        final List<ClearAlarmExpirable> clearAlarmList = theMap.get(key);
        if (clearAlarmList == null) {
            return null;
        }
        final ClearAlarmExpirable ret = clearAlarmList.remove(0);
        if (clearAlarmList.isEmpty()) {
            theMap.remove(key);
        }
        return ret;
    }
}

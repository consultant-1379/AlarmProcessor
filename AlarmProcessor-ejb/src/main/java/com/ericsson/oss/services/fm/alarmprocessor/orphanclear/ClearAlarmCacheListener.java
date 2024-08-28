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

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import javax.cache.Cache.Entry;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.models.alarm.cache.ClearAlarmsListWrapper;

/**
 * The clear alarm cache listener class.
 */
public class ClearAlarmCacheListener implements CacheEntryCreatedListener<String, ClearAlarmsListWrapper>,
        CacheEntryRemovedListener<String, ClearAlarmsListWrapper>, CacheEntryUpdatedListener<String, ClearAlarmsListWrapper> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClearAlarmCacheListener.class);

    final ExpireClearAlarmManager expireClearAlarmManager;

    /**
     *  ClearAlarmCacheListener.
     * @param expireClearAlarmManager the expire clear manager
     *
     * */
    public ClearAlarmCacheListener(final ExpireClearAlarmManager expireClearAlarmManager) {
        LOGGER.debug("ClearAlarmCacheListener initialized");
        this.expireClearAlarmManager = expireClearAlarmManager;
    }

    /**
     * On Synchronize method callback.
     * @param events the received event.
     */
    public void onSync(final Iterator<Entry<String, ClearAlarmsListWrapper>> events) {
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(events, Spliterator.NONNULL), false).forEach(event -> this.expireClearAlarmManager
                .update(event.getKey(), event.getValue().getAlarmsList()));
    }

    /**
     * On Updated method callback.
     * @param events the received event.
     */
    @Override
    public void onUpdated(final Iterable<CacheEntryEvent<? extends String, ? extends ClearAlarmsListWrapper>> events) {
        LOGGER.debug("ClearAlarmCacheListener onUpdated {}", events);
        events.forEach(entry -> this.update(entry.getKey(), entry.getValue(), entry.getOldValue()));
    }

    /**
     * On Removed method callback.
     * @param events the received event.
     */
    @Override
    public void onRemoved(final Iterable<CacheEntryEvent<? extends String, ? extends ClearAlarmsListWrapper>> events) {
        LOGGER.debug("ClearAlarmCacheListener onRemoved {}", events);
        StreamSupport.stream(events.spliterator(), false).flatMap(event -> event.getValue().getAlarmsList().stream())
                .forEach(this.expireClearAlarmManager::remove);
    }

    /**
     * On Created method callback.
     * @param events the received event.
     */
    @Override
    public void onCreated(final Iterable<CacheEntryEvent<? extends String, ? extends ClearAlarmsListWrapper>> events) {
        LOGGER.debug("ClearAlarmCacheListener onCreated {}", events);
        StreamSupport.stream(events.spliterator(), false).flatMap(event -> event.getValue().getAlarmsList().stream())
                .forEach(this.expireClearAlarmManager::add);
    }

    private void update(final String key, final ClearAlarmsListWrapper newValue, final ClearAlarmsListWrapper oldValue) {
        LOGGER.debug("ClearAlarmCacheListener update {} {}", newValue, oldValue);
        this.expireClearAlarmManager.update(key, newValue.getAlarmsList());
    }
}

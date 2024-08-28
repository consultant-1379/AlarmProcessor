package com.ericsson.oss.services.fm.alarmprocessor.utility

import com.ericsson.oss.services.fm.alarmprocessor.orphanclear.ClearAlarmCacheListener
import com.ericsson.oss.services.models.alarm.cache.ClearAlarmsListWrapper

import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.event.CacheEntryEvent
import javax.cache.event.EventType
import javax.cache.integration.CompletionListener
import javax.cache.processor.EntryProcessor
import javax.cache.processor.EntryProcessorException
import javax.cache.processor.EntryProcessorResult

class MockedClearCache implements Cache<String, ClearAlarmsListWrapper> {

    class MockedEntryEvent extends CacheEntryEvent<String, ClearAlarmsListWrapper> {

        final String key;
        final ClearAlarmsListWrapper value;

        MockedEntryEvent(final Cache source, final EventType eventType, final String key, final ClearAlarmsListWrapper value) {
            super(source, eventType)
            this.key = key
            this.value = value
        }

        @Override
        ClearAlarmsListWrapper getOldValue() {
            return null
        }

        @Override
        boolean isOldValueAvailable() {
            return false
        }

        @Override
        String getKey() {
            return key
        }

        @Override
        ClearAlarmsListWrapper getValue() {
            return value
        }

        @Override
        def <T> T unwrap(final Class<T> aClass) {
            return null
        }
    }
    private Map<String, ClearAlarmsListWrapper> cache = new HashMap<>();
    private ClearAlarmCacheListener cacheListener;

    public ClearAlarmCacheListener getListener() {
        return this.cacheListener;
    }

    @Override
    <C extends Configuration<String, ClearAlarmsListWrapper> > C getConfiguration(Class<C> var1) {
        return null
    }

    @Override
    def <T> T invoke(final String s, final EntryProcessor<String, ClearAlarmsListWrapper, T> entryProcessor, final Object... objects) throws EntryProcessorException {
        return null
    }

    @Override
    def <T> Map<String, EntryProcessorResult<T>> invokeAll(final Set<? extends String> set, final EntryProcessor<String, ClearAlarmsListWrapper, T> entryProcessor, final Object... objects) {
        return null
    }

    @Override
    String getName() {
        return null
    }

    @Override
    CacheManager getCacheManager() {
        return null
    }

    @Override
    void close() {

    }

    @Override
    boolean isClosed() {
        return false
    }

    @Override
    def <T> T unwrap(final Class<T> aClass) {
        return null
    }

    @Override
    void registerCacheEntryListener(final CacheEntryListenerConfiguration<String, ClearAlarmsListWrapper> cacheEntryListenerConfiguration) {
        this.cacheListener = cacheEntryListenerConfiguration.getCacheEntryListenerFactory().create() as ClearAlarmCacheListener;
    }

    @Override
    void deregisterCacheEntryListener(final CacheEntryListenerConfiguration<String, ClearAlarmsListWrapper> cacheEntryListenerConfiguration) {

    }

    @Override
    Iterator<Entry<String, ClearAlarmsListWrapper>> iterator() {
        return cache.iterator()
    }

    @Override
    ClearAlarmsListWrapper get(final String s) {
        return cache.get(s)
    }

    @Override
    Map<String, ClearAlarmsListWrapper> getAll(final Set<? extends String> set) {
        return null
    }

    @Override
    boolean containsKey(final String s) {
        return false
    }

    @Override
    void loadAll(final Set<? extends String> set, final boolean b, final CompletionListener completionListener) {

    }

    @Override
    void put(final String s, final ClearAlarmsListWrapper clearAlarmsListWrapper) {
        List<CacheEntryEvent<String, ClearAlarmsListWrapper>> iterableList = new ArrayList<>();
        if(cache.get(s)) {
            iterableList.add(new MockedEntryEvent(this, EventType.UPDATED, s, clearAlarmsListWrapper));
            this.cacheListener.onUpdated(iterableList)
        } else {
            iterableList.add(new MockedEntryEvent(this, EventType.CREATED, s, clearAlarmsListWrapper));
            this.cacheListener.onCreated(iterableList)
        }
        cache.put(s, clearAlarmsListWrapper)
    }

    @Override
    ClearAlarmsListWrapper getAndPut(final String s, final ClearAlarmsListWrapper clearAlarmsListWrapper) {
        return null
    }

    @Override
    void putAll(final Map<? extends String, ? extends ClearAlarmsListWrapper> map) {

    }

    @Override
    boolean putIfAbsent(final String s, final ClearAlarmsListWrapper clearAlarmsListWrapper) {
        return false
    }

    @Override
    boolean remove(final String s) {

        ClearAlarmsListWrapper clearAlarmsListWrapper = cache.get(s);

        List<CacheEntryEvent<String, ClearAlarmsListWrapper>> iterableList = new ArrayList<>();
        CacheEntryEvent<String, ClearAlarmsListWrapper> event = [
                getOldValue        : { return null },
                isOldValueAvailable: { return false },
                getKey             : { return s },
                getValue           : { return clearAlarmsListWrapper },
                unwrap             : { aClass -> return null }
        ] as CacheEntryEvent
        iterableList.add(event);

        if(clearAlarmsListWrapper != null) {
            this.cacheListener.onRemoved(iterableList)
        }

        return cache.remove(s)
    }

    @Override
    boolean remove(final String s, final ClearAlarmsListWrapper clearAlarmsListWrapper) {

        List<CacheEntryEvent<String, ClearAlarmsListWrapper>> iterableList = new ArrayList<>();
        CacheEntryEvent<String, ClearAlarmsListWrapper> event = [
                getOldValue        : { return null },
                isOldValueAvailable: { return false },
                getKey             : { return s },
                getValue           : { return clearAlarmsListWrapper },
                unwrap             : { aClass -> return null }
        ] as CacheEntryEvent
        iterableList.add(event);

        if(clearAlarmsListWrapper != null) {
            this.cacheListener.onRemoved(iterableList)
        }

        return cache.remove(s, clearAlarmsListWrapper)
    }

    @Override
    ClearAlarmsListWrapper getAndRemove(final String s) {
        return null
    }

    @Override
    boolean replace(final String s, final ClearAlarmsListWrapper clearAlarmsListWrapper, final ClearAlarmsListWrapper v1) {
        return false
    }

    @Override
    boolean replace(final String s, final ClearAlarmsListWrapper clearAlarmsListWrapper) {
        return false
    }

    @Override
    ClearAlarmsListWrapper getAndReplace(final String s, final ClearAlarmsListWrapper clearAlarmsListWrapper) {
        return null
    }

    @Override
    void removeAll(final Set<? extends String> set) {
        this.cache.removeAll(set)
    }

    @Override
    void removeAll() {
        this.cache.removeAll()
    }

    @Override
    void clear() {
        removeAll()
    }
}

package com.ericsson.oss.itpf.sdk.cache.classic

import java.util.concurrent.ConcurrentHashMap
import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.integration.CompletionListener
import javax.cache.processor.*

/**
 * In memory cache.
 *
 * @param <K>
 *            the type of key in this cache.
 * @param <V>
 *            the type of values stored in this cache.
 */
public class StubMemoryCache<K, V> implements Cache<K, V> {

    private static final String UNSUPPORTED_MESSAGE = "XX Not supported by this implementation of cache"

    private String name

    private final Map<K, V> backingMap = new ConcurrentHashMap<>()

    /**
     * Create an in-memory cache with the given name.
     *
     * @param name
     *            the name of the cache.
     */
    public StubMemoryCache(final String name) {
        this.name = name
    }

    @Override
    public V get(final K k) {
        return backingMap.get(k)
    }

    @Override
    public Map<K, V> getAll(final Set<? extends K> set) {
        final Map<K, V> result = new HashMap<>()
        for (final K key : set) {
            if (backingMap.containsKey(key)) {
                result.put(key, backingMap.get(key))
            }
        }
        return result
    }

    @Override
    public boolean containsKey(final K k) {
        return backingMap.containsKey(k)
    }

    @Override
    public void loadAll(final Set<? extends K> set, final boolean b, final CompletionListener completionListener) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    }

    @Override
    public void put(final K key, final V value) {
        backingMap.put(key, value)
    }

    @Override
    public V getAndPut(final K key, final V value) {
        return backingMap.put(key, value)
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
        for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            backingMap.put(entry.getKey(), entry.getValue())
        }
    }

    @Override
    public boolean putIfAbsent(final K key, final V value) {
        if (backingMap.containsKey(key)) {
            return false
        } else {
            backingMap.put(key, value)
            return true
        }
    }

    @Override
    public boolean remove(final K key) {
        return backingMap.remove(key) != null
    }

    @Override
    public boolean remove(final K key, final V value) {
        if (Objects.equals(backingMap.get(key), value)) {
            backingMap.remove(key)
            return true
        }
        return false
    }

    @Override
    public V getAndRemove(final K key) {
        return backingMap.remove(key)
    }

    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
        if (backingMap.containsKey(key) && Objects.equals(backingMap.get(key), oldValue)) {
            backingMap.put(key, newValue)
            return true
        } else {
            return false
        }
    }

    @Override
    public boolean replace(final K key, final V value) {
        if (backingMap.containsKey(key)) {
            backingMap.put(key, value)
            return true
        } else {
            return false
        }
    }

    @Override
    public V getAndReplace(final K key, final V value) {
        if (backingMap.containsKey(key)) {
            final V oldValue = backingMap.get(key)
            backingMap.put(key, value)
            return oldValue
        } else {
            return null
        }
    }

    @Override
    public void removeAll(final Set<? extends K> setOfKeys) {
        for (final K key : setOfKeys) {
            remove(key)
        }
    }

    @Override
    public void removeAll() {
        backingMap.clear()
    }

    @Override
    public void clear() {
        backingMap.clear()
    }

    @Override
    public <C extends Configuration<K, V>> C getConfiguration(final Class<C> aClass) {
        Configuration<K, V> conf = new Configuration() {
              Class<K> getKeyType() {
                  return K.class
              }

              Class<V> getValueType() {
                  return V.class
              }

              boolean isStoreByValue() {
                  return true
              }
        }
        return conf
    }

    @Override
    public <T> T invoke(final K key, final EntryProcessor<K, V, T> entryProcessor, final Object... objects) throws EntryProcessorException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(final Set<? extends K> set, final EntryProcessor<K, V, T> entryProcessor,
                                                         final Object... objects) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    }

    @Override
    public String getName() {
        return this.name
    }

    @Override
    public CacheManager getCacheManager() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isClosed() {
        return false
    }

    @Override
    public <T> T unwrap(final Class<T> aClass) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    }

    @Override
    public void registerCacheEntryListener(final CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
    }

    @Override
    public void deregisterCacheEntryListener(final CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return new InMemoryCacheIterator()
    }

    private class InMemoryCacheIterator implements Iterator<Entry<K, V>> {
        private final Iterator<Map.Entry<K, V>> mapIterator = backingMap.entrySet().iterator()

        @Override
        public boolean hasNext() {
            return mapIterator.hasNext()
        }

        @Override
        public Entry<K, V> next() {
            final Map.Entry<K, V> currentEntry = mapIterator.next()
            return new Entry<K, V>() {
                @Override
                public K getKey() {
                    return currentEntry.getKey()
                }

                @Override
                public V getValue() {
                    return currentEntry.getValue()
                }

                @Override
                public <T> T unwrap(final Class<T> aClass) {
                    throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE)
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE)
        }
    }
}


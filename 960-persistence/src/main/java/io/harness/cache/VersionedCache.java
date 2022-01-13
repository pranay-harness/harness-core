/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cache;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.UnexpectedException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

public class VersionedCache<K, V> implements Cache<K, V> {
  private final Cache<VersionedKey<K>, V> jCache;
  private final String version;
  private static final String KEYS_CANNOT_BE_NULL_MESSAGE = "Keys for cache %s cannot be null";

  public VersionedCache(Cache<VersionedKey<K>, V> jCache, String version) {
    if (jCache == null) {
      throw new InvalidArgumentsException("JCache cannot be null");
    }
    if (isEmpty(version)) {
      throw new InvalidArgumentsException("Cannot create cache with blank or null version");
    }
    this.jCache = jCache;
    this.version = version;
  }

  private VersionedKey<K> buildVersionedKey(K key) {
    return new VersionedKey<>(key, version);
  }

  @Override
  public V get(K key) {
    if (key == null) {
      throw new UnexpectedException(String.format("Key for cache %s cannot be null", getName()));
    }
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    return jCache.get(cacheKey);
  }

  @Override
  public Map<K, V> getAll(Set<? extends K> keys) {
    if (keys == null) {
      throw new UnexpectedException(String.format(KEYS_CANNOT_BE_NULL_MESSAGE, getName()));
    }
    Set<VersionedKey<K>> cacheKeys = keys.stream().map(this::buildVersionedKey).collect(Collectors.toSet());
    Map<VersionedKey<K>, V> cacheResult = jCache.getAll(cacheKeys);
    Map<K, V> result = new HashMap<>();
    cacheResult.keySet().forEach(key -> result.put(key.getKey(), cacheResult.get(key)));
    return result;
  }

  @Override
  public boolean containsKey(K key) {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    return jCache.containsKey(cacheKey);
  }

  @Override
  public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener) {
    if (keys == null) {
      throw new UnexpectedException("Keys cannot be null");
    }
    Set<? extends VersionedKey<K>> cacheKeys = keys.stream().map(this::buildVersionedKey).collect(Collectors.toSet());
    jCache.loadAll(cacheKeys, replaceExistingValues, completionListener);
  }

  @Override
  public void put(K key, V value) {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    jCache.put(cacheKey, value);
  }

  @Override
  public V getAndPut(K key, V value) {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    return jCache.getAndPut(cacheKey, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    if (map == null) {
      throw new UnexpectedException("Map cannot be null");
    }
    Map<VersionedKey<K>, V> cacheMap = new HashMap<>();
    map.keySet().forEach(key -> cacheMap.put(buildVersionedKey(key), map.get(key)));
    jCache.putAll(cacheMap);
  }

  @Override
  public boolean putIfAbsent(K key, V value) {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    return jCache.putIfAbsent(cacheKey, value);
  }

  @Override
  public boolean remove(K key) {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    return jCache.remove(cacheKey);
  }

  @Override
  public boolean remove(K key, V oldValue) {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    return jCache.remove(cacheKey, oldValue);
  }

  @Override
  public V getAndRemove(K key) {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    return jCache.getAndRemove(cacheKey);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    return jCache.replace(cacheKey, oldValue, newValue);
  }

  @Override
  public boolean replace(K key, V value) {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    return jCache.replace(cacheKey, value);
  }

  @Override
  public V getAndReplace(K key, V value) {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    return jCache.getAndReplace(cacheKey, value);
  }

  @Override
  public void removeAll(Set<? extends K> keys) {
    if (keys == null) {
      throw new UnexpectedException(String.format(KEYS_CANNOT_BE_NULL_MESSAGE, getName()));
    }
    Set<? extends VersionedKey<K>> cacheKeys = keys.stream().map(this::buildVersionedKey).collect(Collectors.toSet());
    jCache.removeAll(cacheKeys);
  }

  @Override
  public void removeAll() {
    jCache.removeAll();
  }

  @Override
  public void clear() {
    jCache.clear();
  }

  @Override
  public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
    if (clazz.isAssignableFrom(VersionedCacheConfigurationWrapper.class)) {
      return (C) new VersionedCacheConfigurationWrapper<K, V>(jCache.getConfiguration(Configuration.class), version);
    }
    throw new InvalidArgumentsException("Casting to VersionedCacheConfiguration is only supported");
  }

  @Override
  public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
      throws EntryProcessorException {
    VersionedKey<K> cacheKey = buildVersionedKey(key);
    EntryProcessor<VersionedKey<K>, V, T> cacheEntryProcessor = new VersionedEntryProcessor<>(entryProcessor);
    return jCache.invoke(cacheKey, cacheEntryProcessor, arguments);
  }

  @Override
  public <T> Map<K, EntryProcessorResult<T>> invokeAll(
      Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
    if (keys == null) {
      throw new UnexpectedException(String.format(KEYS_CANNOT_BE_NULL_MESSAGE, getName()));
    }
    Set<? extends VersionedKey<K>> cacheKeys = keys.stream().map(this::buildVersionedKey).collect(Collectors.toSet());
    EntryProcessor<VersionedKey<K>, V, T> cacheEntryProcessor = new VersionedEntryProcessor<>(entryProcessor);
    Map<VersionedKey<K>, EntryProcessorResult<T>> cacheResult =
        jCache.invokeAll(cacheKeys, cacheEntryProcessor, arguments);
    Map<K, EntryProcessorResult<T>> result = new HashMap<>();
    cacheResult.keySet().forEach(key -> result.put(key.getKey(), cacheResult.get(key)));
    return result;
  }

  @Override
  public String getName() {
    return jCache.getName();
  }

  @Override
  public CacheManager getCacheManager() {
    return jCache.getCacheManager();
  }

  @Override
  public void close() {
    jCache.close();
  }

  @Override
  public boolean isClosed() {
    return jCache.isClosed();
  }

  @Override
  public <T> T unwrap(Class<T> clazz) {
    if (clazz.isAssignableFrom(getClass())) {
      return clazz.cast(this);
    }
    return jCache.unwrap(clazz);
  }

  private Factory<CacheEntryListener<? super VersionedKey<K>, ? super V>> getVersionedCacheEntryListenerFactory(
      Factory<CacheEntryListener<? super K, ? super V>> cacheFactory, VersionedCache<K, V> versionedCache) {
    return () -> {
      CacheEntryListener<? super K, ? super V> cacheListener = cacheFactory.create();
      return new VersionedCacheEntryListener<>(cacheListener, versionedCache);
    };
  }

  private CacheEntryListenerConfiguration<VersionedKey<K>, V> getVersionedCacheEntryListenerConfiguration(
      CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration, VersionedCache<K, V> versionedCache) {
    return new CacheEntryListenerConfiguration<VersionedKey<K>, V>() {
      @Override
      public Factory<CacheEntryListener<? super VersionedKey<K>, ? super V>> getCacheEntryListenerFactory() {
        Factory<CacheEntryListener<? super K, ? super V>> cacheFactory =
            cacheEntryListenerConfiguration.getCacheEntryListenerFactory();
        return getVersionedCacheEntryListenerFactory(cacheFactory, versionedCache);
      }

      @Override
      public boolean isOldValueRequired() {
        return cacheEntryListenerConfiguration.isOldValueRequired();
      }

      @Override
      public Factory<CacheEntryEventFilter<? super VersionedKey<K>, ? super V>> getCacheEntryEventFilterFactory() {
        Factory<CacheEntryEventFilter<? super K, ? super V>> cacheEntryEventFilterFactory =
            cacheEntryListenerConfiguration.getCacheEntryEventFilterFactory();
        return () -> {
          CacheEntryEventFilter<? super K, ? super V> cacheEntryEventFilter = cacheEntryEventFilterFactory.create();
          return event -> cacheEntryEventFilter.evaluate(new VersionedCacheEntryEventWrapper<>(event, versionedCache));
        };
      }

      @Override
      public boolean isSynchronous() {
        return cacheEntryListenerConfiguration.isSynchronous();
      }
    };
  }

  @Override
  public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
    CacheEntryListenerConfiguration<VersionedKey<K>, V> harnessCacheEntryListenerConfiguration =
        getVersionedCacheEntryListenerConfiguration(cacheEntryListenerConfiguration, this);
    jCache.registerCacheEntryListener(harnessCacheEntryListenerConfiguration);
  }

  @Override
  public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
    CacheEntryListenerConfiguration<VersionedKey<K>, V> harnessCacheEntryListenerConfiguration =
        getVersionedCacheEntryListenerConfiguration(cacheEntryListenerConfiguration, this);
    jCache.deregisterCacheEntryListener(harnessCacheEntryListenerConfiguration);
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    Iterator<Entry<VersionedKey<K>, V>> cacheIterator = jCache.iterator();
    return new Iterator<Entry<K, V>>() {
      @Override
      public boolean hasNext() {
        return cacheIterator.hasNext();
      }

      @Override
      public Entry<K, V> next() {
        Entry<VersionedKey<K>, V> cacheEntry = cacheIterator.next();
        return new Entry<K, V>() {
          @Override
          public K getKey() {
            return cacheEntry.getKey().getKey();
          }

          @Override
          public V getValue() {
            return cacheEntry.getValue();
          }

          @Override
          public <T> T unwrap(Class<T> clazz) {
            return cacheEntry.unwrap(clazz);
          }
        };
      }
    };
  }
}

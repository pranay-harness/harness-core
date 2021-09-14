/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cache;

import javax.cache.event.CacheEntryEvent;

public class VersionedCacheEntryEventWrapper<K, V> extends CacheEntryEvent<K, V> {
  private CacheEntryEvent<? extends VersionedKey<K>, ? extends V> versionedCacheEntryEvent;

  public VersionedCacheEntryEventWrapper(
      CacheEntryEvent<? extends VersionedKey<K>, ? extends V> versionedCacheEntryEvent,
      VersionedCache<K, V> sourceCache) {
    super(sourceCache, versionedCacheEntryEvent.getEventType());
    this.versionedCacheEntryEvent = versionedCacheEntryEvent;
  }

  @Override
  public V getOldValue() {
    return versionedCacheEntryEvent.getOldValue();
  }

  @Override
  public boolean isOldValueAvailable() {
    return versionedCacheEntryEvent.isOldValueAvailable();
  }

  @Override
  public K getKey() {
    return versionedCacheEntryEvent.getKey().getKey();
  }

  @Override
  public V getValue() {
    return versionedCacheEntryEvent.getValue();
  }

  @Override
  public <T> T unwrap(Class<T> clazz) {
    if (clazz.isAssignableFrom(getClass())) {
      return clazz.cast(this);
    }
    return versionedCacheEntryEvent.unwrap(clazz);
  }
}

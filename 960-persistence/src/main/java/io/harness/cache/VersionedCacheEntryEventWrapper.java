/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

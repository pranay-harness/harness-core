/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cache;

import java.util.Iterator;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

public class VersionedCacheEntryListener<K, V>
    implements CacheEntryListener<VersionedKey<K>, V>, CacheEntryCreatedListener<VersionedKey<K>, V>,
               CacheEntryExpiredListener<VersionedKey<K>, V>, CacheEntryUpdatedListener<VersionedKey<K>, V>,
               CacheEntryRemovedListener<VersionedKey<K>, V> {
  private CacheEntryListener<? super K, ? super V> cacheListener;
  private VersionedCache<K, V> versionedCache;

  public VersionedCacheEntryListener(
      CacheEntryListener<? super K, ? super V> cacheListener, VersionedCache<K, V> versionedCache) {
    this.cacheListener = cacheListener;
    this.versionedCache = versionedCache;
  }

  private Iterable<CacheEntryEvent<? extends K, ? extends V>> getHarnessEntryEvents(
      Iterable<CacheEntryEvent<? extends VersionedKey<K>, ? extends V>> cacheEntryEvents) {
    return () -> {
      Iterator<CacheEntryEvent<? extends VersionedKey<K>, ? extends V>> cacheIterator = cacheEntryEvents.iterator();
      return new Iterator<CacheEntryEvent<? extends K, ? extends V>>() {
        @Override
        public boolean hasNext() {
          return cacheIterator.hasNext();
        }

        @Override
        public CacheEntryEvent<? extends K, ? extends V> next() {
          CacheEntryEvent<VersionedKey<K>, V> cacheEntryEvent =
              (CacheEntryEvent<VersionedKey<K>, V>) cacheIterator.next();
          return new VersionedCacheEntryEventWrapper<>(cacheEntryEvent, versionedCache);
        }
      };
    };
  }

  @Override
  public void onCreated(Iterable<CacheEntryEvent<? extends VersionedKey<K>, ? extends V>> cacheEntryEvents)
      throws CacheEntryListenerException {
    if (CacheEntryCreatedListener.class.isAssignableFrom(cacheListener.getClass())) {
      Iterable<CacheEntryEvent<? extends K, ? extends V>> entryEvents = getHarnessEntryEvents(cacheEntryEvents);
      ((CacheEntryCreatedListener) cacheListener).onCreated(entryEvents);
    }
  }

  @Override
  public void onExpired(Iterable<CacheEntryEvent<? extends VersionedKey<K>, ? extends V>> cacheEntryEvents)
      throws CacheEntryListenerException {
    if (CacheEntryExpiredListener.class.isAssignableFrom(cacheListener.getClass())) {
      Iterable<CacheEntryEvent<? extends K, ? extends V>> entryEvents = getHarnessEntryEvents(cacheEntryEvents);
      ((CacheEntryExpiredListener) cacheListener).onExpired(entryEvents);
    }
  }

  @Override
  public void onRemoved(Iterable<CacheEntryEvent<? extends VersionedKey<K>, ? extends V>> cacheEntryEvents)
      throws CacheEntryListenerException {
    if (CacheEntryRemovedListener.class.isAssignableFrom(cacheListener.getClass())) {
      Iterable<CacheEntryEvent<? extends K, ? extends V>> entryEvents = getHarnessEntryEvents(cacheEntryEvents);
      ((CacheEntryRemovedListener) cacheListener).onRemoved(entryEvents);
    }
  }

  @Override
  public void onUpdated(Iterable<CacheEntryEvent<? extends VersionedKey<K>, ? extends V>> cacheEntryEvents)
      throws CacheEntryListenerException {
    if (CacheEntryUpdatedListener.class.isAssignableFrom(cacheListener.getClass())) {
      Iterable<CacheEntryEvent<? extends K, ? extends V>> entryEvents = getHarnessEntryEvents(cacheEntryEvents);
      ((CacheEntryUpdatedListener) cacheListener).onUpdated(entryEvents);
    }
  }
}

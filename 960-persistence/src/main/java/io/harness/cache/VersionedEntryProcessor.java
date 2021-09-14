/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cache;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

public class VersionedEntryProcessor<K, V, T> implements EntryProcessor<VersionedKey<K>, V, T> {
  private EntryProcessor<K, V, T> entryProcessor;

  public VersionedEntryProcessor(EntryProcessor<K, V, T> entryProcessor) {
    this.entryProcessor = entryProcessor;
  }

  public static class VersionedMutableEntryWrapper<K, V> implements MutableEntry<K, V> {
    private MutableEntry<VersionedKey<K>, V> versionedMutableEntry;

    public VersionedMutableEntryWrapper(MutableEntry<VersionedKey<K>, V> versionedMutableEntry) {
      this.versionedMutableEntry = versionedMutableEntry;
    }

    @Override
    public boolean exists() {
      return versionedMutableEntry.exists();
    }

    @Override
    public void remove() {
      versionedMutableEntry.remove();
    }

    @Override
    public void setValue(V value) {
      versionedMutableEntry.setValue(value);
    }

    @Override
    public K getKey() {
      return versionedMutableEntry.getKey().getKey();
    }

    @Override
    public V getValue() {
      return versionedMutableEntry.getValue();
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
      if (clazz.isAssignableFrom(getClass())) {
        return clazz.cast(this);
      }
      return versionedMutableEntry.unwrap(clazz);
    }
  }

  @Override
  public T process(MutableEntry<VersionedKey<K>, V> entry, Object... arguments) throws EntryProcessorException {
    MutableEntry<K, V> wrappedEntry = new VersionedMutableEntryWrapper<>(entry);
    return entryProcessor.process(wrappedEntry, arguments);
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.structure;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

class ImplementsMap<K, V> implements Map<K, V> {
  private boolean empty;

  ImplementsMap(boolean empty) {
    this.empty = empty;
  }

  @Override
  public int size() {
    return empty ? 0 : 1;
  }

  @Override
  public boolean isEmpty() {
    return empty;
  }

  @Override
  public boolean containsKey(Object key) {
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    return false;
  }

  @Override
  public V get(Object key) {
    return null;
  }

  @Override
  public V put(K key, V value) {
    return null;
  }

  @Override
  public V remove(Object key) {
    return null;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {}

  @Override
  public void clear() {}

  @Override
  public Set<K> keySet() {
    return null;
  }

  @Override
  public Collection<V> values() {
    return null;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return null;
  }
}

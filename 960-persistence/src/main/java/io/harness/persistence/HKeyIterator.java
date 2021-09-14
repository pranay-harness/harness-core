/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.persistence;

import java.util.Iterator;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.MorphiaKeyIterator;

// This is a simple wrapper around MorphiaKeyIterator to provide AutoCloseable implementation
public class HKeyIterator<T> implements AutoCloseable, Iterator<Key<T>>, Iterable<Key<T>> {
  private MorphiaKeyIterator<T> iterator;

  public HKeyIterator(MorphiaKeyIterator<T> iterator) {
    this.iterator = iterator;
  }

  @Override
  public void close() {
    iterator.close();
  }

  @Override
  public boolean hasNext() {
    return HPersistence.retry(() -> iterator.hasNext());
  }

  @Override
  public Key<T> next() {
    return HPersistence.retry(() -> iterator.next());
  }

  @Override
  // This is just wrapper around the morphia iterator, it cannot be reused anyways
  @SuppressWarnings("squid:S4348")
  public Iterator<Key<T>> iterator() {
    return this;
  }
}

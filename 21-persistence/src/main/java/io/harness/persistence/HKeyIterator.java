package io.harness.persistence;

import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.MorphiaKeyIterator;

import java.util.Iterator;

// This is a simple wrapper around MorphiaKeyIterator to provide AutoCloseable implementation
public class HKeyIterator<T> implements AutoCloseable, Iterator<Key<T>> {
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
    return iterator.hasNext();
  }

  @Override
  public Key<T> next() {
    return iterator.next();
  }
}

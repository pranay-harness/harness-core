package io.harness.persistence;

import org.mongodb.morphia.query.MorphiaIterator;

import java.util.Iterator;

// This is a simple wrapper around MorphiaIterator to provide AutoCloseable implementation
public class HIterator<T> implements AutoCloseable, Iterable<T>, Iterator<T> {
  private MorphiaIterator<T, T> iterator;

  public HIterator(MorphiaIterator<T, T> iterator) {
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
  public T next() {
    return HPersistence.retry(() -> iterator.next());
  }

  @Override
  public Iterator<T> iterator() {
    return this;
  }
}

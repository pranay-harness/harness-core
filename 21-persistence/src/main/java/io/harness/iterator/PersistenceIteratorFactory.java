package io.harness.iterator;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;

@Singleton
public final class PersistenceIteratorFactory {
  @Inject Injector injector;

  public <T extends PersistentIterable> PersistenceIterator create(MongoPersistenceIteratorBuilder<T> builder) {
    final MongoPersistenceIterator<T> iterator = builder.build();
    injector.injectMembers(iterator);
    return iterator;
  }
}

package io.harness.cvng.core.jobs;

import io.harness.mongo.iterator.MongoPersistenceIterator;

public interface DataCollectionTaskCreateNextTaskHandler<T> extends MongoPersistenceIterator.Handler<T> {
  @Override void handle(T entity);
}

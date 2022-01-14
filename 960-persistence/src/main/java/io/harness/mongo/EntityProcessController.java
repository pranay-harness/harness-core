package io.harness.mongo;

import io.harness.iterator.PersistentIterable;

public interface EntityProcessController<T extends PersistentIterable> {
  boolean shouldProcessEntity(T entity);
}

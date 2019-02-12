package io.harness.iterator;

import io.harness.persistence.PersistentIterable;

public interface PersistenceIterator<T extends PersistentIterable> {
  enum ProcessMode { LOOP, PUMP }

  void process(ProcessMode mode);
}

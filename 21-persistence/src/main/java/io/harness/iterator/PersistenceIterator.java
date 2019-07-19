package io.harness.iterator;

public interface PersistenceIterator<T extends PersistentIterable> {
  enum ProcessMode { LOOP, PUMP }

  void process(ProcessMode mode);
}

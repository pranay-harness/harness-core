package io.harness.iterator;

public interface PersistentRegularIterable extends PersistentIterable {
  void updateNextIteration(String fieldName, Long nextIteration);
}

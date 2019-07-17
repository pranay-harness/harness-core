package io.harness.persistence;

public interface PersistentRegularIterable extends PersistentIterable {
  void updateNextIteration(String fieldName, Long nextIteration);
}

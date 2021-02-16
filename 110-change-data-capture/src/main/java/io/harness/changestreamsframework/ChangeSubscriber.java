package io.harness.changestreamsframework;

import io.harness.persistence.PersistentEntity;

@FunctionalInterface
public interface ChangeSubscriber<T extends PersistentEntity> {
  void onChange(ChangeEvent<T> changeEvent);
}

package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.persistence.PersistentEntity;

public interface CDCEntity<T extends PersistentEntity> {
  ChangeHandler getTimescaleChangeHandler();
  Class<? extends PersistentEntity> getSubscriptionEntity();
}
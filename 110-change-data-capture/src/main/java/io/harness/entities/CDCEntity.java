package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

@OwnedBy(HarnessTeam.CE)
public interface CDCEntity<T extends PersistentEntity> {
  ChangeHandler getTimescaleChangeHandler();
  Class<? extends PersistentEntity> getSubscriptionEntity();
}

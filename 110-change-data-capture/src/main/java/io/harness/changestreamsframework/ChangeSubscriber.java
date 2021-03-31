package io.harness.changestreamsframework;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

@FunctionalInterface
@OwnedBy(HarnessTeam.CE)
public interface ChangeSubscriber<T extends PersistentEntity> {
  void onChange(ChangeEvent<T> changeEvent);
}

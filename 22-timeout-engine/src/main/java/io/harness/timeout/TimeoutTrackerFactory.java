package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistrableEntity;

@OwnedBy(CDC)
public interface TimeoutTrackerFactory<T extends TimeoutParameters> extends RegistrableEntity {
  TimeoutTracker create(T parameters);
}

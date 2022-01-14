package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class WaiterConfiguration {
  public enum PersistenceLayer { SPRING, MORPHIA }
  @Default PersistenceLayer persistenceLayer = PersistenceLayer.MORPHIA;
}

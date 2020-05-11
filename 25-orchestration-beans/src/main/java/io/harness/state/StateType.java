package io.harness.state;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class StateType implements RegistryKey {
  // Provided From the orchestration layer system states

  public static final String FORK = "FORK";

  @NonNull String type;
}

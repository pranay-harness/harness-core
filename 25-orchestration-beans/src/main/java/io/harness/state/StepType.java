package io.harness.state;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder(buildMethodName = "internalBuild")
@Redesign
public class StepType implements RegistryKey {
  // Provided From the orchestration layer system states

  public static final String FORK = "FORK";

  @NonNull String type;
  @NonNull String group;

  public static class StepTypeBuilder {
    public StepType build() {
      if (EmptyPredicate.isEmpty(group)) {
        group = type;
      }
      return internalBuild();
    }
  }
}

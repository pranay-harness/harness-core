package io.harness.state;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@OwnedBy(CDC)
@Redesign
@NoArgsConstructor
@EqualsAndHashCode
public class StepType implements RegistryKey {
  // Provided From the orchestration layer system states

  public static final String FORK = "FORK";

  @Getter @NonNull String type;
  @EqualsAndHashCode.Exclude @Getter @NonNull String group;

  @Builder(buildMethodName = "internalBuild")
  public StepType(@NonNull String type, @NonNull String group) {
    this.type = type;
    this.group = group;
  }

  public static class StepTypeBuilder {
    public StepType build() {
      if (EmptyPredicate.isEmpty(group)) {
        group = type;
      }
      return internalBuild();
    }
  }
}

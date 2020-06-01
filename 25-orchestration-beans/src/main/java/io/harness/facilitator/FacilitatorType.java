package io.harness.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@EqualsAndHashCode
public class FacilitatorType implements RegistryKey {
  // Provided From the orchestration layer system facilitators
  public static final String SYNC = "SYNC";
  public static final String ASYNC = "ASYNC";
  public static final String CHILD = "CHILD";
  public static final String CHILDREN = "CHILDREN";
  public static final String TASK = "TASK";
  public static final String TASK_CHAIN = "TASK_CHAIN";
  public static final String CHILD_CHAIN = "CHILD_CHAIN";

  @NonNull String type;
}

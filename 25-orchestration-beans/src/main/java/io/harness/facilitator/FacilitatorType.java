package io.harness.facilitator;

import io.harness.annotations.Redesign;
import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@Redesign
public class FacilitatorType implements RegistryKey {
  // Provided From the orchestration layer system facilitators
  public static final String SYNC = "SYNC";
  public static final String ASYNC = "ASYNC";
  public static final String CHILD = "CHILD";
  public static final String CHILDREN = "CHILDREN";
  public static final String SKIP = "SKIP";

  @NonNull String type;
}

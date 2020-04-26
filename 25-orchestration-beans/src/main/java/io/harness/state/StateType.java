package io.harness.state;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@Redesign
public class StateType {
  // Provided From the orchestration layer system states

  public static final String FORK = "FORK";

  @NonNull String type;
}

package io.harness.executionplan.plancreator.beans;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class PlanLevelNode {
  @NonNull String planNodeType;
  @NonNull String identifier;
}

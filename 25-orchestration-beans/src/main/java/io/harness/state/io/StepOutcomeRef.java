package io.harness.state.io;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class StepOutcomeRef {
  @NonNull String name;
  String instanceId;
}

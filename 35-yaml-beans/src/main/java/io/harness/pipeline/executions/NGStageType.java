package io.harness.pipeline.executions;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class NGStageType {
  @NonNull String type;
}

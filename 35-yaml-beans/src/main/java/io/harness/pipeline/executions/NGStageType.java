package io.harness.pipeline.executions;

import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class NGStageType implements RegistryKey {
  @NonNull String type;
}

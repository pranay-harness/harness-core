package io.harness.ambiance;

import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LevelType implements RegistryKey {
  String type;
}

package io.harness.references;

import io.harness.annotations.Redesign;
import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class RefType implements RegistryKey {
  public static final String SWEEPING_OUTPUT = "SWEEPING_OUTPUT";

  String type;
}

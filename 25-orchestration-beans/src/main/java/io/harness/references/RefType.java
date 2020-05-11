package io.harness.references;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class RefType implements RegistryKey {
  public static final String SWEEPING_OUTPUT = "SWEEPING_OUTPUT";
  public static final String OUTCOME = "OUTCOME";
  String type;
}

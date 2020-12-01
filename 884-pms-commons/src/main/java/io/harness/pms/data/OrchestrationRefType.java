package io.harness.pms.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationRefType {
  public static final String SWEEPING_OUTPUT = "SWEEPING_OUTPUT";
  public static final String OUTCOME = "OUTCOME";
}

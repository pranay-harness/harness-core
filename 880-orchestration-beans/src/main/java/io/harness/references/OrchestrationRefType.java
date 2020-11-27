package io.harness.references;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@Redesign
@UtilityClass
public class OrchestrationRefType {
  public static final String SWEEPING_OUTPUT = "SWEEPING_OUTPUT";
  public static final String OUTCOME = "OUTCOME";
}

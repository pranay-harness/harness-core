package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
public enum OrchestrationWorkflowType {
  BUILD,
  BASIC,
  CANARY,
  MULTI_SERVICE,
  BLUE_GREEN,
  ROLLING,
  CUSTOM
}

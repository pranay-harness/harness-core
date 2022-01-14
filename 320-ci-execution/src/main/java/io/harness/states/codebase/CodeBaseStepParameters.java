package io.harness.states.codebase;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.ExecutionSource;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CI)
@RecasterAlias("io.harness.states.codebase.CodeBaseStepParameters")
public class CodeBaseStepParameters implements StepParameters {
  String codeBaseSyncTaskId;
  String codeBaseDelegateTaskId;

  String connectorRef;
  ExecutionSource executionSource;
}

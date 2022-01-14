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
@RecasterAlias("io.harness.states.codebase.CodeBaseTaskStepParameters")
public class CodeBaseTaskStepParameters implements StepParameters {
  String connectorRef;
  String repoName;
  ExecutionSource executionSource;
}

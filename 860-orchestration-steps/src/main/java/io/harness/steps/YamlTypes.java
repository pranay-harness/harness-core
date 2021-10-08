package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface YamlTypes {
  String SOURCE = "source";
  String EXECUTION_TARGET = "executionTarget";
  String OUTPUT_VARIABLES = "outputVariables";
  String ENVIRONMENT_VARIABLES = "environmentVariables";
  String SPEC = "spec";
}

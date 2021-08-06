package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface OutputExpressionConstants {
  String K8S_INFRA_DELEGATE_CONFIG_OUTPUT_NAME = "K8S_INFRA_DELEGATE_CONFIG_OUTPUT";
  String ENVIRONMENT = "env";
  String OUTPUT = "output";
}

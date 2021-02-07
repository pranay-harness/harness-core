package io.harness.cdng.k8s;

import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

public interface K8sStepParameters extends StepParameters {
  ParameterField<String> getTimeout();
  ParameterField<Boolean> getSkipDryRun();
}

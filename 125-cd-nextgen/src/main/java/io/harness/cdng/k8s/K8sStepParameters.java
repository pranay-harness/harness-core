package io.harness.cdng.k8s;

import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import java.util.Map;

public interface K8sStepParameters extends StepParameters {
  Map<String, StepDependencySpec> getStepDependencySpecs();
  ParameterField<String> getTimeout();
  ParameterField<Boolean> getSkipDryRun();
}

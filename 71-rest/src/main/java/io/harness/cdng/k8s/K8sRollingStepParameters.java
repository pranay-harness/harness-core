package io.harness.cdng.k8s;

import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class K8sRollingStepParameters implements StepParameters {
  int timeout;
  boolean skipDryRun;
  Map<String, StepDependencySpec> stepDependencySpecs;
}

package io.harness.cdng.k8s;

import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.state.io.StepParameters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sRollingRollbackStepParameters implements StepParameters {
  private int timeout;
  Map<String, StepDependencySpec> stepDependencySpecs;
}

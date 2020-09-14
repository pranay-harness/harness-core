package io.harness.cdng.k8s;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.state.io.StepParameters;
import io.harness.utils.ParameterField;
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
  private ParameterField<Integer> timeout;
  @JsonIgnore Map<String, StepDependencySpec> stepDependencySpecs;
}

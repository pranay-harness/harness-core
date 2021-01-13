package io.harness.pms.sample.steps;

import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfrastructureStepParameters implements StepParameters {
  String environmentName;
  Map<String, Object> infrastructureDefinition;
  ParameterField<Boolean> tmpBool;
}

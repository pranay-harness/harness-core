package io.harness.cdng.environment.steps;

import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EnvironmentStepParameters implements StepParameters {
  EnvironmentYaml environment;
  EnvironmentYaml environmentOverrides;
}

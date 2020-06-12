package io.harness.cdng.infra.yaml;

import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InfrastructureYaml implements StepParameters {
  private EnvironmentYaml environmentYaml;
  private Infrastructure infrastructureSpec;
}

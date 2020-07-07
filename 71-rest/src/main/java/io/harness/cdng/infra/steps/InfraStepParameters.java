package io.harness.cdng.infra.steps;

import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InfraStepParameters implements StepParameters {
  Infrastructure infrastructure;
  Infrastructure infrastructureOverrides;
}

package io.harness.cdng.infra.steps;

import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.state.io.StepParameters;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InfraStepParameters implements StepParameters {
  PipelineInfrastructure pipelineInfrastructure;
}

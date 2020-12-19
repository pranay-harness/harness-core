package io.harness.cdng.infra.steps;

import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("infraStepParameters")
public class InfraStepParameters implements StepParameters {
  PipelineInfrastructure pipelineInfrastructure;
}

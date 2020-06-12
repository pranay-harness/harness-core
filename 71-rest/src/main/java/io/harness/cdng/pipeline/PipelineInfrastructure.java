package io.harness.cdng.pipeline;

import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.InfrastructureSpec;
import io.harness.data.Outcome;
import io.harness.state.Step;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PipelineInfrastructure implements Outcome {
  private InfrastructureSpec infrastructureSpec;
  private EnvironmentYaml environment;
  private List<Step> steps;
  private List<Step> rollbackSteps;
  private String previousStageIdentifier;
}

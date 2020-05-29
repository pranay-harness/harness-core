package io.harness.cdng.pipeline;

import io.harness.cdng.infra.beans.InfraDefinition;
import io.harness.data.Outcome;
import io.harness.state.Step;
import io.harness.yaml.core.intfc.Infrastructure;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
public class PipelineInfrastructure implements Infrastructure, Outcome {
  private InfraDefinition infraDefinition;
  private List<Step> steps;
  private List<Step> rollbackSteps;
  private String previousStageIdentifier;

  @NotNull
  @Override
  public String getType() {
    return "CD Infrastructure";
  }
}

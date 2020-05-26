package software.wings.cdng.pipeline;

import lombok.Builder;
import lombok.Data;
import software.wings.cdng.infra.beans.InfraDefinition;

import java.util.List;

@Data
@Builder
public class PipelineInfrastructure {
  private InfraDefinition infraDefinition;
  private List<Step> steps;
  private List<Step> rollbackSteps;
}

package software.wings.beans;

import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class PipelineYaml extends BaseEntityYaml {
  private String description;
  private List<PipelineStageYaml> pipelineStages = new ArrayList<>();
  private List<FailureStrategyYaml> failureStrategies;

  @lombok.Builder
  public PipelineYaml(String harnessApiVersion, String description, List<PipelineStageYaml> pipelineStages) {
    super(EntityType.PIPELINE.name(), harnessApiVersion);
    this.description = description;
    this.pipelineStages = pipelineStages;
  }
}

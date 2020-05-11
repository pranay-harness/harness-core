package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.Action.ActionType;

import java.util.List;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("PIPELINE")
@JsonPropertyOrder({"harnessApiVersion"})
public class PipelineActionYaml extends ActionYaml {
  private String pipelineName;
  private boolean excludeHostsWithSameArtifact;
  private List<TriggerArtifactVariableYaml> artifactSelections;
  private List<TriggerVariableYaml> variables;

  public PipelineActionYaml() {
    super.setType(ActionType.PIPELINE.name());
  }

  @Builder
  PipelineActionYaml(String pipelineName, List<TriggerArtifactVariableYaml> artifactSelections,
      List<TriggerVariableYaml> variables, boolean excludeHostsWithSameArtifact) {
    super.setType(ActionType.PIPELINE.name());
    this.pipelineName = pipelineName;
    this.artifactSelections = artifactSelections;
    this.variables = variables;
    this.excludeHostsWithSameArtifact = excludeHostsWithSameArtifact;
  }
}

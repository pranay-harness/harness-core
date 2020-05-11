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
@JsonTypeName("WORKFLOW")
@JsonPropertyOrder({"harnessApiVersion"})
public class WorkflowActionYaml extends ActionYaml {
  private String workflowName;
  private boolean excludeHostsWithSameArtifact;
  private List<TriggerArtifactVariableYaml> artifactSelections;
  private List<TriggerVariableYaml> variables;

  public WorkflowActionYaml() {
    super.setType(ActionType.WORKFLOW.name());
  }

  @Builder
  WorkflowActionYaml(String workflowName, List<TriggerArtifactVariableYaml> artifactSelections,
      List<TriggerVariableYaml> variables, boolean excludeHostsWithSameArtifact) {
    super.setType(ActionType.WORKFLOW.name());
    this.workflowName = workflowName;
    this.artifactSelections = artifactSelections;
    this.variables = variables;
    this.excludeHostsWithSameArtifact = excludeHostsWithSameArtifact;
  }
}

package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYaml;

import java.util.List;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TriggerArtifactVariableYaml extends BaseYaml {
  private String entityType;
  private String entityName;
  private List<TriggerArtifactSelectionValueYaml> variableValue;

  @Builder
  public TriggerArtifactVariableYaml(
      String entityType, String entityName, List<TriggerArtifactSelectionValueYaml> variableValue) {
    this.entityType = entityType;
    this.entityName = entityName;
    this.variableValue = variableValue;
  }
}

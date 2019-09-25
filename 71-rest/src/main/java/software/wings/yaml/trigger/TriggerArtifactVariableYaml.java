package software.wings.yaml.trigger;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYaml;

import java.util.List;

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

package software.wings.beans.trigger;

import software.wings.beans.EntityType;
import software.wings.yaml.BaseEntityYaml;
import software.wings.yaml.trigger.TriggerConditionYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class TriggerYaml extends BaseEntityYaml {
  private String description;
  @NotEmpty List<TriggerConditionYaml> triggerCondition = new ArrayList<>();
  private String executionType;
  private String executionName;
  private boolean continueWithDefaultValues;
  private List<ArtifactSelectionYaml> artifactSelections = new ArrayList<>();
  private List<ManifestSelection.Yaml> manifestSelections = new ArrayList<>();
  private List<TriggerVariable> workflowVariables = new ArrayList<>();

  @lombok.Builder
  public TriggerYaml(String harnessApiVersion, String description, String executionType, String executionName,
      List<TriggerVariable> workflowVariables, List<TriggerConditionYaml> triggerCondition,
      List<ArtifactSelectionYaml> artifactSelections, List<ManifestSelection.Yaml> manifestSelections,
      boolean continueWithDefaultValues) {
    super(EntityType.TRIGGER.name(), harnessApiVersion);
    this.continueWithDefaultValues = continueWithDefaultValues;
    this.setHarnessApiVersion(harnessApiVersion);
    this.description = description;
    this.executionType = executionType;
    this.executionName = executionName;
    this.workflowVariables = workflowVariables;
    this.triggerCondition = triggerCondition;
    this.artifactSelections = artifactSelections;
    this.manifestSelections = manifestSelections;
  }
}

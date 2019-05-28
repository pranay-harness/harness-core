package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SCHEDULED")
@JsonPropertyOrder({"harnessApiVersion"})
public class ScheduleTriggerConditionYaml extends TriggerConditionYaml {
  private String cronExpression;
  private String cronDescription;
  private boolean onNewArtifact;

  public ScheduleTriggerConditionYaml() {
    super.setType("SCHEDULED");
  }
  @lombok.Builder
  public ScheduleTriggerConditionYaml(String cronExpression, String cronDescription, boolean onNewArtifact) {
    super.setType("SCHEDULED");
    this.cronExpression = cronExpression;
    this.cronDescription = cronDescription;
    this.onNewArtifact = onNewArtifact;
  }
}

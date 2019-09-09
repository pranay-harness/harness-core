package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.Condition.Type;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SCHEDULED")
@JsonPropertyOrder({"harnessApiVersion"})
public class ScheduledConditionYaml extends ConditionYaml {
  private String cronExpression;
  private String cronDescription;
  private boolean onNewArtifactOnly;

  public ScheduledConditionYaml() {
    super.setType(Type.SCHEDULED.name());
  }

  @Builder
  public ScheduledConditionYaml(String cronExpression, String cronDescription, boolean onNewArtifactOnly) {
    super.setType(Type.SCHEDULED.name());
    this.cronExpression = cronExpression;
    this.cronDescription = cronDescription;
    this.onNewArtifactOnly = onNewArtifactOnly;
  }
}

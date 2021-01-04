package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.trigger.TriggerConditionType.SCHEDULED;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by sgurubelli on 10/25/17.
 */
@OwnedBy(CDC)
@JsonTypeName("SCHEDULED")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ScheduledTriggerCondition extends TriggerCondition {
  @NotEmpty private String cronExpression;
  private String cronDescription;
  @Builder.Default private boolean onNewArtifactOnly = true;

  public ScheduledTriggerCondition() {
    super(SCHEDULED);
  }

  public ScheduledTriggerCondition(String cronExpression, String cronDescription, boolean onNewArtifactOnly) {
    this();
    this.cronExpression = cronExpression;
    this.cronDescription = cronDescription;
    this.onNewArtifactOnly = onNewArtifactOnly;
  }
}

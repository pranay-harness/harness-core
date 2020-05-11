package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EntityType;
import software.wings.yaml.BaseEntityYaml;

import java.util.List;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DeploymentTriggerYaml extends BaseEntityYaml {
  private String description;
  @NotEmpty List<ConditionYaml> condition;
  @NotEmpty List<ActionYaml> action;
  private boolean triggerDisabled;

  @Builder
  public DeploymentTriggerYaml(String harnessApiVersion, String description, List<ConditionYaml> condition,
      boolean triggerDisabled, List<ActionYaml> action) {
    super(EntityType.TRIGGER.name(), harnessApiVersion);
    this.setHarnessApiVersion(harnessApiVersion);
    this.description = description;
    this.condition = condition;
    this.action = action;
    this.triggerDisabled = triggerDisabled;
  }
}

package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.ScheduledCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.trigger.ScheduledConditionYaml;

import java.util.List;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class ScheduledConditionYamlHandler extends ConditionYamlHandler<ScheduledConditionYaml> {
  @Override
  public ScheduledConditionYaml toYaml(Condition bean, String appId) {
    ScheduledCondition condition = (ScheduledCondition) bean;
    return ScheduledConditionYaml.builder()
        .cronDescription(condition.getCronDescription())
        .cronExpression(condition.getCronExpression())
        .onNewArtifactOnly(condition.isOnNewArtifactOnly())
        .build();
  }

  @Override
  public Condition upsertFromYaml(
      ChangeContext<ScheduledConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    ScheduledConditionYaml yaml = changeContext.getYaml();

    return ScheduledCondition.builder()
        .cronDescription(yaml.getCronDescription())
        .cronExpression(yaml.getCronExpression())
        .onNewArtifactOnly(yaml.isOnNewArtifactOnly())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return ScheduledConditionYaml.class;
  }
}

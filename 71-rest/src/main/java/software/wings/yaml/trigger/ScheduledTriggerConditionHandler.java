package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.trigger.TriggerConditionYamlHandler;

import java.util.List;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class ScheduledTriggerConditionHandler extends TriggerConditionYamlHandler<ScheduleTriggerConditionYaml> {
  @Override
  public ScheduleTriggerConditionYaml toYaml(TriggerCondition bean, String appId) {
    ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) bean;

    return ScheduleTriggerConditionYaml.builder()
        .cronDescription(scheduledTriggerCondition.getCronDescription())
        .cronExpression(scheduledTriggerCondition.getCronExpression())
        .onNewArtifact(scheduledTriggerCondition.isOnNewArtifactOnly())
        .build();
  }

  @Override
  public TriggerCondition upsertFromYaml(
      ChangeContext<ScheduleTriggerConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    TriggerConditionYaml yaml = changeContext.getYaml();
    ScheduleTriggerConditionYaml scheduleTriggerConditionYaml = (ScheduleTriggerConditionYaml) yaml;

    return ScheduledTriggerCondition.builder()
        .cronDescription(scheduleTriggerConditionYaml.getCronDescription())
        .cronExpression(scheduleTriggerConditionYaml.getCronExpression())
        .onNewArtifactOnly(scheduleTriggerConditionYaml.isOnNewArtifact())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return ScheduledTriggerConditionHandler.class;
  }
}

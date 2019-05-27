package software.wings.yaml.trigger;

import com.google.inject.Singleton;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.trigger.TriggerConditionYamlHandler;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class PipelineTriggerConditionHandler extends TriggerConditionYamlHandler<PipelineTriggerConditionYaml> {
  @Override
  public PipelineTriggerConditionYaml toYaml(TriggerCondition bean, String appId) {
    PipelineTriggerCondition pipelineTriggerCondition = (PipelineTriggerCondition) bean;

    return PipelineTriggerConditionYaml.builder().pipelineName(pipelineTriggerCondition.getPipelineName()).build();
  }

  @Override
  public TriggerCondition upsertFromYaml(
      ChangeContext<PipelineTriggerConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    TriggerConditionYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    PipelineTriggerConditionYaml pipelineTriggerConditionYaml = (PipelineTriggerConditionYaml) yaml;
    String pipelineName = pipelineTriggerConditionYaml.getPipelineName();

    return PipelineTriggerCondition.builder()
        .pipelineName(pipelineTriggerConditionYaml.getPipelineName())
        .pipelineId(yamlHelper.getPipelineId(appId, pipelineName))
        .build();
  }

  @Override
  public Class getYamlClass() {
    return PipelineTriggerConditionHandler.class;
  }
}

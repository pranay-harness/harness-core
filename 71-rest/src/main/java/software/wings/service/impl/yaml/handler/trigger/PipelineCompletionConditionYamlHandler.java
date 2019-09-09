package software.wings.service.impl.yaml.handler.trigger;

import com.google.inject.Singleton;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.PipelineCondition;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.trigger.PipelineCompletionConditionYaml;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class PipelineCompletionConditionYamlHandler extends ConditionYamlHandler<PipelineCompletionConditionYaml> {
  @Override
  public PipelineCompletionConditionYaml toYaml(Condition bean, String appId) {
    PipelineCondition condition = (PipelineCondition) bean;
    return PipelineCompletionConditionYaml.builder().pipelineName(condition.getPipelineName()).build();
  }

  @Override
  public Condition upsertFromYaml(
      ChangeContext<PipelineCompletionConditionYaml> changeContext, List<ChangeContext> changeSetContext) {
    PipelineCompletionConditionYaml yaml = changeContext.getYaml();

    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    String pipelineName = yaml.getPipelineName();

    return PipelineCondition.builder()
        .pipelineName(yaml.getPipelineName())
        .pipelineId(yamlHelper.getPipelineId(appId, pipelineName))
        .build();
  }

  @Override
  public Class getYamlClass() {
    return PipelineCompletionConditionYaml.class;
  }
}

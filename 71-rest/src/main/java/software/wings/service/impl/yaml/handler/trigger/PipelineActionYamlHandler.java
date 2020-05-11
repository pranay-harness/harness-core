package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.Pipeline;
import software.wings.beans.Variable;
import software.wings.beans.trigger.Action;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.trigger.PipelineActionYaml;
import software.wings.yaml.trigger.TriggerArtifactVariableYaml;
import software.wings.yaml.trigger.TriggerVariableYaml;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class PipelineActionYamlHandler extends ActionYamlHandler<PipelineActionYaml> {
  @Override
  public PipelineActionYaml toYaml(Action bean, String appId) {
    PipelineAction action = (PipelineAction) bean;
    TriggerArgs triggerArgs = action.getTriggerArgs();

    PipelineActionYaml yaml = PipelineActionYaml.builder().pipelineName(action.getPipelineName()).build();

    if (triggerArgs != null) {
      List<TriggerArtifactVariableYaml> triggerArtifactVariableYamls = new ArrayList<>();
      if (isNotEmpty(triggerArgs.getTriggerArtifactVariables())) {
        getArtifactVariableYaml(appId, triggerArgs, triggerArtifactVariableYamls);
        yaml.setArtifactSelections(triggerArtifactVariableYamls);
      }

      List<TriggerVariableYaml> variableYamls = new ArrayList<>();

      if (isNotEmpty(triggerArgs.getVariables())) {
        getTriggerVariablesYaml(appId, triggerArgs, variableYamls);
        yaml.setVariables(variableYamls);
      }

      yaml.setExcludeHostsWithSameArtifact(triggerArgs.isExcludeHostsWithSameArtifact());
    }

    return yaml;
  }

  @Override
  public Action upsertFromYaml(ChangeContext<PipelineActionYaml> changeContext, List<ChangeContext> changeSetContext) {
    Change change = changeContext.getChange();
    String accountId = change.getAccountId();
    String appId = yamlHelper.getAppId(accountId, change.getFilePath());
    PipelineActionYaml yaml = changeContext.getYaml();

    Pipeline pipeline = yamlHelper.getPipeline(appId, yaml.getPipelineName());
    notNullCheck("Invalid Pipeline " + yaml.getPipelineName(), pipeline, USER);

    List<Variable> triggerVariables = new ArrayList<>();
    getTriggerVariablesBean(accountId, appId, pipeline.getEnvIds().get(0), yaml.getVariables(), triggerVariables);

    List<TriggerArtifactVariable> triggerArtifactVariables = new ArrayList<>();
    getArtifactVariableBean(
        accountId, appId, changeContext, changeSetContext, yaml.getArtifactSelections(), triggerArtifactVariables);

    TriggerArgs triggerArgs = TriggerArgs.builder()
                                  .excludeHostsWithSameArtifact(yaml.isExcludeHostsWithSameArtifact())
                                  .variables(triggerVariables)
                                  .triggerArtifactVariables(triggerArtifactVariables)
                                  .build();

    return PipelineAction.builder()
        .pipelineName(yaml.getPipelineName())
        .pipelineId(pipeline.getUuid())
        .triggerArgs(triggerArgs)
        .build();
  }

  @Override
  public Class getYamlClass() {
    return PipelineActionYaml.class;
  }
}

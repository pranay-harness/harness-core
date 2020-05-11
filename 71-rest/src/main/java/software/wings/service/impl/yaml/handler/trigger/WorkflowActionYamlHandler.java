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
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.Action;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.trigger.TriggerArtifactVariableYaml;
import software.wings.yaml.trigger.TriggerVariableYaml;
import software.wings.yaml.trigger.WorkflowActionYaml;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Singleton
public class WorkflowActionYamlHandler extends ActionYamlHandler<WorkflowActionYaml> {
  @Override
  public WorkflowActionYaml toYaml(Action bean, String appId) {
    WorkflowAction action = (WorkflowAction) bean;
    WorkflowActionYaml yaml = WorkflowActionYaml.builder().workflowName(action.getWorkflowName()).build();

    TriggerArgs triggerArgs = action.getTriggerArgs();
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
  public Action upsertFromYaml(ChangeContext<WorkflowActionYaml> changeContext, List<ChangeContext> changeSetContext) {
    Change change = changeContext.getChange();
    String accountId = change.getAccountId();
    String appId = yamlHelper.getAppId(accountId, change.getFilePath());
    WorkflowActionYaml yaml = changeContext.getYaml();

    Workflow workflow = yamlHelper.getWorkflowFromName(appId, yaml.getWorkflowName());
    notNullCheck("Invalid Workflow " + yaml.getWorkflowName(), workflow, USER);

    List<Variable> triggerVariables = new ArrayList<>();
    if (isNotEmpty(yaml.getVariables())) {
      getTriggerVariablesBean(accountId, appId, workflow.getEnvId(), yaml.getVariables(), triggerVariables);
    }

    List<TriggerArtifactVariable> triggerArtifactVariables = new ArrayList<>();
    if (isNotEmpty(yaml.getArtifactSelections())) {
      getArtifactVariableBean(
          accountId, appId, changeContext, changeSetContext, yaml.getArtifactSelections(), triggerArtifactVariables);
    }

    TriggerArgs triggerArgs = TriggerArgs.builder()
                                  .excludeHostsWithSameArtifact(yaml.isExcludeHostsWithSameArtifact())
                                  .variables(triggerVariables)
                                  .triggerArtifactVariables(triggerArtifactVariables)
                                  .build();

    return WorkflowAction.builder()
        .workflowName(yaml.getWorkflowName())
        .workflowId(workflow.getUuid())
        .triggerArgs(triggerArgs)
        .build();
  }

  @Override
  public Class getYamlClass() {
    return WorkflowActionYaml.class;
  }
}

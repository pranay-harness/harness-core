package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import software.wings.beans.Workflow;
import software.wings.beans.trigger.TriggerArtifactSelectionLastDeployed;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.yaml.trigger.TriggerArtifactSelectionLastDeployedYaml;

import java.util.List;

public class TriggerArtifactLastDeployedYamlHandler
    extends TriggerArtifactValueYamlHandler<TriggerArtifactSelectionLastDeployedYaml> {
  @Inject protected YamlHelper yamlHelper;

  @Override
  public TriggerArtifactSelectionLastDeployedYaml toYaml(TriggerArtifactSelectionValue bean, String appId) {
    TriggerArtifactSelectionLastDeployed triggerArtifactSelectionLastDeployed =
        (TriggerArtifactSelectionLastDeployed) bean;

    return TriggerArtifactSelectionLastDeployedYaml.builder()
        .workflowName(triggerArtifactSelectionLastDeployed.getWorkflowName())
        .build();
  }

  @Override
  public TriggerArtifactSelectionValue upsertFromYaml(
      ChangeContext<TriggerArtifactSelectionLastDeployedYaml> changeContext, List<ChangeContext> changeSetContext) {
    Change change = changeContext.getChange();
    String accountId = change.getAccountId();
    String appId = yamlHelper.getAppId(accountId, change.getFilePath());
    TriggerArtifactSelectionLastDeployedYaml yaml = changeContext.getYaml();

    Workflow workflow = yamlHelper.getWorkflow(appId, yaml.getWorkflowName());
    notNullCheck("Invalid workflow", workflow, USER);

    return TriggerArtifactSelectionLastDeployed.builder()
        .workflowId(workflow.getUuid())
        .workflowName(workflow.getName())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return null;
  }
}

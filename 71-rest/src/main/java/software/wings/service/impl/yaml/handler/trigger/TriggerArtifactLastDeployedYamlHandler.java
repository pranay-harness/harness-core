package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.trigger.TriggerLastDeployedType.PIPELINE;
import static software.wings.beans.trigger.TriggerLastDeployedType.WORKFLOW;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.TriggerException;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.TriggerArtifactSelectionLastDeployed;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.yaml.trigger.TriggerArtifactSelectionLastDeployedYaml;

import java.util.List;

@OwnedBy(CDC)
public class TriggerArtifactLastDeployedYamlHandler
    extends TriggerArtifactValueYamlHandler<TriggerArtifactSelectionLastDeployedYaml> {
  @Inject protected YamlHelper yamlHelper;

  @Override
  public TriggerArtifactSelectionLastDeployedYaml toYaml(TriggerArtifactSelectionValue bean, String appId) {
    TriggerArtifactSelectionLastDeployed triggerArtifactSelectionLastDeployed =
        (TriggerArtifactSelectionLastDeployed) bean;

    return TriggerArtifactSelectionLastDeployedYaml.builder()
        .name(triggerArtifactSelectionLastDeployed.getName())
        .type(triggerArtifactSelectionLastDeployed.getType().name())
        .build();
  }

  @Override
  public TriggerArtifactSelectionValue upsertFromYaml(
      ChangeContext<TriggerArtifactSelectionLastDeployedYaml> changeContext, List<ChangeContext> changeSetContext) {
    Change change = changeContext.getChange();
    String accountId = change.getAccountId();
    String appId = yamlHelper.getAppId(accountId, change.getFilePath());
    TriggerArtifactSelectionLastDeployedYaml yaml = changeContext.getYaml();

    if (yaml.getType().equals("WORKFLOW")) {
      Workflow workflow = yamlHelper.getWorkflowFromName(appId, yaml.getName());
      notNullCheck("Invalid workflow", workflow, USER);
      return TriggerArtifactSelectionLastDeployed.builder()
          .id(workflow.getUuid())
          .type(WORKFLOW)
          .name(workflow.getName())
          .build();
    } else if (yaml.getType().equals("PIPELINE")) {
      Pipeline pipeline = yamlHelper.getPipeline(appId, yaml.getName());
      notNullCheck("Invalid pipeline", pipeline, USER);
      return TriggerArtifactSelectionLastDeployed.builder()
          .id(pipeline.getUuid())
          .type(PIPELINE)
          .name(pipeline.getName())
          .build();
    } else {
      throw new TriggerException("Invalid input type " + yaml.getType(), USER);
    }
  }

  @Override
  public Class getYamlClass() {
    return null;
  }
}

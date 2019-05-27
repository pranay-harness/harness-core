package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.WorkflowType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.Yaml;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.TriggerService;
import software.wings.utils.Validator;
import software.wings.yaml.trigger.TriggerConditionYaml;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class TriggerYamlHandler extends BaseYamlHandler<Yaml, Trigger> {
  @Inject private TriggerService triggerService;
  private String WORKFLOW = "Workflow";
  private String PIPELINE = "Pipeline";
  @Inject YamlHelper yamlHelper;
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Override
  public void delete(ChangeContext<Yaml> changeContext) {}

  @Override
  public Yaml toYaml(Trigger bean, String appId) {
    TriggerConditionYamlHandler handler =
        yamlHandlerFactory.getYamlHandler(YamlType.TRIGGER_CONDITION, bean.getCondition().getConditionType().name());

    ArtifactSelectionYamlHandler artifactSelectionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.ARTIFACT_SELECTION);

    String executionType = getExecutionType(bean.getWorkflowType());
    String executionName = null;
    String workflowOrPipelineId = bean.getWorkflowId();
    if (bean.getWorkflowType().equals(WorkflowType.ORCHESTRATION)) {
      executionName = yamlHelper.getWorkflowName(appId, workflowOrPipelineId);
    } else if (bean.getWorkflowType().equals(WorkflowType.PIPELINE)) {
      executionName = yamlHelper.getPipelineName(appId, workflowOrPipelineId);
    }

    List<ArtifactSelection.Yaml> artifactSelectionList =
        bean.getArtifactSelections()
            .stream()
            .map(artifactSelection -> { return artifactSelectionYamlHandler.toYaml(artifactSelection, appId); })
            .collect(Collectors.toList());

    Trigger.Yaml yaml = Trigger.Yaml.builder()
                            .description(bean.getDescription())
                            .executionType(executionType)
                            .triggerCondition(handler.toYaml(bean.getCondition(), appId))
                            .executionName(executionName)
                            .workflowVariables(bean.getWorkflowVariables())
                            .artifactSelections(artifactSelectionList)
                            .build();

    yaml.setType(bean.getCondition().getConditionType().name());
    return yaml;
  }

  @Override
  public Trigger upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Trigger trigger = toBean(changeContext, changeSetContext);
    triggerService.update(trigger);
    return trigger;
  }

  @Override
  public Class getYamlClass() {
    return Trigger.Yaml.class;
  }

  @Override
  public Trigger get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId);
    return yamlHelper.getTrigger(appId, yamlFilePath);
  }

  private Trigger toBean(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml yaml = changeContext.getYaml();
    Change change = changeContext.getChange();
    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
    notNullCheck("Could not locate app info in file path:" + change.getFilePath(), appId, USER);

    TriggerConditionYamlHandler handler = yamlHandlerFactory.getYamlHandler(YamlType.TRIGGER_CONDITION, yaml.getType());

    ArtifactSelectionYamlHandler artifactSelectionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.ARTIFACT_SELECTION);

    TriggerConditionYaml condition = yaml.getTriggerCondition();

    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, condition);
    TriggerCondition triggerCondition = handler.upsertFromYaml(clonedContext.build(), changeSetContext);

    Trigger trigger = yamlHelper.getTrigger(appId, change.getFilePath());

    List<ArtifactSelection> artifactSelectionList = new ArrayList<>();
    yaml.getArtifactSelections().forEach(artifactSelectionYaml -> {
      ChangeContext.Builder artifactClonedContext = cloneFileChangeContext(changeContext, artifactSelectionYaml);
      ArtifactSelection artifactSelection =
          artifactSelectionYamlHandler.upsertFromYaml(artifactClonedContext.build(), changeSetContext);
      artifactSelectionList.add(artifactSelection);
    });

    WorkflowType workflowType = getWorkflowType(changeContext.getChange().getFilePath(), yaml.getExecutionType());
    Trigger updatedTrigger = Trigger.builder()
                                 .description(yaml.getDescription())
                                 .name(trigger.getName())
                                 .workflowName(yaml.getExecutionName())
                                 .workflowType(workflowType)
                                 .condition(triggerCondition)
                                 .appId(appId)
                                 .uuid(trigger.getUuid())
                                 .artifactSelections(artifactSelectionList)
                                 .workflowVariables(yaml.getWorkflowVariables())
                                 .build();

    updatedTrigger.setWebHookToken(trigger.getWebHookToken());

    if (workflowType == ORCHESTRATION) {
      Workflow workflow = yamlHelper.getWorkflowFromName(appId, yaml.getExecutionName());
      updatedTrigger.setWorkflowId(workflow.getUuid());
    } else if (workflowType == WorkflowType.PIPELINE) {
      String pipelineId = yamlHelper.getPipelineId(appId, yaml.getExecutionName());
      updatedTrigger.setWorkflowId(pipelineId);
    }

    return updatedTrigger;
  }

  private String getExecutionType(WorkflowType workflowType) {
    if (workflowType.equals(ORCHESTRATION)) {
      return WORKFLOW;
    } else if (workflowType.equals(WorkflowType.PIPELINE)) {
      return PIPELINE;
    } else {
      Validator.notNullCheck("WorkflowType type is invalid", workflowType);
      return null;
    }
  }

  private WorkflowType getWorkflowType(String yamlFilePath, String name) {
    if (name.equals(WORKFLOW)) {
      return WorkflowType.ORCHESTRATION;
    } else if (name.equals(PIPELINE)) {
      return WorkflowType.PIPELINE;
    } else {
      Validator.notNullCheck("Execution type is invalid" + yamlFilePath, name);
      return null;
    }
  }
}

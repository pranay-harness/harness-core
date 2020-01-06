package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.validation.Validator.notNullCheck;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.Yaml;
import software.wings.beans.trigger.Trigger.Yaml.TriggerVariable;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.WorkflowYAMLHelper;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.TriggerService;
import software.wings.yaml.trigger.TriggerConditionYaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class TriggerYamlHandler extends BaseYamlHandler<Yaml, Trigger> {
  @Inject private TriggerService triggerService;
  private String WORKFLOW = "Workflow";
  private String PIPELINE = "Pipeline";
  @Inject YamlHelper yamlHelper;
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject EnvironmentService environmentService;
  @Inject private WorkflowYAMLHelper workflowYAMLHelper;
  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    Trigger trigger = yamlHelper.getTrigger(appId, yamlFilePath);
    if (trigger == null) {
      return;
    }

    triggerService.delete(optionalApplication.get().getUuid(), trigger.getUuid());
  }

  @Override
  public Yaml toYaml(Trigger bean, String appId) {
    TriggerConditionYamlHandler handler =
        yamlHandlerFactory.getYamlHandler(YamlType.TRIGGER_CONDITION, bean.getCondition().getConditionType().name());

    ArtifactSelectionYamlHandler artifactSelectionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.ARTIFACT_SELECTION);

    String executionType = getExecutionType(bean.getWorkflowType());
    String executionName = null;

    Workflow workflow = null;

    String workflowOrPipelineId = bean.getWorkflowId();
    if (bean.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      workflow = yamlHelper.getWorkflowFromId(appId, workflowOrPipelineId);
      executionName = workflow.getName();
    } else if (bean.getWorkflowType() == WorkflowType.PIPELINE) {
      executionName = yamlHelper.getPipelineName(appId, workflowOrPipelineId);
    }

    List<ArtifactSelection.Yaml> artifactSelectionList =
        bean.getArtifactSelections()
            .stream()
            .map(artifactSelection -> { return artifactSelectionYamlHandler.toYaml(artifactSelection, appId); })
            .collect(Collectors.toList());

    TriggerConditionYaml triggerConditionYaml = handler.toYaml(bean.getCondition(), appId);
    Yaml yaml = Yaml.builder()
                    .description(bean.getDescription())
                    .executionType(executionType)
                    .triggerCondition(Arrays.asList(triggerConditionYaml))
                    .executionName(executionName)
                    .workflowVariables(convertToTriggerYamlVariables(appId, bean.getWorkflowVariables(), workflow))
                    .artifactSelections(artifactSelectionList)
                    .harnessApiVersion(getHarnessApiVersion())
                    .build();

    updateYamlWithAdditionalInfo(bean, appId, yaml);
    return yaml;
  }

  @Override
  public Trigger upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Change change = changeContext.getChange();
    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
    notNullCheck("Could not locate app info in file path:" + change.getFilePath(), appId, USER);

    Trigger existingTrigger = yamlHelper.getTrigger(appId, change.getFilePath());

    Trigger trigger = toBean(appId, changeContext, changeSetContext);
    if (existingTrigger == null) {
      trigger = triggerService.save(trigger);
    } else {
      trigger = triggerService.update(trigger, false);
    }

    changeContext.setEntity(trigger);
    return trigger;
  }

  @Override
  public Class getYamlClass() {
    return Trigger.Yaml.class;
  }

  @Override
  public Trigger get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId);
    return yamlHelper.getTrigger(appId, yamlFilePath);
  }

  private Trigger toBean(String appId, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml yaml = changeContext.getYaml();
    Change change = changeContext.getChange();
    TriggerConditionYaml condition = yaml.getTriggerCondition().get(0);

    TriggerConditionYamlHandler handler =
        yamlHandlerFactory.getYamlHandler(YamlType.TRIGGER_CONDITION, condition.getType());

    ArtifactSelectionYamlHandler artifactSelectionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.ARTIFACT_SELECTION);

    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, condition);
    TriggerCondition triggerCondition = handler.upsertFromYaml(clonedContext.build(), changeSetContext);

    Trigger trigger = yamlHelper.getTrigger(appId, change.getFilePath());

    String uuid = null;
    if (trigger != null) {
      uuid = trigger.getUuid();
    }

    String triggerName = yamlHelper.extractEntityNameFromYamlPath(
        YamlType.TRIGGER.getPathExpression(), change.getFilePath(), PATH_DELIMITER);

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
                                 .name(triggerName)
                                 .workflowName(yaml.getExecutionName())
                                 .workflowType(workflowType)
                                 .condition(triggerCondition)
                                 .appId(appId)
                                 .uuid(uuid)
                                 .artifactSelections(artifactSelectionList)
                                 .build();

    if (workflowType == ORCHESTRATION) {
      Workflow workflow = yamlHelper.getWorkflowFromName(appId, yaml.getExecutionName());
      updatedTrigger.setWorkflowId(workflow.getUuid());
      updatedTrigger.setWorkflowVariables(
          convertToTriggerBeanVariables(change.getAccountId(), appId, workflow, yaml.getWorkflowVariables()));
    } else if (workflowType == WorkflowType.PIPELINE) {
      String pipelineId = yamlHelper.getPipelineId(appId, yaml.getExecutionName());
      updatedTrigger.setWorkflowId(pipelineId);
    }

    return updatedTrigger;
  }

  private String getExecutionType(WorkflowType workflowType) {
    if (workflowType == ORCHESTRATION) {
      return WORKFLOW;
    } else if (workflowType == WorkflowType.PIPELINE) {
      return PIPELINE;
    } else {
      notNullCheck("WorkflowType type is invalid", workflowType);
      return null;
    }
  }

  private WorkflowType getWorkflowType(String yamlFilePath, String name) {
    if (name.equals(WORKFLOW)) {
      return WorkflowType.ORCHESTRATION;
    } else if (name.equals(PIPELINE)) {
      return WorkflowType.PIPELINE;
    } else {
      notNullCheck("Execution type is invalid" + yamlFilePath, name);
      return null;
    }
  }

  private Map<String, String> convertToTriggerBeanVariables(
      String accountId, String appId, Workflow workflow, List<TriggerVariable> triggerVariable) {
    Map<String, String> workflowVariables = Maps.newLinkedHashMap();

    String envId = resolveEnvironmentId(triggerVariable, appId, workflowVariables, workflow);

    if (isNotEmpty(triggerVariable)) {
      triggerVariable.forEach((TriggerVariable variable) -> {
        String entityType = variable.getEntityType();
        String variableName = variable.getName();
        String variableValue = variable.getValue();
        String workflowVariableValueForBean =
            workflowYAMLHelper.getWorkflowVariableValueBean(accountId, envId, appId, entityType, variableValue);
        if (workflowVariableValueForBean != null) {
          workflowVariables.put(variableName, workflowVariableValueForBean);
        }
      });
    }

    return workflowVariables;
  }

  private List<TriggerVariable> convertToTriggerYamlVariables(
      String appId, Map<String, String> workflowVariables, Workflow workflow) {
    if (workflow == null || EmptyPredicate.isEmpty(workflowVariables)) {
      return null;
    }

    List<TriggerVariable> triggerVariables = new ArrayList<>();

    List<Variable> userVariables = workflow.getOrchestrationWorkflow().getUserVariables();
    if (isEmpty(userVariables)) {
      userVariables = new ArrayList<>();
    }

    Map<String, Variable> nameVariableMap =
        userVariables.stream().collect(Collectors.toMap(Variable::getName, Function.identity()));

    for (Map.Entry<String, String> entry : workflowVariables.entrySet()) {
      Variable variable = nameVariableMap.get(entry.getKey());
      if (variable != null) {
        EntityType entityType = variable.obtainEntityType();
        TriggerVariable workflowVariable = TriggerVariable.builder()
                                               .name(entry.getKey())
                                               .entityType(entityType != null ? entityType.name() : null)
                                               .build();
        String entryValue = entry.getValue();

        String variableValue = workflowYAMLHelper.getWorkflowVariableValueYaml(appId, entryValue, entityType);
        if (variableValue != null) {
          workflowVariable.setValue(variableValue);
          triggerVariables.add(workflowVariable);
        }
      }
    }

    return triggerVariables;
  }

  private String resolveEnvironmentId(
      List<TriggerVariable> variables, String appId, Map<String, String> workflowVariables, Workflow workflow) {
    String envId = null;

    if (workflow.checkEnvironmentTemplatized()) {
      logger.info("Workflow environment templatized. Workflow envId of appId {} and workflowId {} is {}", appId,
          workflow.getUuid(), workflow.getEnvId());

      if (isNotEmpty(variables)) {
        TriggerVariable workflowEnvVariable =
            variables.stream()
                .filter((TriggerVariable variable) -> ENVIRONMENT.name().equals(variable.getEntityType()))
                .findFirst()
                .orElse(null);

        if (workflowEnvVariable != null) {
          if (matchesVariablePattern(workflowEnvVariable.getValue())) {
            workflowVariables.put(workflowEnvVariable.getName(), workflowEnvVariable.getValue());
            logger.info("Environment parameterized in pipeline and the value is {}", workflowEnvVariable.getValue());
          } else {
            Environment environment =
                environmentService.getEnvironmentByName(appId, workflowEnvVariable.getValue(), false);
            notNullCheck("Environment [" + workflowEnvVariable.getValue() + "] does not exist", environment, USER);

            envId = environment.getUuid();
            workflowVariables.put(workflowEnvVariable.getName(), envId);
          }
        }
      }
    }

    if (isBlank(envId)) {
      envId = workflow.getEnvId();
    }

    return envId;
  }
}

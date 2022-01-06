/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.trigger.ManifestSelection.ManifestSelectionType;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ManifestSelection;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.Yaml;
import software.wings.beans.trigger.Trigger.Yaml.TriggerVariable;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.trigger.TriggerConditionType;
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

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
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
  @Inject private FeatureFlagService featureFlagService;
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

    triggerService.delete(
        optionalApplication.get().getUuid(), trigger.getUuid(), changeContext.getChange().isSyncFromGit());
  }

  @Override
  public Yaml toYaml(Trigger bean, String appId) {
    TriggerConditionYamlHandler handler =
        yamlHandlerFactory.getYamlHandler(YamlType.TRIGGER_CONDITION, bean.getCondition().getConditionType().name());

    ArtifactSelectionYamlHandler artifactSelectionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.ARTIFACT_SELECTION);

    ManifestSelectionYamlHandler manifestSelectionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.MANIFEST_SELECTION);

    String executionType = getExecutionType(bean.getWorkflowType());
    String executionName = null;

    List<TriggerVariable> triggerVariablesYaml = null;
    String workflowOrPipelineId = bean.getWorkflowId();
    if (bean.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      Workflow workflow = yamlHelper.getWorkflowFromId(appId, workflowOrPipelineId);
      executionName = workflow.getName();
      if (workflow.getOrchestrationWorkflow() != null) {
        triggerVariablesYaml = convertToTriggerYamlVariables(
            appId, bean.getWorkflowVariables(), workflow.getOrchestrationWorkflow().getUserVariables());
      }
    } else if (bean.getWorkflowType() == WorkflowType.PIPELINE) {
      Pipeline pipeline = yamlHelper.getPipelineFromId(appId, workflowOrPipelineId);
      executionName = pipeline.getName();
      triggerVariablesYaml =
          convertToTriggerYamlVariables(appId, bean.getWorkflowVariables(), pipeline.getPipelineVariables());
    }

    List<ArtifactSelection.Yaml> artifactSelectionList =
        bean.getArtifactSelections()
            .stream()
            .map(artifactSelection -> { return artifactSelectionYamlHandler.toYaml(artifactSelection, appId); })
            .collect(Collectors.toList());

    List<ManifestSelection.Yaml> manifestSelectionList =
        featureFlagService.isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, bean.getAccountId())
        ? bean.getManifestSelections()
              .stream()
              .map(artifactSelection -> manifestSelectionYamlHandler.toYaml(artifactSelection, appId))
              .collect(Collectors.toList())
        : null;

    if (isNotEmpty(artifactSelectionList)) {
      for (ArtifactSelection.Yaml artifactSelectionYAML : artifactSelectionList) {
        if (bean.getWorkflowType() == WorkflowType.ORCHESTRATION) {
          artifactSelectionYAML.setPipelineName(null);
        } else if (bean.getWorkflowType() == WorkflowType.PIPELINE) {
          artifactSelectionYAML.setWorkflowName(null);
        }
      }
    }

    TriggerConditionYaml triggerConditionYaml = handler.toYaml(bean.getCondition(), appId);
    Yaml yaml = Yaml.builder()
                    .description(bean.getDescription())
                    .executionType(executionType)
                    .triggerCondition(Arrays.asList(triggerConditionYaml))
                    .executionName(executionName)
                    .workflowVariables(triggerVariablesYaml)
                    .artifactSelections(artifactSelectionList)
                    .manifestSelections(manifestSelectionList)
                    .harnessApiVersion(getHarnessApiVersion())
                    .continueWithDefaultValues(bean.isContinueWithDefaultValues())
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
    trigger.setSyncFromGit(changeContext.getChange().isSyncFromGit());

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

    ManifestSelectionYamlHandler manifestSelectionYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.MANIFEST_SELECTION);

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
    WorkflowType workflowType = getWorkflowType(changeContext.getChange().getFilePath(), yaml.getExecutionType());

    yaml.getArtifactSelections().forEach(artifactSelectionYaml -> {
      if (workflowType == ORCHESTRATION) {
        artifactSelectionYaml.setPipelineName(null);
      } else if (workflowType == WorkflowType.PIPELINE) {
        artifactSelectionYaml.setWorkflowName(null);
      }
      ChangeContext.Builder artifactClonedContext = cloneFileChangeContext(changeContext, artifactSelectionYaml);
      ArtifactSelection artifactSelection =
          artifactSelectionYamlHandler.upsertFromYaml(artifactClonedContext.build(), changeSetContext);
      artifactSelectionList.add(artifactSelection);
    });

    List<ManifestSelection> manifestSelections = new ArrayList<>();
    boolean helmArtifactEnabled =
        trigger == null || featureFlagService.isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, trigger.getAccountId());
    if (helmArtifactEnabled && isNotEmpty(yaml.getManifestSelections())) {
      yaml.getManifestSelections().forEach(manifestSelectionYaml -> {
        if (workflowType == ORCHESTRATION) {
          manifestSelectionYaml.setPipelineName(null);
        } else if (workflowType == WorkflowType.PIPELINE) {
          manifestSelectionYaml.setWorkflowName(null);
        }
        ChangeContext.Builder manifestClonedContext = cloneFileChangeContext(changeContext, manifestSelectionYaml);
        ManifestSelection manifestSelection =
            manifestSelectionYamlHandler.upsertFromYaml(manifestClonedContext.build(), changeSetContext);
        validateManifestSelectionType(triggerCondition, manifestSelection);
        manifestSelections.add(manifestSelection);
      });
    }

    Trigger updatedTrigger = Trigger.builder()
                                 .description(yaml.getDescription())
                                 .name(triggerName)
                                 .workflowName(yaml.getExecutionName())
                                 .workflowType(workflowType)
                                 .condition(triggerCondition)
                                 .appId(appId)
                                 .uuid(uuid)
                                 .artifactSelections(artifactSelectionList)
                                 .manifestSelections(manifestSelections)
                                 .continueWithDefaultValues(yaml.isContinueWithDefaultValues())
                                 .build();

    if (workflowType == ORCHESTRATION) {
      Workflow workflow = yamlHelper.getWorkflowFromName(appId, yaml.getExecutionName());
      updatedTrigger.setWorkflowId(workflow.getUuid());
      updatedTrigger.setWorkflowVariables(
          convertToTriggerBeanVariables(change.getAccountId(), appId, workflow, yaml.getWorkflowVariables()));
    } else if (workflowType == WorkflowType.PIPELINE) {
      Pipeline pipeline = yamlHelper.getPipelineFromName(appId, yaml.getExecutionName());
      updatedTrigger.setWorkflowId(pipeline.getUuid());
      updatedTrigger.setWorkflowVariables(
          convertToTriggerBeanVariablesPipeline(change.getAccountId(), appId, pipeline, yaml.getWorkflowVariables()));
    }

    return updatedTrigger;
  }

  private void validateManifestSelectionType(TriggerCondition triggerCondition, ManifestSelection manifestSelection) {
    if (manifestSelection.getType() == ManifestSelectionType.PIPELINE_SOURCE
        && triggerCondition.getConditionType() != TriggerConditionType.PIPELINE_COMPLETION) {
      throw new InvalidRequestException(
          String.format("Trigger condition %s doesn't support PIPELINE_SOURCE manifest selection",
              triggerCondition.getConditionType()));
    }
    if (manifestSelection.getType() == ManifestSelectionType.WEBHOOK_VARIABLE
        && triggerCondition.getConditionType() != TriggerConditionType.WEBHOOK) {
      throw new InvalidRequestException(
          String.format("Trigger condition %s doesn't support WEBHOOK_VARIABLE manifest selection",
              triggerCondition.getConditionType()));
    }
  }

  private Map<String, String> convertToTriggerBeanVariablesPipeline(
      String accountId, String appId, Pipeline pipeline, List<TriggerVariable> triggerVariables) {
    Map<String, String> workflowVariables = Maps.newLinkedHashMap();

    if (isEmpty(triggerVariables)) {
      return workflowVariables;
    }

    List<Variable> variables = pipeline.getPipelineVariables();
    if (isEmpty(variables)) {
      return workflowVariables;
    }
    for (TriggerVariable variable : triggerVariables) {
      String entityType = variable.getEntityType();
      String variableName = variable.getName();
      String variableValue = variable.getValue();
      Variable variableOrg = variables.stream().filter(t -> t.getName().equals(variableName)).findFirst().orElse(null);
      if (variableOrg == null || (isBlank(variableValue) && TRUE.equals(variableOrg.getRuntimeInput()))) {
        continue;
      }
      String valueForBean = null;
      if (isNotEmpty(entityType)
          && (entityType.equals(INFRASTRUCTURE_DEFINITION.name())
              || entityType.equals(INFRASTRUCTURE_MAPPING.name()))) {
        valueForBean = getValueForInfraVariable(triggerVariables, variable, pipeline, accountId, appId, variableOrg);
      } else {
        valueForBean = workflowYAMLHelper.getWorkflowVariableValueBean(
            accountId, null, appId, entityType, variableValue, variableOrg);
      }
      if (valueForBean != null) {
        workflowVariables.put(variableName, valueForBean);
      }
    }

    return workflowVariables;
  }

  private String getValueForInfraVariable(List<TriggerVariable> triggerVariables, TriggerVariable variable,
      Pipeline pipeline, String accountId, String appId, Variable infraVarInPipeline) {
    List<Variable> pipelineVariables = pipeline.getPipelineVariables();
    String envId = null;
    if (infraVarInPipeline != null) {
      Map<String, Object> metadata = infraVarInPipeline.getMetadata();
      // get envId from Metadata
      if (isNotEmpty(metadata) && metadata.get(Variable.ENV_ID) != null) {
        envId = (String) metadata.get(Variable.ENV_ID);
        return workflowYAMLHelper.getWorkflowVariableValueBean(
            accountId, envId, appId, variable.getEntityType(), variable.getValue(), infraVarInPipeline);
      }
    }
    // look for an env variable in pipeline having infra var as related field and get its value from trigger variables.
    for (Variable var : pipelineVariables) {
      Map<String, Object> metadata = var.getMetadata();
      if (isNotEmpty(metadata) && ENVIRONMENT == var.obtainEntityType()
          && Arrays.asList(var.obtainRelatedField().split(",")).contains(variable.getName())) {
        TriggerVariable envVarInTrigger =
            triggerVariables.stream().filter(t -> t.getName().equals(var.getName())).findFirst().orElse(null);
        if (envVarInTrigger != null) {
          if (TRUE.equals(var.getRuntimeInput()) && isBlank(envVarInTrigger.getValue())) {
            return null;
          }

          if (matchesVariablePattern(envVarInTrigger.getValue())) {
            log.info("Environment parameterized in Trigger and the value is {}", envVarInTrigger.getValue());
            if (!matchesVariablePattern(variable.getValue())) {
              throw new InvalidRequestException(
                  "Infrastructure Definition should be templatised when environment value is templatised: Invalid Infra value: "
                      + variable.getValue(),
                  USER);
            }
            return variable.getValue();
          } else {
            Environment environment = environmentService.getEnvironmentByName(appId, envVarInTrigger.getValue(), false);
            notNullCheck("Environment [" + envVarInTrigger.getValue() + "] does not exist", environment, USER);

            envId = environment.getUuid();
          }
          return workflowYAMLHelper.getWorkflowVariableValueBean(
              accountId, envId, appId, variable.getEntityType(), variable.getValue(), infraVarInPipeline);
        }
      }
    }
    return variable.getValue();
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

    String envId = resolveEnvironmentId(triggerVariable, appId, workflow);

    notNullCheck("Workflow does not exist " + workflow.getName(), workflow.getOrchestrationWorkflow());
    List<Variable> variables = workflow.getOrchestrationWorkflow().getUserVariables();
    if (isEmpty(variables)) {
      return workflowVariables;
    }
    if (isNotEmpty(triggerVariable)) {
      triggerVariable.forEach((TriggerVariable variable) -> {
        String entityType = variable.getEntityType();
        String variableName = variable.getName();
        Variable variableOrg =
            variables.stream().filter(t -> t.getName().equals(variableName)).findFirst().orElse(null);
        String variableValue = variable.getValue();
        String workflowVariableValueForBean = workflowYAMLHelper.getWorkflowVariableValueBean(
            accountId, envId, appId, entityType, variableValue, variableOrg);
        if (workflowVariableValueForBean != null) {
          workflowVariables.put(variableName, workflowVariableValueForBean);
        }
      });
    }

    return workflowVariables;
  }

  private List<TriggerVariable> convertToTriggerYamlVariables(
      String appId, Map<String, String> workflowVariables, List<Variable> userVariables) {
    if (EmptyPredicate.isEmpty(workflowVariables)) {
      return null;
    }
    List<TriggerVariable> triggerVariables = new ArrayList<>();

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
        triggerVariables.add(workflowVariable);

        if (isNotEmpty(entryValue)) {
          String variableValue = workflowYAMLHelper.getWorkflowVariableValueYaml(appId, entryValue, entityType);
          if (variableValue != null) {
            workflowVariable.setValue(variableValue);
          }
        }
      }
    }

    return triggerVariables;
  }

  private String resolveEnvironmentId(List<TriggerVariable> variables, String appId, Workflow workflow) {
    String envId = null;

    if (workflow.checkEnvironmentTemplatized()) {
      log.info("Workflow environment templatized. Workflow envId of appId {} and workflowId {} is {}", appId,
          workflow.getUuid(), workflow.getEnvId());

      if (isNotEmpty(variables)) {
        TriggerVariable workflowEnvVariable =
            variables.stream()
                .filter((TriggerVariable variable) -> ENVIRONMENT.name().equals(variable.getEntityType()))
                .findFirst()
                .orElse(null);

        if (workflowEnvVariable != null) {
          if (matchesVariablePattern(workflowEnvVariable.getValue())) {
            log.info("Environment parameterized in Trigger and the value is {}", workflowEnvVariable.getValue());
          } else {
            Environment environment =
                environmentService.getEnvironmentByName(appId, workflowEnvVariable.getValue(), false);
            notNullCheck("Environment [" + workflowEnvVariable.getValue() + "] does not exist", environment, USER);

            envId = environment.getUuid();
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

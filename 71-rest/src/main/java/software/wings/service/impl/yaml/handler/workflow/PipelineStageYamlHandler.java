package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.EntityType.CF_AWS_CONFIG_ID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.HELM_GIT_CONFIG_ID;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.PipelineStage.Yaml;
import static software.wings.expression.ManagerExpressionEvaluator.matchesVariablePattern;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStage.WorkflowVariable;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author rktummala on 11/2/17
 */
@Singleton
public class PipelineStageYamlHandler extends BaseYamlHandler<Yaml, PipelineStage> {
  private static final Logger logger = LoggerFactory.getLogger(PipelineStageYamlHandler.class);
  @Inject YamlHelper yamlHelper;
  @Inject WorkflowService workflowService;
  @Inject EnvironmentService environmentService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject InfrastructureMappingService infrastructureMappingService;
  @Inject SettingsService settingsService;

  private PipelineStage toBean(ChangeContext<Yaml> context) {
    Yaml yaml = context.getYaml();
    Change change = context.getChange();

    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
    notNullCheck("Could not retrieve valid app from path: " + change.getFilePath(), appId, USER);

    PipelineStage stage = PipelineStage.builder().build();
    stage.setName(yaml.getName());
    stage.setParallel(yaml.isParallel());

    String stageElementId = null;
    Pipeline previous = yamlHelper.getPipeline(change.getAccountId(), change.getFilePath());
    if (previous != null) {
      Map<String, String> entityIdMap = context.getEntityIdMap();
      if (entityIdMap != null) {
        stageElementId = entityIdMap.remove(yaml.getName());
      }
    }

    Map<String, Object> properties = Maps.newHashMap();
    Map<String, String> workflowVariables = Maps.newLinkedHashMap();
    Workflow workflow;
    if (!yaml.getType().equals(StateType.APPROVAL.name())) {
      workflow = workflowService.readWorkflowByName(appId, yaml.getWorkflowName());
      notNullCheck("Invalid workflow with the given name:" + yaml.getWorkflowName(), workflow, USER);
      properties.put("workflowId", workflow.getUuid());

      String envId = resolveEnvironmentId(yaml, appId, properties, workflowVariables, workflow);

      if (isNotEmpty(yaml.getWorkflowVariables())) {
        yaml.getWorkflowVariables().forEach((PipelineStage.WorkflowVariable variable) -> {
          String entityType = variable.getEntityType();
          String variableName = variable.getName();
          String variableValue = variable.getValue();
          if (entityType != null) {
            if (ENVIRONMENT.name().equals(entityType)) {
              // This is already taken care in resolveEnvironmentId method
              return;
            } else if (SERVICE.name().equals(entityType)) {
              if (matchesVariablePattern(variableValue)) {
                workflowVariables.put(variableName, variableValue);
                return;
              }
              Service service = serviceResourceService.getServiceByName(appId, variableValue, false);
              if (service != null) {
                workflowVariables.put(variableName, service.getUuid());
              } else {
                notNullCheck("Service [" + variableValue + "] does not exist", service, USER);
              }
            } else if (INFRASTRUCTURE_MAPPING.name().equals(entityType)) {
              if (matchesVariablePattern(variableValue)) {
                workflowVariables.put(variableName, variableValue);
                return;
              }
              InfrastructureMapping infrastructureMapping =
                  infrastructureMappingService.getInfraMappingByName(appId, envId, variableValue);
              if (infrastructureMapping != null) {
                workflowVariables.put(variableName, infrastructureMapping.getUuid());
              } else {
                notNullCheck("Service Infrastructure [" + variableValue + "] does not exist for the environment",
                    infrastructureMapping, USER);
              }
            } else if (CF_AWS_CONFIG_ID.name().equals(entityType)) {
              if (matchesVariablePattern(variableValue)) {
                workflowVariables.put(variableName, variableValue);
                return;
              }
              SettingAttribute settingAttribute = settingsService.fetchSettingAttributeByName(
                  change.getAccountId(), variableValue, SettingVariableTypes.AWS);
              if (settingAttribute != null) {
                workflowVariables.put(variableName, settingAttribute.getUuid());
              } else {
                notNullCheck(
                    "Aws Cloud Provider [" + variableValue + "] associated to the Cloud Formation State does not exist",
                    settingAttribute, USER);
              }
            } else if (HELM_GIT_CONFIG_ID.name().equals(entityType)) {
              if (matchesVariablePattern(variableValue)) {
                workflowVariables.put(variableName, variableValue);
                return;
              }
              SettingAttribute settingAttribute = settingsService.fetchSettingAttributeByName(
                  change.getAccountId(), variableValue, SettingVariableTypes.GIT);
              if (settingAttribute != null) {
                workflowVariables.put(variableName, settingAttribute.getUuid());
              } else {
                notNullCheck("Git Connector [" + variableValue + "] associated to the Helm State does not exist",
                    settingAttribute, USER);
              }
            } else {
              workflowVariables.put(variableName, variableValue);
            }
          } else {
            // Non entity variables
            workflowVariables.put(variableName, variableValue);
          }
        });
      }
      logger.info("The pipeline env stage properties for appId {} wrokflowId {} are {}", appId, workflow.getUuid(),
          String.valueOf(properties));
    } else if (StateType.APPROVAL.name().equals(yaml.getType())) {
      Map<String, Object> yamlProperties = yaml.getProperties();

      if (yamlProperties != null) {
        yamlProperties.forEach((name, value) -> properties.put(name, value));
      }
    }

    PipelineStageElement pipelineStageElement = PipelineStageElement.builder()
                                                    .uuid(stageElementId)
                                                    .name(yaml.getName())
                                                    .type(yaml.getType())
                                                    .properties(properties)
                                                    .workflowVariables(workflowVariables)
                                                    .build();

    stage.setPipelineStageElements(Lists.newArrayList(pipelineStageElement));

    return stage;
  }

  private String resolveEnvironmentId(Yaml yaml, String appId, Map<String, Object> properties,
      Map<String, String> workflowVariables, Workflow workflow) {
    String envId = null;

    if (workflow.checkEnvironmentTemplatized()) {
      logger.info("Workflow environment templatized. Workflow envId of appId {} and workflowId {} is {}", appId,
          workflow.getUuid(), workflow.getEnvId());

      if (isNotEmpty(yaml.getWorkflowVariables())) {
        WorkflowVariable workflowEnvVariable =
            yaml.getWorkflowVariables()
                .stream()
                .filter((WorkflowVariable variable) -> ENVIRONMENT.name().equals(variable.getEntityType()))
                .findFirst()
                .orElse(null);

        if (workflowEnvVariable != null) {
          if (matchesVariablePattern(workflowEnvVariable.getValue())) {
            workflowVariables.put(workflowEnvVariable.getName(), workflowEnvVariable.getValue());
            logger.info("Environment parameterized in pipeline and the value is {}", workflowEnvVariable.getValue());
            properties.put("envId", workflowEnvVariable.getValue());
          } else {
            Environment environment =
                environmentService.getEnvironmentByName(appId, workflowEnvVariable.getValue(), false);
            notNullCheck("Environment [" + workflowEnvVariable.getValue() + "] does not exist", environment, USER);

            envId = environment.getUuid();
            workflowVariables.put(workflowEnvVariable.getName(), envId);
            properties.put("envId", envId);
          }
        }
      }
    }

    if (isBlank(envId)) {
      envId = workflow.getEnvId();
      properties.put("envId", envId);
    }

    return envId;
  }

  @Override
  public Yaml toYaml(PipelineStage bean, String appId) {
    PipelineStageElement stageElement = bean.getPipelineStageElements().get(0);
    notNullCheck("Pipeline stage element is null", stageElement, USER);

    Map<String, Object> outputProperties = new HashMap<>();
    String workflowName = null;

    List<PipelineStage.WorkflowVariable> pipelineStageVariables = new ArrayList<>();
    if (!StateType.APPROVAL.name().equals(stageElement.getType())) {
      Map<String, Object> properties = stageElement.getProperties();
      notNullCheck("Pipeline stage element is null", properties, USER);

      String workflowId = (String) properties.get("workflowId");
      notNullCheck("Workflow id is null in stage properties", workflowId, USER);

      Workflow workflow = workflowService.readWorkflow(appId, workflowId);
      notNullCheck("Workflow id is null in stage properties", workflowId, USER);

      workflowName = workflow.getName();

      Map<String, String> workflowVariables = stageElement.getWorkflowVariables();
      notNullCheck("Pipeline stage element is null", workflowVariables, USER);

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
          PipelineStage.WorkflowVariable workflowVariable =
              PipelineStage.WorkflowVariable.builder()
                  .name(entry.getKey())
                  .entityType(entityType != null ? entityType.name() : null)
                  .build();
          String entryValue = entry.getValue();
          if (matchesVariablePattern(entryValue)) {
            workflowVariable.setValue(entryValue);
            pipelineStageVariables.add(workflowVariable);
            continue;
          }
          if (ENVIRONMENT.equals(entityType)) {
            Environment environment = environmentService.get(appId, entryValue, false);
            if (environment != null) {
              workflowVariable.setValue(environment.getName());
              pipelineStageVariables.add(workflowVariable);
            }
          } else if (SERVICE.equals(entityType)) {
            Service service = serviceResourceService.get(appId, entryValue, false);
            if (service != null) {
              workflowVariable.setValue(service.getName());
              pipelineStageVariables.add(workflowVariable);
            }
          } else if (INFRASTRUCTURE_MAPPING.equals(entityType)) {
            InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, entryValue);
            if (infrastructureMapping != null) {
              workflowVariable.setValue(infrastructureMapping.getName());
              pipelineStageVariables.add(workflowVariable);
            }
          } else if (CF_AWS_CONFIG_ID.equals(entityType)) {
            SettingAttribute settingAttribute = settingsService.get(entryValue);
            if (settingAttribute != null) {
              workflowVariable.setValue(settingAttribute.getName());
              pipelineStageVariables.add(workflowVariable);
            }
          } else if (HELM_GIT_CONFIG_ID.equals(entityType)) {
            SettingAttribute settingAttribute = settingsService.get(entryValue);
            if (settingAttribute != null) {
              workflowVariable.setValue(settingAttribute.getName());
              pipelineStageVariables.add(workflowVariable);
            }
          } else {
            workflowVariable.setValue(entryValue);
            pipelineStageVariables.add(workflowVariable);
          }
        }
      }
    } else if (StateType.APPROVAL.name().equals(stageElement.getType())) {
      Map<String, Object> properties = stageElement.getProperties();

      if (properties != null) {
        properties.forEach((name, value) -> {
          if (!shouldBeIgnored(name)) {
            outputProperties.put(name, value);
          }
        });
      }
    }

    return Yaml.builder()
        .name(stageElement.getName())
        .parallel(bean.isParallel())
        .type(stageElement.getType())
        .workflowName(workflowName)
        .workflowVariables(pipelineStageVariables)
        .properties(outputProperties.isEmpty() ? null : outputProperties)
        .build();
  }

  private boolean shouldBeIgnored(String name) {
    if (isEmpty(name)) {
      return true;
    }

    switch (name) {
      case "id":
      case "parentId":
      case "subWorkflowId":
      case "groupName":
        return true;
      default:
        return false;
    }
  }

  @Override
  public PipelineStage upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public PipelineStage get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}

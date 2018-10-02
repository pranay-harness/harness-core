package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.PipelineStage.Yaml;
import static software.wings.expression.ExpressionEvaluator.matchesVariablePattern;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author rktummala on 11/2/17
 */
@Singleton
public class PipelineStageYamlHandler extends BaseYamlHandler<Yaml, PipelineStage> {
  @Inject YamlHelper yamlHelper;
  @Inject WorkflowService workflowService;
  @Inject EnvironmentService environmentService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject InfrastructureMappingService infrastructureMappingService;

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

      properties.put("envId", workflow.getEnvId());
      properties.put("workflowId", workflow.getUuid());

      if (isNotEmpty(yaml.getWorkflowVariables())) {
        final String[] envId = {workflow.getEnvId()};
        yaml.getWorkflowVariables().forEach((PipelineStage.WorkflowVariable variable) -> {
          String entityType = variable.getEntityType();
          String variableName = variable.getName();
          String variableValue = variable.getValue();
          if (entityType != null) {
            if (matchesVariablePattern(variableValue)) {
              workflowVariables.put(variableName, variableValue);
              return;
            }
            if (ENVIRONMENT.name().equals(entityType)) {
              Environment environment = environmentService.getEnvironmentByName(appId, variableValue, false);
              if (environment != null) {
                envId[0] = environment.getUuid();
                workflowVariables.put(variableName, envId[0]);
                properties.put("envId", envId[0]);
              } else {
                notNullCheck("Environment [" + variableValue + "] does not exist", environment, USER);
              }
            } else if (SERVICE.name().equals(entityType)) {
              Service service = serviceResourceService.getServiceByName(appId, variableValue, false);
              if (service != null) {
                workflowVariables.put(variableName, service.getUuid());
              } else {
                notNullCheck("Service [" + variableValue + "] does not exist", service, USER);
              }
            } else if (INFRASTRUCTURE_MAPPING.name().equals(entityType)) {
              InfrastructureMapping infrastructureMapping =
                  infrastructureMappingService.getInfraMappingByName(appId, envId[0], variableValue);
              if (infrastructureMapping != null) {
                workflowVariables.put(variableName, infrastructureMapping.getUuid());
              } else {
                notNullCheck("Service Infrastructure [" + variableValue + "] does not exist for the environment",
                    infrastructureMapping, USER);
              }
            } else {
              // TODO: Other entity variables verification states
              workflowVariables.put(variableName, variableValue);
            }
          } else {
            // Non entity variables
            workflowVariables.put(variableName, variableValue);
          }
        });
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

  @Override
  public Yaml toYaml(PipelineStage bean, String appId) {
    if (isEmpty(bean.getPipelineStageElements())) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "No stage elements present in the given phase stage: " + bean.getName());
    }

    PipelineStageElement stageElement = bean.getPipelineStageElements().get(0);
    notNullCheck("Pipeline stage element is null", stageElement, USER);

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
          EntityType entityType = variable.getEntityType();
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
          } else {
            workflowVariable.setValue(entryValue);
            pipelineStageVariables.add(workflowVariable);
          }
        }
      }
    }

    return Yaml.builder()
        .name(stageElement.getName())
        .parallel(bean.isParallel())
        .type(stageElement.getType())
        .workflowName(workflowName)
        .workflowVariables(pipelineStageVariables)
        .build();
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

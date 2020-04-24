package software.wings.graphql.datafetcher.execution;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.service.impl.workflow.WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.CreatedByType;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.WorkflowType;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.mutation.execution.input.QLServiceInput;
import software.wings.graphql.schema.mutation.execution.input.QLStartExecutionInput;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.graphql.schema.mutation.execution.input.QLVariableValue;
import software.wings.graphql.schema.mutation.execution.input.QLVariableValueType;
import software.wings.graphql.schema.query.QLTriggerQueryParameters.QLTriggerQueryParametersKeys;
import software.wings.graphql.schema.type.QLApiKey;
import software.wings.graphql.schema.type.QLCause;
import software.wings.graphql.schema.type.QLExecuteOptions;
import software.wings.graphql.schema.type.QLExecutedByAPIKey;
import software.wings.graphql.schema.type.QLExecutedByTrigger;
import software.wings.graphql.schema.type.QLExecutedByUser;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.graphql.schema.type.QLPipelineExecution.QLPipelineExecutionBuilder;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTag;
import software.wings.infra.InfrastructureDefinition;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.AppLogContext;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
@Singleton
@Slf4j
public class PipelineExecutionController {
  @Inject AuthHandler authHandler;
  @Inject PipelineService pipelineService;
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject EnvironmentService environmentService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject ExecutionController executionController;

  public void populatePipelineExecution(
      @NotNull WorkflowExecution workflowExecution, QLPipelineExecutionBuilder builder) {
    QLCause cause;
    if (workflowExecution.getDeploymentTriggerId() != null) {
      cause = QLExecutedByTrigger.builder()
                  .context(ImmutableMap.<String, Object>builder()
                               .put(QLTriggerQueryParametersKeys.triggerId, workflowExecution.getDeploymentTriggerId())
                               .build())
                  .build();
    } else if (CreatedByType.API_KEY == workflowExecution.getCreatedByType()) {
      EmbeddedUser apiKey = workflowExecution.getCreatedBy();
      cause = QLExecutedByAPIKey.builder()
                  .apiKey(QLApiKey.builder().name(apiKey.getName()).id(apiKey.getUuid()).build())
                  .using(QLExecuteOptions.GRAPHQL_API)
                  .build();
    } else {
      cause = QLExecutedByUser.builder()
                  .user(UserController.populateUser(workflowExecution.getTriggeredBy()))
                  .using(QLExecuteOptions.WEB_UI)
                  .build();
    }

    List<QLDeploymentTag> tags = new ArrayList<>();
    if (isNotEmpty(workflowExecution.getTags())) {
      tags = workflowExecution.getTags()
                 .stream()
                 .map(tag -> QLDeploymentTag.builder().name(tag.getName()).value(tag.getValue()).build())
                 .collect(Collectors.toList());
    }

    builder.id(workflowExecution.getUuid())
        .appId(workflowExecution.getAppId())
        .createdAt(workflowExecution.getCreatedAt())
        .startedAt(workflowExecution.getStartTs())
        .endedAt(workflowExecution.getEndTs())
        .status(ExecutionController.convertStatus(workflowExecution.getStatus()))
        .cause(cause)
        .notes(workflowExecution.getExecutionArgs() == null ? null : workflowExecution.getExecutionArgs().getNotes())
        .tags(tags)
        .build();
  }

  public QLPipelineExecution startPipelineExecution(QLStartExecutionInput triggerExecutionInput,
      MutationContext mutationContext, List<PermissionAttribute> permissionAttributes) {
    String appId = triggerExecutionInput.getApplicationId();
    try (AutoLogContext ignore = new AppLogContext(appId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      String pipelineId = triggerExecutionInput.getEntityId();
      authorizePipelineExecution(triggerExecutionInput, permissionAttributes, appId);
      Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, true);
      notNullCheck("Pipeline " + pipelineId + " doesn't exist in the specified application " + appId, pipeline, USER);
      String envId = resolveEnvId(pipeline, triggerExecutionInput.getVariableInputs());

      Map<String, String> variableValues =
          validateAndResolvePipelineVariables(pipeline, triggerExecutionInput.getVariableInputs(), envId);
      List<Artifact> artifacts =
          validateAndGetArtifactsFromServiceInputs(variableValues, triggerExecutionInput.getServiceInputs(), pipeline);
      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setWorkflowType(WorkflowType.PIPELINE);
      executionArgs.setPipelineId(triggerExecutionInput.getEntityId());
      executionController.populateExecutionArgs(
          variableValues, artifacts, triggerExecutionInput, mutationContext, executionArgs);
      WorkflowExecution workflowExecution =
          workflowExecutionService.triggerEnvExecution(appId, envId, executionArgs, null);
      final QLPipelineExecutionBuilder builder = QLPipelineExecution.builder();
      populatePipelineExecution(workflowExecution, builder);
      return builder.build();
    }
  }

  private String resolveEnvId(Pipeline pipeline, List<QLVariableInput> variableInputs) {
    List<Variable> pipelineVariables = pipeline.getPipelineVariables();
    String templatizedEnvName = getTemplatizedEnvVariableName(pipelineVariables);
    if (templatizedEnvName == null) {
      logger.info("Environment is Not templatized in pipeline {} ", pipeline.getUuid());
      return null;
    }
    if (!isEmpty(variableInputs)) {
      QLVariableInput envVarInput =
          variableInputs.stream().filter(t -> templatizedEnvName.equals(t.getName())).findFirst().orElse(null);
      if (envVarInput != null) {
        QLVariableValue envVarValue = envVarInput.getVariableValue();
        switch (envVarValue.getType()) {
          case ID:
            return envVarValue.getValue();
          case NAME:
            String envName = envVarValue.getValue();
            Environment environmentFromName = environmentService.getEnvironmentByName(pipeline.getAppId(), envName);
            notNullCheck("Environment [" + envName + "] doesn't exist in specified application " + pipeline.getAppId(),
                environmentFromName, USER);
            return environmentFromName.getUuid();
          default:
            throw new UnsupportedOperationException("Value Type " + envVarValue.getType() + " Not supported");
        }
      }
    }
    throw new InvalidRequestException(
        "Pipeline [" + pipeline.getName() + "] has environment parameterized. However, the value not supplied", USER);
  }

  private List<Artifact> validateAndGetArtifactsFromServiceInputs(
      Map<String, String> variableValues, List<QLServiceInput> serviceInputs, Pipeline pipeline) {
    /* Fetch the deployment data to find out the required entity types */
    DeploymentMetadata deploymentMetadata = pipelineService.fetchDeploymentMetadata(
        pipeline.getAppId(), pipeline.getUuid(), variableValues, null, null, false, null);

    // Fetch the service
    List<String> artifactNeededServiceIds =
        deploymentMetadata == null ? new ArrayList<>() : deploymentMetadata.getArtifactRequiredServiceIds();
    if (isEmpty(artifactNeededServiceIds)) {
      return new ArrayList<>();
    }

    List<Artifact> artifacts = new ArrayList<>();
    executionController.getArtifactsFromServiceInputs(
        serviceInputs, pipeline.getAppId(), artifactNeededServiceIds, artifacts);
    return artifacts;
  }

  private Map<String, String> validateAndResolvePipelineVariables(
      Pipeline pipeline, List<QLVariableInput> variableInputs, String envId) {
    List<Variable> pipelineVariables = pipeline.getPipelineVariables();
    if (isEmpty(pipelineVariables)) {
      return new HashMap<>();
    }

    validateRequiredVarsPresent(variableInputs, pipelineVariables);

    Map<String, String> pipelineVariableValues = new HashMap<>();
    for (QLVariableInput variableInput : variableInputs) {
      Variable variableInPipeline =
          pipelineVariables.stream().filter(t -> t.getName().equals(variableInput.getName())).findFirst().orElse(null);
      if (variableInPipeline != null) {
        QLVariableValue variableValue = variableInput.getVariableValue();
        QLVariableValueType type = variableValue.getType();
        switch (type) {
          case ID:
            pipelineVariableValues.put(variableInput.getName(), variableValue.getValue());
            break;
          case NAME:
            String value = resolveVariableValue(
                pipeline.getAppId(), variableValue.getValue(), variableInPipeline, pipeline, envId);
            pipelineVariableValues.put(variableInput.getName(), value);
            break;
          default:
            throw new UnsupportedOperationException("Value Type " + type + " Not supported");
        }
      }
    }

    return pipelineVariableValues;
  }

  private String resolveVariableValue(String appId, String value, Variable variable, Pipeline pipeline, String envId) {
    EntityType entityType = variable.obtainEntityType();
    if (entityType != null) {
      switch (entityType) {
        case ENVIRONMENT:
          notNullCheck("Value for environment variable not supplied", envId, USER);
          return envId;
        case SERVICE:
          Service serviceFromName = serviceResourceService.getServiceByName(appId, value);
          notNullCheck(
              "Service [" + value + "] doesn't exist in specified application " + appId, serviceFromName, USER);
          return serviceFromName.getUuid();
        case INFRASTRUCTURE_DEFINITION:
          if (envId != null) {
            InfrastructureDefinition infrastructureDefinition =
                infrastructureDefinitionService.getInfraDefByName(appId, envId, value);
            notNullCheck("Infrastructure Definition  [" + value
                    + "] doesn't exist in specified application and environment " + appId,
                infrastructureDefinition, USER);
            return infrastructureDefinition.getUuid();
          }
          String envIdFromMetadata = variable.obtainEnvIdField();
          if (isNotEmpty(envIdFromMetadata)) {
            InfrastructureDefinition infrastructureDefinition =
                infrastructureDefinitionService.getInfraDefByName(appId, envIdFromMetadata, value);
            notNullCheck("Infrastructure Definition  [" + value
                    + "] doesn't exist in specified application and environment " + appId,
                infrastructureDefinition, USER);
            return infrastructureDefinition.getUuid();
          }
          return value;
        default:
          return value;
      }
    }
    return value;
  }

  private void validateRequiredVarsPresent(List<QLVariableInput> variableInputs, List<Variable> workflowVariables) {
    List<String> requiredVariables = workflowVariables.stream()
                                         .filter(t -> t.isMandatory() && !t.isFixed())
                                         .map(Variable::getName)
                                         .collect(Collectors.toList());
    List<String> variablesPresent = variableInputs.stream().map(QLVariableInput::getName).collect(Collectors.toList());
    if (!variablesPresent.containsAll(requiredVariables)) {
      throw new InvalidRequestException("Value not provided for required variable: ["
          + StringUtils.join(requiredVariables.removeAll(variablesPresent), ","));
    }
  }

  public void authorizePipelineExecution(
      QLStartExecutionInput triggerExecutionInput, List<PermissionAttribute> permissionAttributeList, String appId) {
    authHandler.authorize(
        permissionAttributeList, Collections.singletonList(appId), triggerExecutionInput.getEntityId());
    logger.info("Authorization successful for executing pipeline: {} for User {}", triggerExecutionInput.getEntityId(),
        UserThreadLocal.get());
  }
}

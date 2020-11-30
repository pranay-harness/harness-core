package software.wings.graphql.datafetcher.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.graphql.datafetcher.DataFetcherUtils.GENERIC_EXCEPTION_MSG;
import static software.wings.service.impl.workflow.WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CreatedByType;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.logging.AutoLogContext;

import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.deployment.WorkflowVariablesMetadata;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.VariableController;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.mutation.execution.input.QLServiceInput;
import software.wings.graphql.schema.mutation.execution.input.QLStartExecutionInput;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.graphql.schema.mutation.execution.input.QLVariableValue;
import software.wings.graphql.schema.mutation.execution.input.QLVariableValueType;
import software.wings.graphql.schema.mutation.execution.payload.QLStartExecutionPayload;
import software.wings.graphql.schema.query.QLServiceInputsForExecutionParams;
import software.wings.graphql.schema.query.QLTriggerQueryParameters.QLTriggerQueryParametersKeys;
import software.wings.graphql.schema.type.QLApiKey;
import software.wings.graphql.schema.type.QLApprovalStageExecution;
import software.wings.graphql.schema.type.QLCause;
import software.wings.graphql.schema.type.QLExecuteOptions;
import software.wings.graphql.schema.type.QLExecutedByAPIKey;
import software.wings.graphql.schema.type.QLExecutedByTrigger;
import software.wings.graphql.schema.type.QLExecutedByUser;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.graphql.schema.type.QLPipelineExecution.QLPipelineExecutionBuilder;
import software.wings.graphql.schema.type.QLPipelineStageExecution;
import software.wings.graphql.schema.type.QLVariable;
import software.wings.graphql.schema.type.QLWorkflowStageExecution;
import software.wings.graphql.schema.type.QLWorkflowStageExecution.QLWorkflowStageExecutionBuilder;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTag;
import software.wings.infra.InfrastructureDefinition;
import software.wings.security.PermissionAttribute;
import software.wings.service.impl.AppLogContext;
import software.wings.service.impl.WorkflowLogContext;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class PipelineExecutionController {
  @Inject AuthHandler authHandler;
  @Inject AuthService authService;
  @Inject PipelineService pipelineService;
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject EnvironmentService environmentService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject ExecutionController executionController;
  @Inject FeatureFlagService featureFlagService;

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

    if (workflowExecution.getPipelineExecution() != null
        && workflowExecution.getPipelineExecution().getPipelineStageExecutions() != null) {
      Pipeline pipeline = workflowExecution.getPipelineExecution().getPipeline();
      builder.pipelineStageExecutions(workflowExecution.getPipelineExecution()
                                          .getPipelineStageExecutions()
                                          .stream()
                                          .map(exec
                                              -> populatePipelineStageExecution(workflowExecution.getUuid(), pipeline,
                                                  exec, workflowExecution.getExecutionArgs()))
                                          .collect(Collectors.toList()));
    }

    builder.id(workflowExecution.getUuid())
        .pipelineId(workflowExecution.getWorkflowId())
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

  private QLPipelineStageExecution populatePipelineStageExecution(
      String pipelineExecutionId, Pipeline pipeline, PipelineStageExecution execution, ExecutionArgs args) {
    StateType stateType = StateType.valueOf(execution.getStateType());
    PipelineStageElement element =
        pipeline.getPipelineStages()
            .stream()
            .flatMap(ps
                -> ps.getPipelineStageElements().stream().filter(
                    se -> se.getUuid().equals(execution.getPipelineStageElementId())))
            .findFirst()
            .orElseThrow(() -> new UnexpectedException("Expected at least one pipeline stage element"));

    if (Lists.newArrayList(StateType.APPROVAL, StateType.APPROVAL_RESUME).stream().anyMatch(stateType::equals)) {
      return QLApprovalStageExecution.builder()
          .pipelineStageElementId(execution.getPipelineStageElementId())
          .pipelineStageName(element.getProperties().get("stageName").toString())
          .pipelineStepName(element.getName())
          .status(ExecutionController.convertStatus(execution.getStatus()))
          .approvalStepType(ApprovalStateType.valueOf(element.getProperties().get("approvalStateType").toString()))
          .build();
    } else {
      String workflowExecutionId = null;
      if (!execution.getWorkflowExecutions().isEmpty()) {
        workflowExecutionId =
            execution.getWorkflowExecutions()
                .stream()
                .findFirst()
                .orElseThrow(() -> new UnexpectedException("Expected at least one workflow execution"))
                .getUuid();
      }
      QLWorkflowStageExecutionBuilder builder = QLWorkflowStageExecution.builder();
      if (ExecutionStatus.PAUSED.equals(execution.getStatus())) {
        // We will set variables only for paused pipeline stages.
        // Since we do not store information about a WF definition in the execution entity
        // we need to read the WF every time and this can be different from the time of execution.
        // We need Workflow variables here. For which we need to read from DB.
        try {
          WorkflowVariablesMetadata metadata = workflowExecutionService.fetchWorkflowVariables(
              pipeline.getAppId(), args, pipelineExecutionId, execution.getPipelineStageElementId());

          List<QLVariable> variables = new ArrayList<>();
          VariableController.populateVariables(metadata.getWorkflowVariables(), variables);
          builder.runtimeInputVariables(variables);
        } catch (Exception e) {
          log.warn("Exception was thrown try to fetch runtime variables for a paused pipeline stage", e);
        }
      }
      return builder.pipelineStageElementId(execution.getPipelineStageElementId())
          .pipelineStageName(element.getProperties().get("stageName").toString())
          .pipelineStepName(element.getName())
          .status(ExecutionController.convertStatus(execution.getStatus()))
          .workflowExecutionId(workflowExecutionId)
          .build();
    }
  }

  QLStartExecutionPayload startPipelineExecution(
      QLStartExecutionInput triggerExecutionInput, MutationContext mutationContext) {
    String appId = triggerExecutionInput.getApplicationId();
    try (AutoLogContext ignore = new AppLogContext(appId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      String pipelineId = triggerExecutionInput.getEntityId();

      List<QLVariableInput> variableInputs = triggerExecutionInput.getVariableInputs();
      if (variableInputs == null) {
        variableInputs = new ArrayList<>();
      }

      List<QLServiceInput> serviceInputs = triggerExecutionInput.getServiceInputs();
      if (serviceInputs == null) {
        serviceInputs = new ArrayList<>();
      }

      validateInputs(triggerExecutionInput);

      Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, true);
      notNullCheck("Pipeline " + pipelineId + " doesn't exist in the specified application " + appId, pipeline, USER);

      String envId = resolveEnvId(pipeline, variableInputs, true);
      authService.checkIfUserAllowedToDeployPipelineToEnv(appId, envId);

      List<String> extraVariables = new ArrayList<>();
      Map<String, String> variableValues =
          validateAndResolvePipelineVariables(pipeline, variableInputs, envId, extraVariables, false);
      List<Artifact> artifacts = validateAndGetArtifactsFromServiceInputs(variableValues, serviceInputs, pipeline);
      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setWorkflowType(WorkflowType.PIPELINE);
      executionArgs.setPipelineId(triggerExecutionInput.getEntityId());
      executionArgs.setContinueWithDefaultValues(triggerExecutionInput.isContinueWithDefaultValues());
      executionController.populateExecutionArgs(
          variableValues, artifacts, triggerExecutionInput, mutationContext, executionArgs);
      WorkflowExecution workflowExecution =
          workflowExecutionService.triggerEnvExecution(appId, envId, executionArgs, null);

      if (workflowExecution == null) {
        throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
      }

      final QLPipelineExecutionBuilder executionBuilder = QLPipelineExecution.builder();
      populatePipelineExecution(workflowExecution, executionBuilder);

      String warningMessage = null;
      if (isNotEmpty(extraVariables)) {
        warningMessage = "Ignoring values for variables: [" + StringUtils.join(extraVariables, ",")
            + "] as they don't exist in Pipeline. Might be modified";
      }

      return QLStartExecutionPayload.builder()
          .execution(executionBuilder.build())
          .warningMessage(warningMessage)
          .clientMutationId(triggerExecutionInput.getClientMutationId())
          .build();
    }
  }

  private void validateInputs(QLStartExecutionInput triggerExecutionInput) {
    if (triggerExecutionInput.isTargetToSpecificHosts() || isNotEmpty(triggerExecutionInput.getSpecificHosts())) {
      throw new InvalidRequestException(
          "Hosts can't be overridden for pipeline,Target to specific hosts is only available in a Workflow Execution",
          USER);
    }
  }

  public String resolveEnvId(WorkflowExecution execution, Pipeline pipeline, List<QLVariableInput> variableInputs) {
    String envId = null;
    Variable envVariable = WorkflowServiceTemplateHelper.getEnvVariable(pipeline.getPipelineVariables());
    if (envVariable != null && !Boolean.TRUE.equals(envVariable.getRuntimeInput())) {
      String key = envVariable.getName();
      envId = execution.getExecutionArgs().getWorkflowVariables().get(key);
    } else {
      envId = resolveEnvId(pipeline, variableInputs);
    }
    return envId;
  }

  public String resolveEnvId(Pipeline pipeline, List<QLVariableInput> variableInputs) {
    return resolveEnvId(pipeline, variableInputs, false);
  }

  public String resolveEnvId(Pipeline pipeline, List<QLVariableInput> variableInputs, boolean skipRuntimeVars) {
    List<Variable> pipelineVariables = pipeline.getPipelineVariables();
    String templatizedEnvName = getTemplatizedEnvVariableName(pipelineVariables);
    if (templatizedEnvName == null) {
      log.info("Environment is Not templatized in pipeline {} ", pipeline.getUuid());
      return null;
    }
    if (!isEmpty(variableInputs)) {
      QLVariableInput envVarInput =
          variableInputs.stream().filter(t -> templatizedEnvName.equals(t.getName())).findFirst().orElse(null);
      if (envVarInput != null) {
        QLVariableValue envVarValue = envVarInput.getVariableValue();
        notNullCheck(envVarInput.getName() + " has no variable value present", envVarValue, USER);
        switch (envVarValue.getType()) {
          case ID:
            String envId = envVarValue.getValue();
            Environment environment = environmentService.get(pipeline.getAppId(), envId);
            notNullCheck("Environment [" + envId + "] doesn't exist in specified application " + pipeline.getAppId(),
                environment, USER);
            return envId;
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
    boolean isRuntime = pipeline.getPipelineVariables()
                            .stream()
                            .filter(v -> v.getName().equals(templatizedEnvName))
                            .anyMatch(v -> Boolean.TRUE.equals(v.getRuntimeInput()));
    if (isRuntime && skipRuntimeVars) {
      log.info("Environment is runtime in pipeline {} ", pipeline.getUuid());
      return null;
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
        serviceInputs, pipeline.getAppId(), artifactNeededServiceIds, artifacts, new ArrayList<>());
    return artifacts;
  }

  public Map<String, String> resolvePipelineVariables(Pipeline pipeline, List<QLVariableInput> variableInputs,
      String envId, List<String> extraVariables, boolean isTriggerFlow) {
    List<Variable> pipelineVariables = pipeline.getPipelineVariables();
    if (isEmpty(pipelineVariables)) {
      return new HashMap<>();
    }

    Map<String, String> pipelineVariableValues = new HashMap<>();
    for (QLVariableInput variableInput : variableInputs) {
      Variable variableInPipeline =
          pipelineVariables.stream().filter(t -> t.getName().equals(variableInput.getName())).findFirst().orElse(null);
      if (variableInPipeline != null) {
        QLVariableValue variableValue = variableInput.getVariableValue();
        QLVariableValueType type = variableValue.getType();
        switch (type) {
          case ID:
            executionController.validateVariableValue(
                pipeline.getAppId(), variableValue.getValue(), variableInPipeline, envId);
            pipelineVariableValues.put(variableInput.getName(), variableValue.getValue());
            break;
          case NAME:
            String value = resolveVariableValue(
                pipeline.getAppId(), variableValue.getValue(), variableInPipeline, pipeline, envId);
            pipelineVariableValues.put(variableInput.getName(), value);
            break;
          case EXPRESSION:
            if (isTriggerFlow) {
              pipelineVariableValues.put(variableInput.getName(), variableValue.getValue());
            } else {
              throw new UnsupportedOperationException("Expression Type not supported");
            }
            break;
          default:
            throw new UnsupportedOperationException("Value Type " + type + " Not supported");
        }
      } else {
        extraVariables.add(variableInput.getName());
      }
    }
    return pipelineVariableValues;
  }

  public Map<String, String> validateAndResolveRuntimePipelineStageVars(Pipeline pipeline,
      List<QLVariableInput> variableInputs, String envId, List<String> extraVariables, String pipelineStageElementId,
      boolean isTriggerFlow) {
    PipelineStageElement pipelineStageElement =
        pipeline.getPipelineStages()
            .stream()
            .flatMap(pipelineStage -> pipelineStage.getPipelineStageElements().stream())
            .filter(stageElement -> stageElement.getUuid().equals(pipelineStageElementId))
            .findFirst()
            .orElse(null);

    if (pipelineStageElement == null) {
      throw new InvalidRequestException(
          "No stage found for the given pipeline stage ID " + pipelineStageElementId, USER);
    }

    if (pipelineStageElement.getRuntimeInputsConfig() == null
        || pipelineStageElement.getRuntimeInputsConfig().getRuntimeInputVariables() == null) {
      return new HashMap<>();
    }

    Set<String> stageVariables = pipelineStageElement.getWorkflowVariables()
                                     .values()
                                     .stream()
                                     .filter(ExpressionEvaluator::matchesVariablePattern)
                                     .map(ExpressionEvaluator::getName)
                                     .collect(Collectors.toSet());

    List<Variable> pipelineVariables = pipeline.getPipelineVariables()
                                           .stream()
                                           .filter(variable -> stageVariables.contains(variable.getName()))

                                           .collect(Collectors.toList());
    if (isEmpty(pipelineVariables)) {
      return new HashMap<>();
    }
    validateRuntimeReqVars(variableInputs, pipelineVariables);
    return resolvePipelineVariables(pipeline, variableInputs, envId, extraVariables, isTriggerFlow);
  }

  public Map<String, String> validateAndResolvePipelineVariables(Pipeline pipeline,
      List<QLVariableInput> variableInputs, String envId, List<String> extraVariables, boolean isTriggerFlow) {
    List<Variable> pipelineVariables = pipeline.getPipelineVariables();
    if (isEmpty(pipelineVariables)) {
      return new HashMap<>();
    }
    validateRequiredVarsPresent(variableInputs, pipelineVariables);
    return resolvePipelineVariables(pipeline, variableInputs, envId, extraVariables, isTriggerFlow);
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
          String envIdForInfra;
          if (envId != null) {
            envIdForInfra = envId;
          } else {
            envIdForInfra = variable.obtainEnvIdField();
          }
          if (isNotEmpty(envIdForInfra)) {
            if (value.contains(",")
                && featureFlagService.isEnabled(FeatureName.MULTISELECT_INFRA_PIPELINE, pipeline.getAccountId())) {
              return handleMultiInfra(appId, envIdForInfra, value, variable);
            }
            InfrastructureDefinition infrastructureDefinition =
                infrastructureDefinitionService.getInfraDefByName(appId, envIdForInfra, value);
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

  private String handleMultiInfra(String appId, String envIdForInfra, String value, Variable variable) {
    if (!variable.isAllowMultipleValues()) {
      throw new InvalidRequestException("Variable " + variable.getName() + " doesnt allow multiple values");
    }
    String[] values = value.trim().split("\\s*,\\s*");
    List<String> infraValues = new ArrayList<>();
    for (String val : values) {
      InfrastructureDefinition infrastructureDefinition =
          infrastructureDefinitionService.getInfraDefByName(appId, envIdForInfra, val);
      notNullCheck(
          "Infrastructure Definition  [" + val + "] doesn't exist in specified application and environment " + appId,
          infrastructureDefinition, USER);
      infraValues.add(infrastructureDefinition.getUuid());
    }
    return String.join(",", infraValues);
  }

  private void validateRuntimeReqVars(List<QLVariableInput> variableInputs, List<Variable> workflowVariables) {
    List<String> requiredVariables =
        workflowVariables.stream()
            .filter(t -> t.isMandatory() && !t.isFixed() && Boolean.TRUE.equals(t.getRuntimeInput()))
            .map(Variable::getName)
            .collect(Collectors.toList());
    validateRequiredVars(requiredVariables, variableInputs);
  }

  private void validateRequiredVars(List<String> required, List<QLVariableInput> present) {
    List<String> variablesPresent = present.stream().map(QLVariableInput::getName).collect(Collectors.toList());
    if (!variablesPresent.containsAll(required)) {
      required.removeAll(variablesPresent);
      throw new InvalidRequestException(
          "Value not provided for required variable: [" + StringUtils.join(required, ",") + "]");
    }
  }

  private void validateRequiredVarsPresent(List<QLVariableInput> variableInputs, List<Variable> workflowVariables) {
    List<String> requiredVariables =
        workflowVariables.stream()
            .filter(t -> t.isMandatory() && !t.isFixed() && !Boolean.TRUE.equals(t.getRuntimeInput()))
            .map(Variable::getName)
            .collect(Collectors.toList());
    validateRequiredVars(requiredVariables, variableInputs);
  }

  public void handleAuthentication(String appId, Pipeline pipeline) {
    String pipelineId = pipeline.getUuid();
    notNullCheck("Pipeline " + pipelineId + " doesn't exist in the specified application " + appId, pipeline, USER);
    PermissionAttribute permissionAttribute =
        new PermissionAttribute(PermissionAttribute.PermissionType.PIPELINE, PermissionAttribute.Action.READ);
    List<PermissionAttribute> permissionAttributeList = Collections.singletonList(permissionAttribute);
    authHandler.authorize(permissionAttributeList, Collections.singletonList(appId), pipelineId);
  }

  List<String> getArtifactNeededServices(QLServiceInputsForExecutionParams parameters) {
    String appId = parameters.getApplicationId();
    try (AutoLogContext ignore = new AppLogContext(appId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      String pipelineId = parameters.getEntityId();
      List<QLVariableInput> variableInputs = parameters.getVariableInputs();
      if (variableInputs == null) {
        variableInputs = new ArrayList<>();
      }

      Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, true);
      handleAuthentication(appId, pipeline);
      try (
          AutoLogContext ignore1 = new WorkflowLogContext(pipelineId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
        String envId = resolveEnvId(pipeline, variableInputs);
        List<String> extraVariables = new ArrayList<>();
        Map<String, String> variableValues =
            validateAndResolvePipelineVariables(pipeline, variableInputs, envId, extraVariables, false);
        DeploymentMetadata finalDeploymentMetadata =
            pipelineService.fetchDeploymentMetadata(appId, pipeline, variableValues);
        if (finalDeploymentMetadata != null) {
          List<String> artifactNeededServiceIds = finalDeploymentMetadata.getArtifactRequiredServiceIds();
          if (isNotEmpty(artifactNeededServiceIds)) {
            return artifactNeededServiceIds;
          }
        }
        log.info("No Services requires artifact inputs for this pipeline: " + pipelineId);
        return new ArrayList<>();
      }
    }
  }
}

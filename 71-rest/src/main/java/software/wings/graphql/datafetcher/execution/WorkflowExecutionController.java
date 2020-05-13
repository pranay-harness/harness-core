package software.wings.graphql.datafetcher.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.graphql.datafetcher.DataFetcherUtils.GENERIC_EXCEPTION_MSG;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CreatedByType;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.WorkflowType;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.artifact.ArtifactController;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.mutation.execution.input.QLServiceInput;
import software.wings.graphql.schema.mutation.execution.input.QLStartExecutionInput;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.graphql.schema.mutation.execution.input.QLVariableValue;
import software.wings.graphql.schema.mutation.execution.input.QLVariableValueType;
import software.wings.graphql.schema.mutation.execution.payload.QLStartExecutionPayload;
import software.wings.graphql.schema.query.QLExecutionQueryParameters.QLExecutionQueryParametersKeys;
import software.wings.graphql.schema.query.QLServiceInputsForExecutionParams;
import software.wings.graphql.schema.query.QLTriggerQueryParameters.QLTriggerQueryParametersKeys;
import software.wings.graphql.schema.type.QLApiKey;
import software.wings.graphql.schema.type.QLCause;
import software.wings.graphql.schema.type.QLExecuteOptions;
import software.wings.graphql.schema.type.QLExecutedAlongPipeline;
import software.wings.graphql.schema.type.QLExecutedByAPIKey;
import software.wings.graphql.schema.type.QLExecutedByTrigger;
import software.wings.graphql.schema.type.QLExecutedByUser;
import software.wings.graphql.schema.type.QLWorkflowExecution;
import software.wings.graphql.schema.type.QLWorkflowExecution.QLWorkflowExecutionBuilder;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTag;
import software.wings.graphql.schema.type.artifact.QLArtifact;
import software.wings.graphql.schema.type.artifact.QLArtifact.QLArtifactBuilder;
import software.wings.infra.InfrastructureDefinition;
import software.wings.security.PermissionAttribute;
import software.wings.service.impl.AppLogContext;
import software.wings.service.impl.WorkflowLogContext;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class WorkflowExecutionController {
  @Inject private HPersistence persistence;
  @Inject AuthHandler authHandler;
  @Inject WorkflowService workflowService;
  @Inject EnvironmentService environmentService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject ExecutionController executionController;

  public void populateWorkflowExecution(
      @NotNull WorkflowExecution workflowExecution, QLWorkflowExecutionBuilder builder) {
    QLCause cause = null;
    List<QLDeploymentTag> tags = new ArrayList<>();
    List<QLArtifact> artifacts = new ArrayList<>();

    if (workflowExecution.getPipelineExecutionId() != null) {
      cause =
          QLExecutedAlongPipeline.builder()
              .context(ImmutableMap.<String, Object>builder()
                           .put(QLExecutionQueryParametersKeys.executionId, workflowExecution.getPipelineExecutionId())
                           .build())
              .build();
    } else {
      CreatedByType createdByType = workflowExecution.getCreatedByType();
      if (CreatedByType.API_KEY == createdByType) {
        EmbeddedUser apiKey = workflowExecution.getCreatedBy();
        cause = QLExecutedByAPIKey.builder()
                    .apiKey(QLApiKey.builder().name(apiKey.getName()).id(apiKey.getUuid()).build())
                    .using(QLExecuteOptions.GRAPHQL_API)
                    .build();
      } else if (workflowExecution.getDeploymentTriggerId() != null) {
        cause =
            QLExecutedByTrigger.builder()
                .context(ImmutableMap.<String, Object>builder()
                             .put(QLTriggerQueryParametersKeys.triggerId, workflowExecution.getDeploymentTriggerId())
                             .build())
                .build();
      } else if (workflowExecution.getTriggeredBy() != null) {
        cause = QLExecutedByUser.builder()
                    .user(UserController.populateUser(workflowExecution.getTriggeredBy()))
                    .using(QLExecuteOptions.WEB_UI)
                    .build();
      }
    }

    if (isNotEmpty(workflowExecution.getTags())) {
      tags = workflowExecution.getTags()
                 .stream()
                 .map(tag -> QLDeploymentTag.builder().name(tag.getName()).value(tag.getValue()).build())
                 .collect(Collectors.toList());
    }

    if (isNotEmpty(workflowExecution.getArtifacts())) {
      artifacts = workflowExecution.getArtifacts()
                      .stream()
                      .map(artifact -> {
                        QLArtifactBuilder qlArtifactBuilder = QLArtifact.builder();
                        ArtifactController.populateArtifact(artifact, qlArtifactBuilder);
                        return qlArtifactBuilder.build();
                      })
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
        .artifacts(artifacts);
  }

  public QLStartExecutionPayload startWorkflowExecution(QLStartExecutionInput triggerExecutionInput,
      MutationContext mutationContext, List<PermissionAttribute> permissionAttributes) {
    String appId = triggerExecutionInput.getApplicationId();
    try (AutoLogContext ignore = new AppLogContext(appId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      String workflowId = triggerExecutionInput.getEntityId();
      List<QLVariableInput> variableInputs = triggerExecutionInput.getVariableInputs();
      if (variableInputs == null) {
        variableInputs = new ArrayList<>();
      }

      List<QLServiceInput> serviceInputs = triggerExecutionInput.getServiceInputs();
      if (serviceInputs == null) {
        serviceInputs = new ArrayList<>();
      }

      validateInputs(triggerExecutionInput);
      Workflow workflow = workflowService.readWorkflow(appId, workflowId);
      notNullCheck("Workflow " + workflowId + " doesn't exist in the specified application " + appId, workflow, USER);
      notNullCheck(
          "Error reading workflow " + workflowId + " Might be deleted", workflow.getOrchestrationWorkflow(), USER);

      try (
          AutoLogContext ignore1 = new WorkflowLogContext(workflowId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
        authHandler.authorize(permissionAttributes, Collections.singletonList(appId), workflowId);

        String envId = resolveEnvId(workflow, variableInputs);
        authHandler.checkIfUserAllowedToDeployToEnv(appId, envId);

        List<String> extraVariables = new ArrayList<>();
        Map<String, String> variableValues =
            validateAndResolveWorkflowVariables(workflow, variableInputs, envId, extraVariables);
        List<Artifact> artifacts = validateAndGetArtifactsFromServiceInputs(variableValues, serviceInputs, workflow);
        ExecutionArgs executionArgs = new ExecutionArgs();
        executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
        executionArgs.setOrchestrationId(triggerExecutionInput.getEntityId());

        executionController.populateExecutionArgs(
            variableValues, artifacts, triggerExecutionInput, mutationContext, executionArgs);
        WorkflowExecution workflowExecution =
            workflowExecutionService.triggerEnvExecution(appId, envId, executionArgs, null);

        if (workflowExecution == null) {
          throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
        }

        final QLWorkflowExecutionBuilder executionBuilder = QLWorkflowExecution.builder();
        populateWorkflowExecution(workflowExecution, executionBuilder);

        String warningMessage = null;
        if (isNotEmpty(extraVariables)) {
          warningMessage = "Ignoring values for variables: [" + StringUtils.join(extraVariables, ",")
              + "] as they don't exist in workflow. Workflow Might be modified";
        }

        return QLStartExecutionPayload.builder()
            .execution(executionBuilder.build())
            .warningMessage(warningMessage)
            .clientMutationId(triggerExecutionInput.getClientMutationId())
            .build();
      }
    }
  }

  private void validateInputs(QLStartExecutionInput triggerExecutionInput) {
    if (triggerExecutionInput.isTargetToSpecificHosts() && isEmpty(triggerExecutionInput.getSpecificHosts())) {
      throw new InvalidRequestException(
          "Host list can't be empty when Target To Specific Hosts option is enabled", USER);
    }
  }

  private List<Artifact> validateAndGetArtifactsFromServiceInputs(
      Map<String, String> variableValues, List<QLServiceInput> serviceInputs, Workflow workflow) {
    /* Fetch the deployment data to find out the required entity types */
    DeploymentMetadata deploymentMetadata = workflowService.fetchDeploymentMetadata(
        workflow.getAppId(), workflow, variableValues, null, null, DeploymentMetadata.Include.ARTIFACT_SERVICE);

    // Fetch the service
    List<String> artifactNeededServiceIds =
        deploymentMetadata == null ? new ArrayList<>() : deploymentMetadata.getArtifactRequiredServiceIds();
    if (isEmpty(artifactNeededServiceIds)) {
      return new ArrayList<>();
    }

    List<Artifact> artifacts = new ArrayList<>();
    executionController.getArtifactsFromServiceInputs(
        serviceInputs, workflow.getAppId(), artifactNeededServiceIds, artifacts);
    return artifacts;
  }

  private Map<String, String> validateAndResolveWorkflowVariables(
      Workflow workflow, List<QLVariableInput> variableInputs, String envId, List<String> extraVariablesInAPI) {
    List<Variable> workflowVariables = workflow.getOrchestrationWorkflow().getUserVariables();
    if (isEmpty(workflowVariables)) {
      List<String> extraVariables = variableInputs.stream().map(t -> t.getName()).collect(Collectors.toList());
      extraVariablesInAPI.addAll(extraVariables);
      return new HashMap<>();
    }

    validateRequiredVarsPresent(variableInputs, workflowVariables);
    validateFixedVarValueUnchanged(variableInputs, workflowVariables);

    Map<String, String> workflowVariableValues = new HashMap<>();
    for (QLVariableInput variableInput : variableInputs) {
      Variable variableInWorkflow =
          workflowVariables.stream().filter(t -> t.getName().equals(variableInput.getName())).findFirst().orElse(null);
      if (variableInWorkflow != null) {
        QLVariableValue variableValue = variableInput.getVariableValue();
        QLVariableValueType type = variableValue.getType();
        switch (type) {
          case ID:
            workflowVariableValues.put(variableInput.getName(), variableValue.getValue());
            break;
          case NAME:
            String value = resolveVariableValue(
                workflow.getAppId(), variableValue.getValue(), variableInWorkflow, workflow, envId);
            workflowVariableValues.put(variableInput.getName(), value);
            break;
          default:
            throw new UnsupportedOperationException("Value Type " + type + " Not supported");
        }
      } else {
        extraVariablesInAPI.add(variableInput.getName());
      }
    }

    return workflowVariableValues;
  }

  private void validateFixedVarValueUnchanged(List<QLVariableInput> variableInputs, List<Variable> workflowVariables) {
    List<Variable> fixedVariables = workflowVariables.stream().filter(Variable::isFixed).collect(Collectors.toList());
    if (isEmpty(fixedVariables)) {
      return;
    }
    for (Variable var : fixedVariables) {
      QLVariableInput valueInInput =
          variableInputs.stream().filter(t -> t.getName().equals(var.getName())).findFirst().orElse(null);
      if (valueInInput != null && valueInInput.getVariableValue() != null) {
        String variableValueInput = valueInInput.getVariableValue().getValue();
        if (!variableValueInput.equals(var.getValue())) {
          throw new InvalidRequestException("Cannot change value of a fixed variable in workflow: " + var.getName()
                  + ". Value set in workflow is: " + var.getValue(),
              USER);
        }
      }
    }
  }

  private String resolveVariableValue(String appId, String value, Variable variable, Workflow workflow, String envId) {
    EntityType entityType = variable.obtainEntityType();
    if (entityType != null) {
      switch (entityType) {
        case ENVIRONMENT:
          return envId;
        case SERVICE:
          Service serviceFromName = serviceResourceService.getServiceByName(appId, value);
          notNullCheck(
              "Service [" + value + "] doesn't exist in specified application " + appId, serviceFromName, USER);
          return serviceFromName.getUuid();
        case INFRASTRUCTURE_DEFINITION:
          InfrastructureDefinition infrastructureDefinition =
              infrastructureDefinitionService.getInfraDefByName(appId, envId, value);
          notNullCheck("Infrastructure Definition  [" + value
                  + "] doesn't exist in specified application and environment " + appId,
              infrastructureDefinition, USER);
          return infrastructureDefinition.getUuid();
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
      requiredVariables.removeAll(variablesPresent);
      throw new InvalidRequestException(
          "Value not provided for required variable: [" + StringUtils.join(requiredVariables, ",") + "]");
    }
  }

  private String resolveEnvId(Workflow workflow, List<QLVariableInput> variableInputs) {
    if (!workflow.checkEnvironmentTemplatized()) {
      return workflow.getEnvId();
    }
    if (!isEmpty(variableInputs)) {
      OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      String envVarName =
          WorkflowServiceTemplateHelper.getTemplatizedEnvVariableName(orchestrationWorkflow.getUserVariables());
      if (envVarName != null) {
        QLVariableInput envVarInput =
            variableInputs.stream().filter(t -> envVarName.equals(t.getName())).findFirst().orElse(null);
        if (envVarInput != null) {
          QLVariableValue envVarValue = envVarInput.getVariableValue();
          switch (envVarValue.getType()) {
            case ID:
              return envVarValue.getValue();
            case NAME:
              String envName = envVarValue.getValue();
              Environment environmentFromName = environmentService.getEnvironmentByName(workflow.getAppId(), envName);
              notNullCheck(
                  "Environment [" + envName + "] doesn't exist in specified application " + workflow.getAppId(),
                  environmentFromName, USER);
              return environmentFromName.getUuid();
            default:
              throw new UnsupportedOperationException("Value Type " + envVarValue.getType() + " Not supported");
          }
        }
      }
    }
    throw new InvalidRequestException(
        "Workflow [" + workflow.getName() + "] has environment parameterized. However, the value not supplied", USER);
  }

  public List<String> getArtifactNeededServices(QLServiceInputsForExecutionParams parameters) {
    String appId = parameters.getApplicationId();
    try (AutoLogContext ignore = new AppLogContext(appId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      String workflowId = parameters.getEntityId();
      List<QLVariableInput> variableInputs = parameters.getVariableInputs();
      if (variableInputs == null) {
        variableInputs = new ArrayList<>();
      }

      Workflow workflow = workflowService.readWorkflow(appId, workflowId);
      notNullCheck("Workflow " + workflowId + " doesn't exist in the specified application " + appId, workflow, USER);
      notNullCheck(
          "Error reading workflow " + workflowId + " Might be deleted", workflow.getOrchestrationWorkflow(), USER);
      try (
          AutoLogContext ignore1 = new WorkflowLogContext(workflowId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
        PermissionAttribute permissionAttribute =
            new PermissionAttribute(PermissionAttribute.PermissionType.WORKFLOW, PermissionAttribute.Action.READ);
        List<PermissionAttribute> permissionAttributeList = Collections.singletonList(permissionAttribute);
        authHandler.authorize(permissionAttributeList, Collections.singletonList(appId), workflowId);

        String envId = resolveEnvId(workflow, variableInputs);

        List<String> extraVariables = new ArrayList<>();
        Map<String, String> variableValues =
            validateAndResolveWorkflowVariables(workflow, variableInputs, envId, extraVariables);

        DeploymentMetadata finalDeploymentMetadata =
            workflowService.fetchDeploymentMetadata(appId, workflow, variableValues, null, null, false, null);
        if (finalDeploymentMetadata != null) {
          List<String> artifactNeededServiceIds = finalDeploymentMetadata.getArtifactRequiredServiceIds();
          if (isNotEmpty(artifactNeededServiceIds)) {
            return artifactNeededServiceIds;
          }
        }
        logger.info("No Services requires artifact inputs for this workflow: " + workflowId);
        return new ArrayList<>();
      }
    }
  }
}

package software.wings.graphql.datafetcher.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerBuilder;
import software.wings.graphql.datafetcher.execution.PipelineExecutionController;
import software.wings.graphql.datafetcher.execution.WorkflowExecutionController;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.graphql.schema.type.trigger.QLArtifactSelectionInput;
import software.wings.graphql.schema.type.trigger.QLCreateOrUpdateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLTrigger;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;
import software.wings.graphql.schema.type.trigger.QLTriggerPayload;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.security.auth.DeploymentAuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class TriggerController {
  @Inject AppService appService;
  @Inject TriggerActionController triggerActionController;
  @Inject TriggerConditionController triggerConditionController;
  @Inject HPersistence persistence;
  @Inject DeploymentAuthHandler deploymentAuthHandler;
  @Inject PipelineService pipelineService;
  @Inject WorkflowService workflowService;
  @Inject AuthHandler authHandler;
  @Inject WorkflowExecutionController workflowExecutionController;
  @Inject PipelineExecutionController pipelineExecutionController;

  public void populateTrigger(Trigger trigger, QLTriggerBuilder qlTriggerBuilder, String accountId) {
    qlTriggerBuilder.id(trigger.getUuid())
        .name(trigger.getName())
        .description(trigger.getDescription())
        .condition(triggerConditionController.populateTriggerCondition(trigger, accountId))
        .action(triggerActionController.populateTriggerAction(trigger))
        .createdAt(trigger.getCreatedAt())
        .excludeHostsWithSameArtifact(trigger.isExcludeHostsWithSameArtifact())
        .createdBy(UserController.populateUser(trigger.getCreatedBy()));
  }

  public QLTriggerPayload prepareQLTrigger(Trigger trigger, String clientMutationId, String accountId) {
    QLTriggerBuilder builder = QLTrigger.builder();
    populateTrigger(trigger, builder, accountId);
    return QLTriggerPayload.builder().clientMutationId(clientMutationId).trigger(builder.build()).build();
  }

  public Trigger prepareTrigger(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, String accountId) {
    validateTrigger(qlCreateOrUpdateTriggerInput, accountId);

    TriggerBuilder triggerBuilder = Trigger.builder();
    triggerBuilder.uuid(qlCreateOrUpdateTriggerInput.getTriggerId());
    triggerBuilder.name(qlCreateOrUpdateTriggerInput.getName().trim());
    triggerBuilder.appId(qlCreateOrUpdateTriggerInput.getApplicationId());
    triggerBuilder.description(qlCreateOrUpdateTriggerInput.getDescription());
    triggerBuilder.workflowId(qlCreateOrUpdateTriggerInput.getAction().getEntityId());

    String envId = null;
    Map<String, String> resolvedWorkflowVariables = null;
    String appId = qlCreateOrUpdateTriggerInput.getApplicationId();
    List<QLVariableInput> variables = qlCreateOrUpdateTriggerInput.getAction().getVariables();

    triggerBuilder.workflowType(triggerActionController.resolveWorkflowType(qlCreateOrUpdateTriggerInput));
    switch (qlCreateOrUpdateTriggerInput.getAction().getExecutionType()) {
      case WORKFLOW:
        String workflowId = qlCreateOrUpdateTriggerInput.getAction().getEntityId();
        Workflow workflow = workflowService.readWorkflow(appId, workflowId);
        validateWorkflow(appId, workflowId, workflow);

        envId = workflowExecutionController.resolveEnvId(workflow, variables);
        deploymentAuthHandler.authorizeWorkflowExecution(appId, workflowId);
        authHandler.checkIfUserAllowedToDeployToEnv(appId, envId);

        resolvedWorkflowVariables =
            triggerActionController.validateAndResolveWorkflowVariables(variables, workflow, envId);
        triggerBuilder.workflowVariables(resolvedWorkflowVariables);
        validateWorkflowArtifactSourceServiceIds(
            resolvedWorkflowVariables, qlCreateOrUpdateTriggerInput.getAction().getArtifactSelections(), workflow);
        break;
      case PIPELINE:
        String pipelineId = qlCreateOrUpdateTriggerInput.getAction().getEntityId();
        Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, false);
        validatePipeline(appId, pipelineId, pipeline);
        deploymentAuthHandler.authorizePipelineExecution(appId, pipelineId);

        envId = pipelineExecutionController.resolveEnvId(pipeline, variables);

        resolvedWorkflowVariables =
            triggerActionController.validateAndResolvePipelineVariables(variables, pipeline, envId);
        triggerBuilder.workflowVariables(resolvedWorkflowVariables);
        validatePipelineArtifactSourceServiceIds(
            resolvedWorkflowVariables, qlCreateOrUpdateTriggerInput.getAction().getArtifactSelections(), pipeline);
        break;
      default:
    }

    triggerBuilder.artifactSelections(triggerActionController.resolveArtifactSelections(qlCreateOrUpdateTriggerInput));
    triggerBuilder.condition(triggerConditionController.resolveTriggerCondition(qlCreateOrUpdateTriggerInput));

    return triggerBuilder.build();
  }

  void validateWorkflow(String appId, String workflowId, Workflow workflow) {
    notNullCheck("Workflow " + workflowId + " doesn't exist in the specified application " + appId, workflow, USER);
    notNullCheck(
        "Error reading workflow " + workflowId + " Might be deleted", workflow.getOrchestrationWorkflow(), USER);
  }

  void validatePipeline(String appId, String pipelineId, Pipeline pipeline) {
    notNullCheck("Pipeline " + pipelineId + " doesn't exist in the specified application " + appId, pipeline, USER);
  }

  void validateTrigger(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, String accountId) {
    validateUpdateTrigger(qlCreateOrUpdateTriggerInput, accountId);

    if (EmptyPredicate.isEmpty(qlCreateOrUpdateTriggerInput.getApplicationId())) {
      throw new InvalidRequestException("ApplicationId must not be empty", USER);
    }
    if (!accountId.equals(appService.getAccountIdByAppId(qlCreateOrUpdateTriggerInput.getApplicationId()))) {
      throw new InvalidRequestException("ApplicationId doesn't belong to this account", USER);
    }

    if (EmptyPredicate.isEmpty(qlCreateOrUpdateTriggerInput.getName().trim())) {
      throw new InvalidRequestException("Trigger name must not be empty", USER);
    }

    if (EmptyPredicate.isEmpty(qlCreateOrUpdateTriggerInput.getAction().getEntityId())) {
      throw new InvalidRequestException("Entity Id must not be empty", USER);
    }
  }

  private void validateUpdateTrigger(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, String accountId) {
    if (EmptyPredicate.isNotEmpty(qlCreateOrUpdateTriggerInput.getTriggerId())) {
      Trigger trigger = persistence.get(Trigger.class, qlCreateOrUpdateTriggerInput.getTriggerId());
      if (trigger == null) {
        throw new InvalidRequestException("Trigger doesn't exist", USER);
      }

      if (!accountId.equals(appService.getAccountIdByAppId(trigger.getAppId()))) {
        throw new InvalidRequestException("Trigger doesn't exist", USER);
      }

      QLExecutionType qlExecutionType =
          trigger.getWorkflowType() == WorkflowType.ORCHESTRATION ? QLExecutionType.WORKFLOW : QLExecutionType.PIPELINE;

      if (qlCreateOrUpdateTriggerInput.getAction().getExecutionType() != qlExecutionType) {
        throw new InvalidRequestException("Execution Type cannot be modified", USER);
      }
    }
  }

  void validateWorkflowArtifactSourceServiceIds(
      Map<String, String> variables, List<QLArtifactSelectionInput> artifactSelections, Workflow workflow) {
    /* Fetch the deployment data to find out the required entity types */
    DeploymentMetadata deploymentMetadata = workflowService.fetchDeploymentMetadata(
        workflow.getAppId(), workflow, variables, null, null, DeploymentMetadata.Include.ARTIFACT_SERVICE);

    List<String> artifactNeededServiceIds =
        deploymentMetadata == null ? new ArrayList<>() : deploymentMetadata.getArtifactRequiredServiceIds();
    validateArtifactSelections(artifactSelections, artifactNeededServiceIds);
  }

  void validatePipelineArtifactSourceServiceIds(
      Map<String, String> variables, List<QLArtifactSelectionInput> artifactSelections, Pipeline pipeline) {
    /* Fetch the deployment data to find out the required entity types */
    DeploymentMetadata deploymentMetadata = pipelineService.fetchDeploymentMetadata(
        pipeline.getAppId(), pipeline.getUuid(), variables, null, null, false, null);

    List<String> artifactNeededServiceIds =
        deploymentMetadata == null ? new ArrayList<>() : deploymentMetadata.getArtifactRequiredServiceIds();
    validateArtifactSelections(artifactSelections, artifactNeededServiceIds);
  }

  private void validateArtifactSelections(
      List<QLArtifactSelectionInput> artifactSelections, List<String> artifactNeededServiceIds) {
    if (artifactSelections == null) {
      artifactSelections = new ArrayList<>();
    }

    for (String id : artifactNeededServiceIds) {
      if (artifactSelections.stream().noneMatch(e -> id.equals(e.getServiceId()))) {
        throw new InvalidRequestException(
            String.format("Artifact Source for service id: %s must be specified", id), USER);
      }
    }
  }
}

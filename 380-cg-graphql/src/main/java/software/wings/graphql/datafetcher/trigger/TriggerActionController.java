/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WorkflowType;
import io.harness.exception.GraphQLException;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactSelection.ArtifactSelectionBuilder;
import software.wings.beans.trigger.ArtifactSelection.Type;
import software.wings.beans.trigger.ManifestSelection;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerBuilder;
import software.wings.graphql.datafetcher.execution.PipelineExecutionController;
import software.wings.graphql.datafetcher.execution.WorkflowExecutionController;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.graphql.schema.type.trigger.QLArtifactSelection;
import software.wings.graphql.schema.type.trigger.QLArtifactSelectionInput;
import software.wings.graphql.schema.type.trigger.QLConditionType;
import software.wings.graphql.schema.type.trigger.QLCreateOrUpdateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLFromTriggeringAppManifest;
import software.wings.graphql.schema.type.trigger.QLFromTriggeringArtifactSource;
import software.wings.graphql.schema.type.trigger.QLFromTriggeringPipeline;
import software.wings.graphql.schema.type.trigger.QLFromWebhookPayload;
import software.wings.graphql.schema.type.trigger.QLLastCollected;
import software.wings.graphql.schema.type.trigger.QLLastCollectedManifest;
import software.wings.graphql.schema.type.trigger.QLLastDeployedFromPipeline;
import software.wings.graphql.schema.type.trigger.QLLastDeployedFromWorkflow;
import software.wings.graphql.schema.type.trigger.QLLastDeployedManifestFromPipeline;
import software.wings.graphql.schema.type.trigger.QLLastDeployedManifestFromWorkflow;
import software.wings.graphql.schema.type.trigger.QLManifestFromTriggeringPipeline;
import software.wings.graphql.schema.type.trigger.QLManifestFromWebhookPayload;
import software.wings.graphql.schema.type.trigger.QLManifestSelection;
import software.wings.graphql.schema.type.trigger.QLManifestSelectionInput;
import software.wings.graphql.schema.type.trigger.QLPipelineAction;
import software.wings.graphql.schema.type.trigger.QLTriggerAction;
import software.wings.graphql.schema.type.trigger.QLTriggerActionInput;
import software.wings.graphql.schema.type.trigger.QLTriggerVariableValue;
import software.wings.graphql.schema.type.trigger.QLWebhookSource;
import software.wings.graphql.schema.type.trigger.QLWorkflowAction;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class TriggerActionController {
  @Inject WorkflowService workflowService;
  @Inject PipelineService pipelineService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject PipelineExecutionController pipelineExecutionController;
  @Inject WorkflowExecutionController workflowExecutionController;
  @Inject ArtifactStreamService artifactStreamService;

  QLTriggerAction populateTriggerAction(Trigger trigger) {
    QLTriggerAction triggerAction = null;

    List<QLArtifactSelection> artifactSelections =
        trigger.getArtifactSelections()
            .stream()
            .map(e -> {
              QLArtifactSelection artifactSelection = null;
              switch (e.getType()) {
                case ARTIFACT_SOURCE:
                  artifactSelection = QLFromTriggeringArtifactSource.builder()
                                          .serviceId(e.getServiceId())
                                          .serviceName(e.getServiceName())
                                          .build();
                  break;
                case LAST_COLLECTED:
                  artifactSelection = QLLastCollected.builder()
                                          .serviceId(e.getServiceId())
                                          .serviceName(e.getServiceName())
                                          .artifactSourceId(e.getArtifactStreamId())
                                          .artifactSourceName(e.getArtifactSourceName())
                                          .artifactFilter(e.getArtifactFilter())
                                          .regex(e.isRegex())
                                          .build();
                  break;
                case LAST_DEPLOYED:
                  if (trigger.getWorkflowType() == WorkflowType.PIPELINE) {
                    artifactSelection = QLLastDeployedFromPipeline.builder()
                                            .serviceId(e.getServiceId())
                                            .serviceName(e.getServiceName())
                                            .pipelineId(e.getPipelineId())
                                            .pipelineName(e.getPipelineName())
                                            .build();
                  } else {
                    artifactSelection = QLLastDeployedFromWorkflow.builder()
                                            .serviceId(e.getServiceId())
                                            .serviceName(e.getServiceName())
                                            .workflowId(e.getWorkflowId())
                                            .workflowName(e.getWorkflowName())
                                            .build();
                  }
                  break;
                case PIPELINE_SOURCE:
                  artifactSelection = QLFromTriggeringPipeline.builder()
                                          .serviceId(e.getServiceId())
                                          .serviceName(e.getServiceName())
                                          .build();
                  break;
                case WEBHOOK_VARIABLE:
                  artifactSelection = QLFromWebhookPayload.builder()
                                          .serviceId(e.getServiceId())
                                          .serviceName(e.getServiceName())
                                          .artifactSourceId(e.getArtifactStreamId())
                                          .artifactSourceName(e.getArtifactSourceName())
                                          .build();
                  break;
                default:
              }
              return artifactSelection;
            })
            .collect(Collectors.toList());

    List<QLManifestSelection> manifestSelections = new ArrayList<>();
    if (trigger.getManifestSelections() != null) {
      manifestSelections = trigger.getManifestSelections()
                               .stream()
                               .map(e -> getQLManifestSelection(trigger, e))
                               .collect(Collectors.toList());
    }

    List<QLTriggerVariableValue> variableValues = new ArrayList<>();
    if (trigger.getWorkflowVariables() != null) {
      trigger.getWorkflowVariables().forEach(
          (key, value) -> variableValues.add(QLTriggerVariableValue.builder().name(key).value(value).build()));
    }

    if (trigger.getWorkflowType() == WorkflowType.PIPELINE) {
      triggerAction = QLPipelineAction.builder()
                          .pipelineId(trigger.getWorkflowId())
                          .pipelineName(trigger.getWorkflowName())
                          .artifactSelections(artifactSelections)
                          .manifestSelections(manifestSelections)
                          .variables(variableValues)
                          .continueWithDefaultValues(trigger.isContinueWithDefaultValues())
                          .build();
    } else {
      triggerAction = QLWorkflowAction.builder()
                          .workflowId(trigger.getWorkflowId())
                          .workflowName(trigger.getWorkflowName())
                          .artifactSelections(artifactSelections)
                          .manifestSelections(manifestSelections)
                          .variables(variableValues)
                          .build();
    }
    return triggerAction;
  }

  private QLManifestSelection getQLManifestSelection(Trigger trigger, ManifestSelection selection) {
    QLManifestSelection manifestSelection = null;
    switch (selection.getType()) {
      case FROM_APP_MANIFEST:
        manifestSelection = QLFromTriggeringAppManifest.builder()
                                .serviceId(selection.getServiceId())
                                .serviceName(selection.getServiceName())
                                .manifestSelectionType(selection.getType())
                                .build();
        break;
      case LAST_COLLECTED:
        manifestSelection = QLLastCollectedManifest.builder()
                                .serviceId(selection.getServiceId())
                                .serviceName(selection.getServiceName())
                                .versionRegex(selection.getVersionRegex())
                                .appManifestId(selection.getAppManifestId())
                                .appManifestName(selection.getAppManifestName())
                                .manifestSelectionType(selection.getType())
                                .build();
        break;
      case LAST_DEPLOYED:
        if (trigger.getWorkflowType() == WorkflowType.PIPELINE) {
          manifestSelection = QLLastDeployedManifestFromPipeline.builder()
                                  .serviceId(selection.getServiceId())
                                  .serviceName(selection.getServiceName())
                                  .pipelineId(selection.getPipelineId())
                                  .pipelineName(selection.getPipelineName())
                                  .manifestSelectionType(selection.getType())
                                  .build();
        } else {
          manifestSelection = QLLastDeployedManifestFromWorkflow.builder()
                                  .serviceId(selection.getServiceId())
                                  .serviceName(selection.getServiceName())
                                  .workflowId(selection.getWorkflowId())
                                  .workflowName(selection.getWorkflowName())
                                  .manifestSelectionType(selection.getType())
                                  .build();
        }
        break;
      case PIPELINE_SOURCE:
        manifestSelection = QLManifestFromTriggeringPipeline.builder()
                                .serviceId(selection.getServiceId())
                                .serviceName(selection.getServiceName())
                                .manifestSelectionType(selection.getType())
                                .build();
        break;
      case WEBHOOK_VARIABLE:
        manifestSelection = QLManifestFromWebhookPayload.builder()
                                .serviceId(selection.getServiceId())
                                .serviceName(selection.getServiceName())
                                .appManifestId(selection.getAppManifestId())
                                .appManifestName(selection.getAppManifestName())
                                .manifestSelectionType(selection.getType())
                                .build();
        break;
      default:
        throw new GraphQLException(
            "Invalid manifest selection type " + selection.getType() + " present in trigger " + trigger.getUuid(),
            USER);
    }
    return manifestSelection;
  }

  List<ArtifactSelection> resolveArtifactSelections(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, List<String> artifactNeededServiceIds) {
    if (qlCreateOrUpdateTriggerInput.getAction().getArtifactSelections() == null || isEmpty(artifactNeededServiceIds)) {
      return new ArrayList<>();
    }

    return qlCreateOrUpdateTriggerInput.getAction()
        .getArtifactSelections()
        .stream()
        .map(e -> {
          if (isEmpty(e.getServiceId())) {
            throw new InvalidRequestException("Empty serviceId in Artifact Selection", USER);
          }

          if (serviceResourceService.get(e.getServiceId()) == null) {
            throw new InvalidRequestException(
                "ServiceId does not exist for Artifact Selection. ServiceId: " + e.getServiceId(), USER);
          }

          Type type = null;
          switch (e.getArtifactSelectionType()) {
            case FROM_TRIGGERING_ARTIFACT:
              type = validateAndResolveFromTriggeringArtifactArtifactSelectionType(qlCreateOrUpdateTriggerInput);
              break;
            case FROM_TRIGGERING_PIPELINE:
              type = validateAndResolveFromTriggeringPipelineArtifactSelectionType(qlCreateOrUpdateTriggerInput);
              break;
            case LAST_COLLECTED:
              if (isEmpty(e.getArtifactSourceId())) {
                throw new InvalidRequestException(
                    "Artifact Source Id to select artifact from is required when using LAST_COLLECTED", USER);
              }
              type = validateAndResolveLastCollectedArtifactSelectionType(e);
              break;
            case FROM_PAYLOAD_SOURCE:
              if (isEmpty(e.getArtifactSourceId())) {
                throw new InvalidRequestException(
                    "Artifact Source Id to select artifact from is required when using FROM_PAYLOAD_SOURCE", USER);
              }
              type = validateAndResolveFromPayloadSourceArtifactSelectionType(qlCreateOrUpdateTriggerInput, e);
              break;
            case LAST_DEPLOYED_PIPELINE:
              if (isEmpty(e.getPipelineId())) {
                throw new InvalidRequestException(
                    "Pipeline Id to select artifact from is required when using LAST_DEPLOYED_PIPELINE", USER);
              }
              type = validateAndResolveLastDeployedPipelineArtifactSelectionType(qlCreateOrUpdateTriggerInput);
              break;
            case LAST_DEPLOYED_WORKFLOW:
              if (isEmpty(e.getWorkflowId())) {
                throw new InvalidRequestException(
                    "Workflow Id to select artifact from is required when using LAST_DEPLOYED_WORKFLOW", USER);
              }
              type = validateAndResolveLastDeployedWorkflowArtifactSelectionType(qlCreateOrUpdateTriggerInput);
              break;
            default:
              throw new InvalidRequestException("Invalid Artifact Selection Type", USER);
          }

          ArtifactSelectionBuilder artifactSelectionBuilder = ArtifactSelection.builder();
          artifactSelectionBuilder.type(type);
          artifactSelectionBuilder.serviceId(e.getServiceId());
          artifactSelectionBuilder.artifactStreamId(e.getArtifactSourceId());
          artifactSelectionBuilder.artifactFilter(e.getArtifactFilter());
          artifactSelectionBuilder.workflowId(e.getWorkflowId());
          artifactSelectionBuilder.pipelineId(e.getPipelineId());

          if (e.getRegex() != null) {
            artifactSelectionBuilder.regex(e.getRegex());
          }

          return artifactSelectionBuilder.build();
        })
        .collect(Collectors.toList());
  }

  Type validateAndResolveFromTriggeringArtifactArtifactSelectionType(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    if (QLConditionType.ON_NEW_ARTIFACT != qlCreateOrUpdateTriggerInput.getCondition().getConditionType()) {
      throw new InvalidRequestException(
          "FROM_TRIGGERING_ARTIFACT can be used only with ON_NEW_ARTIFACT Condition Type", USER);
    }
    return ArtifactSelection.Type.ARTIFACT_SOURCE;
  }

  Type validateAndResolveFromTriggeringPipelineArtifactSelectionType(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    if (QLConditionType.ON_PIPELINE_COMPLETION != qlCreateOrUpdateTriggerInput.getCondition().getConditionType()) {
      throw new InvalidRequestException(
          "FROM_TRIGGERING_PIPELINE can be used only with ON_PIPELINE_COMPLETION Condition Type", USER);
    }
    return Type.PIPELINE_SOURCE;
  }

  Type validateAndResolveLastCollectedArtifactSelectionType(QLArtifactSelectionInput qlArtifactSelectionInput) {
    validateArtifactSource(qlArtifactSelectionInput);
    return Type.LAST_COLLECTED;
  }

  Type validateAndResolveFromPayloadSourceArtifactSelectionType(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, QLArtifactSelectionInput qlArtifactSelectionInput) {
    if (QLConditionType.ON_WEBHOOK != qlCreateOrUpdateTriggerInput.getCondition().getConditionType()) {
      throw new InvalidRequestException("FROM_PAYLOAD_SOURCE can be used only with ON_WEBHOOK Condition Type", USER);
    }
    if (QLWebhookSource.CUSTOM
        != qlCreateOrUpdateTriggerInput.getCondition().getWebhookConditionInput().getWebhookSourceType()) {
      throw new InvalidRequestException("FROM_PAYLOAD_SOURCE can be used only with CUSTOM Webhook Event", USER);
    }
    validateArtifactSource(qlArtifactSelectionInput);
    return ArtifactSelection.Type.WEBHOOK_VARIABLE;
  }

  Type validateAndResolveLastDeployedPipelineArtifactSelectionType(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    WorkflowType workflowType = resolveWorkflowType(qlCreateOrUpdateTriggerInput);
    if (workflowType != WorkflowType.PIPELINE) {
      throw new InvalidRequestException("Artifact Selection is not allowed for current Workflow Type", USER);
    }
    validatePipeline(qlCreateOrUpdateTriggerInput, qlCreateOrUpdateTriggerInput.getAction().getEntityId());
    return ArtifactSelection.Type.LAST_DEPLOYED;
  }

  Type validateAndResolveLastDeployedWorkflowArtifactSelectionType(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    WorkflowType workflowType = resolveWorkflowType(qlCreateOrUpdateTriggerInput);
    if (workflowType != WorkflowType.ORCHESTRATION) {
      throw new InvalidRequestException("Artifact Selection is not allowed for current Workflow Type", USER);
    }
    validateWorkflow(qlCreateOrUpdateTriggerInput,
        qlCreateOrUpdateTriggerInput.getAction().getArtifactSelections().get(0).getWorkflowId());
    return ArtifactSelection.Type.LAST_DEPLOYED;
  }

  public WorkflowType resolveWorkflowType(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    WorkflowType workflowType = null;
    if (QLExecutionType.WORKFLOW == qlCreateOrUpdateTriggerInput.getAction().getExecutionType()) {
      workflowType = WorkflowType.ORCHESTRATION;
    } else {
      workflowType = WorkflowType.PIPELINE;
    }
    return workflowType;
  }

  boolean resolveContinueWithDefault(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    if (resolveWorkflowType(qlCreateOrUpdateTriggerInput) == WorkflowType.PIPELINE) {
      Boolean continueWithDefault = qlCreateOrUpdateTriggerInput.getAction().getContinueWithDefaultValues();
      return Boolean.TRUE.equals(continueWithDefault);
    }
    return false;
  }

  Map<String, String> validateAndResolvePipelineVariables(
      List<QLVariableInput> qlVariables, Pipeline pipeline, String envId) {
    if (qlVariables == null) {
      qlVariables = new ArrayList<>();
    }
    return pipelineExecutionController.validateAndResolvePipelineVariables(
        pipeline, qlVariables, envId, new ArrayList<>(), true);
  }

  Map<String, String> validateAndResolveWorkflowVariables(
      List<QLVariableInput> qlVariables, Workflow workflow, String envId) {
    if (qlVariables == null) {
      qlVariables = new ArrayList<>();
    }

    return workflowExecutionController.validateAndResolveWorkflowVariables(
        workflow, qlVariables, envId, new ArrayList<>(), true);
  }

  void validatePipeline(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, String pipelineId) {
    Pipeline pipeline = null;
    String appId = qlCreateOrUpdateTriggerInput.getApplicationId();

    if (appId == null) {
      throw new InvalidRequestException("Application Id must not be empty", USER);
    }
    if (pipelineId == null) {
      throw new InvalidRequestException("Pipeline Id must not be empty", USER);
    }

    pipeline = pipelineService.readPipeline(appId, pipelineId, false);

    if (pipeline != null) {
      if (!pipeline.getAppId().equals(appId)) {
        throw new InvalidRequestException("Pipeline doesn't belong to this application", USER);
      }
    } else {
      throw new InvalidRequestException("Pipeline doesn't exist", USER);
    }
  }

  void validateWorkflow(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, String workflowId) {
    Workflow workflow = null;
    String appId = qlCreateOrUpdateTriggerInput.getApplicationId();
    QLTriggerActionInput qlTriggerActionInput = qlCreateOrUpdateTriggerInput.getAction();

    if (qlTriggerActionInput != null) {
      workflow = workflowService.readWorkflow(appId, workflowId);
    }

    if (workflow != null) {
      if (!workflow.getAppId().equals(appId)) {
        throw new InvalidRequestException("Workflow doesn't belong to this application", USER);
      }
    } else {
      throw new InvalidRequestException("Workflow doesn't exist", USER);
    }
  }

  private void validateArtifactSource(QLArtifactSelectionInput qlArtifactSelectionInput) {
    String artifactSourceId = qlArtifactSelectionInput.getArtifactSourceId();

    if (isEmpty(artifactSourceId)) {
      throw new InvalidRequestException("Artifact Source must not be null", USER);
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifactSourceId);
    if (artifactStream == null) {
      throw new InvalidRequestException("Artifact Stream for given id does not exist. Id: " + artifactSourceId, USER);
    }
    if (!qlArtifactSelectionInput.getServiceId().equals(artifactStream.getServiceId())) {
      throw new InvalidRequestException(
          "Artifact Source does not belong to the service. Service: " + qlArtifactSelectionInput.getServiceId(), USER);
    }
  }

  void validateAndSetManifestSelectionsPipeline(Map<String, String> variables,
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, Pipeline pipeline, TriggerBuilder triggerBuilder) {
    /* Fetch the deployment data to find out the required entity types */
    List<QLManifestSelectionInput> manifestSelections =
        qlCreateOrUpdateTriggerInput.getAction().getManifestSelections();
    DeploymentMetadata deploymentMetadata = pipelineService.fetchDeploymentMetadata(
        pipeline.getAppId(), pipeline.getUuid(), variables, null, null, false, null);

    List<String> manifestNeededServiceIds =
        deploymentMetadata == null ? new ArrayList<>() : deploymentMetadata.getManifestRequiredServiceIds();
    checkIfManifestSelectionPresentForRequiredServices(manifestSelections, manifestNeededServiceIds);
    triggerBuilder.manifestSelections(
        resolveManifestSelections(qlCreateOrUpdateTriggerInput, manifestNeededServiceIds));
  }

  void validateAndSetManifestSelectionsWorkflow(Map<String, String> variables,
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, Workflow workflow, TriggerBuilder triggerBuilder) {
    List<QLManifestSelectionInput> manifestSelections =
        qlCreateOrUpdateTriggerInput.getAction().getManifestSelections();
    DeploymentMetadata deploymentMetadata = workflowService.fetchDeploymentMetadata(
        workflow.getAppId(), workflow, variables, null, null, DeploymentMetadata.Include.ARTIFACT_SERVICE);

    List<String> manifestNeededServiceIds =
        deploymentMetadata == null ? new ArrayList<>() : deploymentMetadata.getManifestRequiredServiceIds();
    checkIfManifestSelectionPresentForRequiredServices(manifestSelections, manifestNeededServiceIds);
    triggerBuilder.manifestSelections(
        resolveManifestSelections(qlCreateOrUpdateTriggerInput, manifestNeededServiceIds));
  }

  List<ManifestSelection> resolveManifestSelections(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, List<String> manifestNeededServiceIds) {
    if (qlCreateOrUpdateTriggerInput.getAction().getManifestSelections() == null || isEmpty(manifestNeededServiceIds)) {
      return new ArrayList<>();
    }

    return qlCreateOrUpdateTriggerInput.getAction()
        .getManifestSelections()
        .stream()
        .map(selection -> {
          validateManifestSelections(
              selection, qlCreateOrUpdateTriggerInput.getApplicationId(), qlCreateOrUpdateTriggerInput);
          String entityId = qlCreateOrUpdateTriggerInput.getAction().getExecutionType() == QLExecutionType.WORKFLOW
              ? selection.getWorkflowId()
              : selection.getPipelineId();
          return ManifestSelection.builder()
              .serviceId(selection.getServiceId())
              .workflowId(entityId)
              .type(selection.getManifestSelectionType())
              .pipelineId(entityId)
              .versionRegex(selection.getVersionRegex())
              .build();
        })
        .collect(Collectors.toList());
  }

  private void validateManifestSelections(
      QLManifestSelectionInput selectionInput, String appId, QLCreateOrUpdateTriggerInput triggerInput) {
    if (isEmpty(selectionInput.getServiceId())) {
      throw new InvalidRequestException("Empty serviceId in Manifest selection", USER);
    }
    if (serviceResourceService.get(appId, selectionInput.getServiceId()) == null) {
      throw new InvalidRequestException(
          "ServiceId mentioned in Manifest Selection doesn't exist. ServiceId: " + selectionInput.getServiceId(), USER);
    }

    QLConditionType triggerType = triggerInput.getCondition().getConditionType();

    switch (selectionInput.getManifestSelectionType()) {
      case FROM_APP_MANIFEST:
        if (triggerType != QLConditionType.ON_NEW_MANIFEST) {
          throw new InvalidRequestException(
              "FROM_APP_MANIFEST can be used only with ON_NEW_MANIFEST Condition Type", USER);
        }
        break;
      case PIPELINE_SOURCE:
        if (triggerType != QLConditionType.ON_PIPELINE_COMPLETION) {
          throw new InvalidRequestException(
              "PIPELINE_SOURCE can be used only with ON_PIPELINE_COMPLETION Condition Type", USER);
        }
        break;
      case LAST_COLLECTED:
        break;
      case LAST_DEPLOYED:
        validateLastDeployedManifestSelection(selectionInput, triggerInput);
        break;
      case WEBHOOK_VARIABLE:
        if (QLConditionType.ON_WEBHOOK != triggerType) {
          throw new InvalidRequestException("WEBHOOK_VARIABLE can be used only with ON_WEBHOOK Condition Type", USER);
        }
        if (QLWebhookSource.CUSTOM != triggerInput.getCondition().getWebhookConditionInput().getWebhookSourceType()) {
          throw new InvalidRequestException("WEBHOOK_VARIABLE can be used only with CUSTOM Webhook Event", USER);
        }
        break;
      default:
        throw new InvalidRequestException(
            "Unsupported manifest selection type: " + selectionInput.getManifestSelectionType(), USER);
    }
  }

  private void validateLastDeployedManifestSelection(
      QLManifestSelectionInput selectionInput, QLCreateOrUpdateTriggerInput triggerInput) {
    if (resolveWorkflowType(triggerInput) == WorkflowType.ORCHESTRATION) {
      if (isEmpty(selectionInput.getWorkflowId())) {
        throw new InvalidRequestException(
            "WorkflowId is required for Last Deployed manifest selection with workflow action");
      }
      validateWorkflow(triggerInput, selectionInput.getWorkflowId());
    } else {
      if (isEmpty(selectionInput.getPipelineId())) {
        throw new InvalidRequestException(
            "PipelineId is required for Last Deployed manifest selection with pipeline action");
      }
      validatePipeline(triggerInput, selectionInput.getPipelineId());
    }
  }

  private void checkIfManifestSelectionPresentForRequiredServices(
      List<QLManifestSelectionInput> manifestSelections, List<String> manifestNeededServiceIds) {
    if (manifestSelections == null) {
      manifestSelections = new ArrayList<>();
    }

    List<String> providedServiceIds =
        manifestSelections.stream().map(QLManifestSelectionInput::getServiceId).collect(Collectors.toList());
    manifestNeededServiceIds.stream()
        .filter(serviceId -> !providedServiceIds.contains(serviceId))
        .findFirst()
        .ifPresent(serviceId -> {
          throw new InvalidRequestException(
              String.format("Manifest selection for service id: %s must be specified", serviceId), USER);
        });
  }
}

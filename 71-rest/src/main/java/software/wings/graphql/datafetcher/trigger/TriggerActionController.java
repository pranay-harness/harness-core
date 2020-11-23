package software.wings.graphql.datafetcher.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactSelection.ArtifactSelectionBuilder;
import software.wings.beans.trigger.ArtifactSelection.Type;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.execution.PipelineExecutionController;
import software.wings.graphql.datafetcher.execution.WorkflowExecutionController;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.graphql.schema.type.trigger.QLArtifactSelection;
import software.wings.graphql.schema.type.trigger.QLArtifactSelectionInput;
import software.wings.graphql.schema.type.trigger.QLConditionType;
import software.wings.graphql.schema.type.trigger.QLCreateOrUpdateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLFromTriggeringArtifactSource;
import software.wings.graphql.schema.type.trigger.QLFromTriggeringPipeline;
import software.wings.graphql.schema.type.trigger.QLFromWebhookPayload;
import software.wings.graphql.schema.type.trigger.QLLastCollected;
import software.wings.graphql.schema.type.trigger.QLLastDeployedFromPipeline;
import software.wings.graphql.schema.type.trigger.QLLastDeployedFromWorkflow;
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
                          .variables(variableValues)
                          .continueWithDefaultValues(trigger.isContinueWithDefaultValues())
                          .build();
    } else {
      triggerAction = QLWorkflowAction.builder()
                          .workflowId(trigger.getWorkflowId())
                          .workflowName(trigger.getWorkflowName())
                          .artifactSelections(artifactSelections)
                          .variables(variableValues)
                          .build();
    }
    return triggerAction;
  }

  List<ArtifactSelection> resolveArtifactSelections(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput, List<String> artifactNeededServiceIds) {
    if (qlCreateOrUpdateTriggerInput.getAction().getArtifactSelections() == null
        || EmptyPredicate.isEmpty(artifactNeededServiceIds)) {
      return new ArrayList<>();
    }

    return qlCreateOrUpdateTriggerInput.getAction()
        .getArtifactSelections()
        .stream()
        .map(e -> {
          if (EmptyPredicate.isEmpty(e.getServiceId())) {
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
              if (EmptyPredicate.isEmpty(e.getArtifactSourceId())) {
                throw new InvalidRequestException(
                    "Artifact Source Id to select artifact from is required when using LAST_COLLECTED", USER);
              }
              type = validateAndResolveLastCollectedArtifactSelectionType(e);
              break;
            case FROM_PAYLOAD_SOURCE:
              if (EmptyPredicate.isEmpty(e.getArtifactSourceId())) {
                throw new InvalidRequestException(
                    "Artifact Source Id to select artifact from is required when using FROM_PAYLOAD_SOURCE", USER);
              }
              type = validateAndResolveFromPayloadSourceArtifactSelectionType(qlCreateOrUpdateTriggerInput, e);
              break;
            case LAST_DEPLOYED_PIPELINE:
              if (EmptyPredicate.isEmpty(e.getPipelineId())) {
                throw new InvalidRequestException(
                    "Pipeline Id to select artifact from is required when using LAST_DEPLOYED_PIPELINE", USER);
              }
              type = validateAndResolveLastDeployedPipelineArtifactSelectionType(qlCreateOrUpdateTriggerInput);
              break;
            case LAST_DEPLOYED_WORKFLOW:
              if (EmptyPredicate.isEmpty(e.getWorkflowId())) {
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
    validateWorkflow(qlCreateOrUpdateTriggerInput);
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

  private void validateWorkflow(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    Workflow workflow = null;
    String appId = qlCreateOrUpdateTriggerInput.getApplicationId();
    QLTriggerActionInput qlTriggerActionInput = qlCreateOrUpdateTriggerInput.getAction();

    if (qlTriggerActionInput != null) {
      workflow =
          workflowService.readWorkflow(appId, qlTriggerActionInput.getArtifactSelections().get(0).getWorkflowId());
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

    if (EmptyPredicate.isEmpty(artifactSourceId)) {
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
}

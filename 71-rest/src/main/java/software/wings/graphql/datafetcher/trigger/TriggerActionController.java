package software.wings.graphql.datafetcher.trigger;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import lombok.experimental.UtilityClass;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactSelection.Type;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.execution.PipelineExecutionController;
import software.wings.graphql.datafetcher.execution.WorkflowExecutionController;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.graphql.schema.type.trigger.QLArtifactSelection;
import software.wings.graphql.schema.type.trigger.QLArtifactSelectionInput;
import software.wings.graphql.schema.type.trigger.QLConditionType;
import software.wings.graphql.schema.type.trigger.QLCreateTriggerInput;
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
import software.wings.security.PermissionAttribute;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class TriggerActionController {
  public QLTriggerAction populateTriggerAction(Trigger trigger) {
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

  public List<ArtifactSelection> resolveArtifactSelections(QLCreateTriggerInput qlCreateTriggerInput,
      PipelineService pipelineService, WorkflowService workflowService, ServiceResourceService serviceResourceService,
      ArtifactStreamService artifactStreamService) {
    if (qlCreateTriggerInput.getAction().getArtifactSelections() == null) {
      return new ArrayList<>();
    }

    return qlCreateTriggerInput.getAction()
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
              type = validateAndResolveFromTriggeringArtifactArtifactSelectionType(qlCreateTriggerInput);
              break;
            case FROM_TRIGGERING_PIPELINE:
              type = validateAndResolveFromTriggeringPipelineArtifactSelectionType(qlCreateTriggerInput);
              break;
            case LAST_COLLECTED:
              type = validateAndResolveLastCollectedArtifactSelectionType(e, artifactStreamService);
              break;
            case FROM_PAYLOAD_SOURCE:
              type = validateAndResolveFromPayloadSourceArtifactSelectionType(
                  qlCreateTriggerInput, e, artifactStreamService);
              break;
            case LAST_DEPLOYED_PIPELINE:
              type = validateAndResolveLastDeployedPipelineArtifactSelectionType(qlCreateTriggerInput, pipelineService);
              break;
            case LAST_DEPLOYED_WORKFLOW:
              type = validateAndResolveLastDeployedWorkflowArtifactSelectionType(qlCreateTriggerInput, workflowService);
              break;
            default:
              throw new InvalidRequestException("Invalid Artifact Selection Type", USER);
          }

          return ArtifactSelection.builder()
              .type(type)
              .serviceId(e.getServiceId())
              .artifactStreamId(e.getArtifactSourceId())
              .artifactFilter(e.getArtifactFilter())
              .regex(e.getRegex())
              .workflowId(e.getWorkflowId())
              .pipelineId(e.getPipelineId())
              .build();
        })
        .collect(Collectors.toList());
  }

  private Type validateAndResolveFromTriggeringArtifactArtifactSelectionType(
      QLCreateTriggerInput qlCreateTriggerInput) {
    if (QLConditionType.ON_NEW_ARTIFACT != qlCreateTriggerInput.getCondition().getConditionType()) {
      throw new InvalidRequestException(
          "FROM_TRIGGERING_ARTIFACT can be used only with ON_NEW_ARTIFACT Condition Type", USER);
    }
    return ArtifactSelection.Type.ARTIFACT_SOURCE;
  }

  private Type validateAndResolveFromTriggeringPipelineArtifactSelectionType(
      QLCreateTriggerInput qlCreateTriggerInput) {
    if (QLConditionType.ON_PIPELINE_COMPLETION != qlCreateTriggerInput.getCondition().getConditionType()) {
      throw new InvalidRequestException(
          "FROM_TRIGGERING_PIPELINE can be used only with ON_PIPELINE_COMPLETION Condition Type", USER);
    }
    return Type.PIPELINE_SOURCE;
  }

  private Type validateAndResolveLastCollectedArtifactSelectionType(
      QLArtifactSelectionInput qlArtifactSelectionInput, ArtifactStreamService artifactStreamService) {
    validateArtifactSource(qlArtifactSelectionInput, artifactStreamService);
    return Type.LAST_COLLECTED;
  }

  private Type validateAndResolveFromPayloadSourceArtifactSelectionType(QLCreateTriggerInput qlCreateTriggerInput,
      QLArtifactSelectionInput qlArtifactSelectionInput, ArtifactStreamService artifactStreamService) {
    if (QLConditionType.ON_WEBHOOK != qlCreateTriggerInput.getCondition().getConditionType()) {
      throw new InvalidRequestException("FROM_PAYLOAD_SOURCE can be used only with ON_WEBHOOK Condition Type", USER);
    }
    if (QLWebhookSource.CUSTOM
        != qlCreateTriggerInput.getCondition().getWebhookConditionInput().getWebhookSourceType()) {
      throw new InvalidRequestException("FROM_PAYLOAD_SOURCE can be used only with CUSTOM Webhook Event", USER);
    }
    validateArtifactSource(qlArtifactSelectionInput, artifactStreamService);
    return ArtifactSelection.Type.WEBHOOK_VARIABLE;
  }

  private Type validateAndResolveLastDeployedPipelineArtifactSelectionType(
      QLCreateTriggerInput qlCreateTriggerInput, PipelineService pipelineService) {
    WorkflowType workflowType = resolveWorkflowType(qlCreateTriggerInput);
    if (workflowType != WorkflowType.PIPELINE) {
      throw new InvalidRequestException("Artifact Selection is not allowed for current Workflow Type", USER);
    }
    validatePipeline(qlCreateTriggerInput, pipelineService);
    return ArtifactSelection.Type.LAST_DEPLOYED;
  }

  private Type validateAndResolveLastDeployedWorkflowArtifactSelectionType(
      QLCreateTriggerInput qlCreateTriggerInput, WorkflowService workflowService) {
    WorkflowType workflowType = resolveWorkflowType(qlCreateTriggerInput);
    if (workflowType != WorkflowType.ORCHESTRATION) {
      throw new InvalidRequestException("Artifact Selection is not allowed for current Workflow Type", USER);
    }
    validateWorkflow(qlCreateTriggerInput, workflowService);
    return ArtifactSelection.Type.LAST_DEPLOYED;
  }

  public WorkflowType resolveWorkflowType(QLCreateTriggerInput qlCreateTriggerInput) {
    WorkflowType workflowType = null;
    if (QLExecutionType.WORKFLOW == qlCreateTriggerInput.getAction().getExecutionType()) {
      workflowType = WorkflowType.ORCHESTRATION;
    } else {
      workflowType = WorkflowType.PIPELINE;
    }
    return workflowType;
  }

  public Map<String, String> resolveWorkflowVariables(QLCreateTriggerInput triggerInput,
      PipelineService pipelineService, PipelineExecutionController pipelineExecutionController,
      WorkflowExecutionController workflowExecutionController, WorkflowService workflowService,
      AuthHandler authHandler) {
    QLTriggerActionInput triggerActionInput = triggerInput.getAction();

    List<QLVariableInput> qlVariables = triggerActionInput.getVariables();

    String appId = triggerInput.getApplicationId();

    PermissionAttribute permissionAttribute =
        new PermissionAttribute(PermissionAttribute.PermissionType.DEPLOYMENT, PermissionAttribute.Action.EXECUTE);
    List<PermissionAttribute> permissionAttributes = Collections.singletonList(permissionAttribute);

    Map<String, String> workflowVariables = new HashMap<>();
    switch (triggerActionInput.getExecutionType()) {
      case PIPELINE:
        workflowVariables = validateAndResolvePipelineVariables(triggerActionInput, permissionAttributes, qlVariables,
            appId, pipelineService, pipelineExecutionController, authHandler);
        break;
      case WORKFLOW:
        workflowVariables = validateAndResolveWorkflowVariables(triggerActionInput, permissionAttributes, qlVariables,
            appId, workflowExecutionController, workflowService, authHandler);
        break;
      default:
    }

    for (QLVariableInput qlVariableInput : qlVariables) {
      workflowVariables.put(
          qlVariableInput.getVariableValue().getType().getStringValue(), qlVariableInput.getVariableValue().getValue());
    }

    return workflowVariables;
  }

  private Map<String, String> validateAndResolvePipelineVariables(QLTriggerActionInput qlTriggerActionInput,
      List<PermissionAttribute> permissionAttributes, List<QLVariableInput> qlVariables, String appId,
      PipelineService pipelineService, PipelineExecutionController pipelineExecutionController,
      AuthHandler authHandler) {
    String pipelineId = qlTriggerActionInput.getEntityId();
    Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, false);
    notNullCheck("Pipeline " + pipelineId + " doesn't exist in the specified application " + appId, pipeline, USER);
    authHandler.authorize(permissionAttributes, Collections.singletonList(appId), pipelineId);

    String envId = pipelineExecutionController.resolveEnvId(pipeline, qlVariables);

    return pipelineExecutionController.validateAndResolvePipelineVariables(
        pipeline, qlVariables, envId, new ArrayList<>(), true);
  }

  private Map<String, String> validateAndResolveWorkflowVariables(QLTriggerActionInput qlTriggerActionInput,
      List<PermissionAttribute> permissionAttributes, List<QLVariableInput> qlVariables, String appId,
      WorkflowExecutionController workflowExecutionController, WorkflowService workflowService,
      AuthHandler authHandler) {
    String workflowId = qlTriggerActionInput.getEntityId();
    Workflow workflow = workflowService.readWorkflow(appId, workflowId);
    notNullCheck("Workflow " + workflowId + " doesn't exist in the specified application " + appId, workflow, USER);
    notNullCheck(
        "Error reading workflow " + workflowId + " Might be deleted", workflow.getOrchestrationWorkflow(), USER);

    authHandler.authorize(permissionAttributes, Collections.singletonList(appId), workflowId);
    String envId = workflowExecutionController.resolveEnvId(workflow, qlVariables);
    authHandler.checkIfUserAllowedToDeployToEnv(appId, envId);

    return workflowExecutionController.validateAndResolveWorkflowVariables(
        workflow, qlVariables, envId, new ArrayList<>(), true);
  }

  public void validatePipeline(QLCreateTriggerInput qlCreateTriggerInput, PipelineService pipelineService) {
    Pipeline pipeline = null;
    String appId = qlCreateTriggerInput.getApplicationId();
    String pipelineId = qlCreateTriggerInput.getAction().getEntityId();

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

  private void validateWorkflow(QLCreateTriggerInput qlCreateTriggerInput, WorkflowService workflowService) {
    Workflow workflow = null;
    String appId = qlCreateTriggerInput.getApplicationId();
    QLTriggerActionInput qlTriggerActionInput = qlCreateTriggerInput.getAction();

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

  private void validateArtifactSource(
      QLArtifactSelectionInput qlArtifactSelectionInput, ArtifactStreamService artifactStreamService) {
    String artifactSourceid = qlArtifactSelectionInput.getArtifactSourceId();

    if (EmptyPredicate.isEmpty(artifactSourceid)) {
      throw new InvalidRequestException("Artifact Source must not be null", USER);
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifactSourceid);
    if (artifactStream == null) {
      throw new InvalidRequestException("Artifact Stream for given id does not exist. Id: " + artifactSourceid, USER);
    }
    if (!qlArtifactSelectionInput.getServiceId().equals(artifactStream.getServiceId())) {
      throw new InvalidRequestException(
          "Artifact Source does not belong to the service. Service: " + qlArtifactSelectionInput.getServiceId(), USER);
    }
  }
}

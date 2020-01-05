package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static java.util.Arrays.asList;
import static software.wings.api.EnvStateExecutionData.Builder.anEnvStateExecutionData;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.api.EnvStateExecutionData;
import software.wings.api.ServiceArtifactElement;
import software.wings.api.ServiceArtifactVariableElement;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.EntityType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.impl.workflow.WorkflowServiceImpl;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.stencils.Expand;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * A Env state to pause state machine execution.
 *
 * @author Rishi
 */
@Attributes(title = "Env")
@Slf4j
@FieldNameConstants(innerTypeName = "EnvStateKeys")
public class EnvState extends State {
  public static final Integer ENV_STATE_TIMEOUT_MILLIS = 7 * 24 * 60 * 60 * 1000;

  @Expand(dataProvider = EnvironmentServiceImpl.class)
  @EnumData(enumDataProvider = EnvironmentServiceImpl.class)
  @Attributes(required = true, title = "Environment")
  @Setter
  private String envId;

  @EnumData(enumDataProvider = WorkflowServiceImpl.class)
  @Attributes(required = true, title = "Workflow")
  @Setter
  private String workflowId;

  @Setter @SchemaIgnore private String pipelineId;
  @Setter @SchemaIgnore private String pipelineStageElementId;
  @Setter @SchemaIgnore private int pipelineStageParallelIndex;
  @Setter @SchemaIgnore private String stageName;

  @JsonIgnore @SchemaIgnore private Map<String, String> workflowVariables;

  @Transient @Inject private WorkflowService workflowService;
  @Transient @Inject private WorkflowExecutionService executionService;
  @Transient @Inject private ArtifactService artifactService;
  @Transient @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Transient @Inject private FeatureFlagService featureFlagService;

  @Getter @Setter @JsonIgnore private boolean disable;

  @Getter @Setter private String disableAssertion;

  public EnvState(String name) {
    super(name, StateType.ENV_STATE.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    if (disableAssertion != null && disableAssertion.equals("true")) {
      return ExecutionResponse.builder()
          .executionStatus(SKIPPED)
          .errorMessage(getName() + " step in " + context.getPipelineStageName() + " has been skipped")
          .stateExecutionData(anEnvStateExecutionData().build())
          .build();
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    Workflow workflow = workflowService.readWorkflowWithoutServices(appId, workflowId);
    EnvStateExecutionData envStateExecutionData = anEnvStateExecutionData().withWorkflowId(workflowId).build();
    if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
      return ExecutionResponse.builder()
          .executionStatus(FAILED)
          .errorMessage("Workflow does not exist")
          .stateExecutionData(envStateExecutionData)
          .build();
    }

    if (isNotEmpty(disableAssertion)) {
      try {
        boolean assertionResult = (boolean) context.evaluateExpression(
            disableAssertion, StateExecutionContext.builder().stateExecutionData(envStateExecutionData).build());
        if (assertionResult) {
          return ExecutionResponse.builder()
              .executionStatus(SKIPPED)
              .errorMessage(getName() + " step in " + context.getPipelineStageName()
                  + " has been skipped based on assertion expression [" + disableAssertion + "]")
              .stateExecutionData(envStateExecutionData)
              .build();
        }
      } catch (Exception e) {
        logger.error("Skip Assertion Evaluation Failed", e);
        return ExecutionResponse.builder()
            .executionStatus(FAILED)
            .errorMessage("Skip Assertion Evaluation Failed : " + e.getMessage())
            .stateExecutionData(envStateExecutionData)
            .build();
      }
    }

    List<Artifact> artifacts = ((DeploymentExecutionContext) context).getArtifacts();
    List<ArtifactVariable> artifactVariables =
        getArtifactVariables((DeploymentExecutionContext) context, workflowStandardParams);

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setOrchestrationId(workflowId);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setArtifactVariables(artifactVariables);
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    executionArgs.setTriggeredFromPipeline(true);
    executionArgs.setPipelineId(pipelineId);
    executionArgs.setPipelinePhaseElementId(context.getPipelineStageElementId());
    executionArgs.setPipelinePhaseParallelIndex(context.getPipelineStageParallelIndex());
    executionArgs.setStageName(context.getPipelineStageName());
    executionArgs.setWorkflowVariables(populatePipelineVariables(workflow, workflowStandardParams));
    executionArgs.setExcludeHostsWithSameArtifact(workflowStandardParams.isExcludeHostsWithSameArtifact());
    executionArgs.setNotifyTriggeredUserOnly(workflowStandardParams.isNotifyTriggeredUserOnly());

    envStateExecutionData.setOrchestrationWorkflowType(
        workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType());
    try {
      WorkflowExecution execution = executionService.triggerOrchestrationExecution(
          appId, envId, workflowId, context.getWorkflowExecutionId(), executionArgs, null);
      envStateExecutionData.setWorkflowExecutionId(execution.getUuid());
      return ExecutionResponse.builder()
          .async(true)
          .correlationIds(asList(execution.getUuid()))
          .stateExecutionData(envStateExecutionData)
          .build();
    } catch (Exception e) {
      String message = ExceptionUtils.getMessage(e);
      return ExecutionResponse.builder()
          .executionStatus(FAILED)
          .errorMessage(message)
          .stateExecutionData(envStateExecutionData)
          .build();
    }
  }

  private List<ArtifactVariable> getArtifactVariables(
      DeploymentExecutionContext context, WorkflowStandardParams workflowStandardParams) {
    List<ArtifactVariable> artifactVariables;
    List<ContextElement> contextElementList = context.getContextElementList(ContextElementType.ARTIFACT_VARIABLE);
    if (isEmpty(contextElementList)) {
      artifactVariables = workflowStandardParams.getWorkflowElement().getArtifactVariables();
      return isEmpty(artifactVariables) ? new ArrayList<>() : artifactVariables;
    }

    artifactVariables = new ArrayList<>();
    Map<String, ArtifactVariable> workflowVariablesMap = new HashMap<>();
    for (ContextElement contextElement : contextElementList) {
      ServiceArtifactVariableElement savElement = (ServiceArtifactVariableElement) contextElement;
      Artifact artifact = artifactService.get(savElement.getUuid());
      if (artifact == null) {
        continue;
      }

      List<String> allowedList = new ArrayList<>();
      allowedList.add(artifact.getArtifactStreamId());

      EntityType entityType = savElement.getEntityType();
      String entityId = savElement.getEntityId();
      if (entityType == null) {
        entityType = EntityType.SERVICE;
        entityId = savElement.getServiceId();
      }

      switch (entityType) {
        case WORKFLOW:
          // Skip if workflow ids are different.
          if (!workflowId.equals(entityId)) {
            continue;
          }
          break;
        case ENVIRONMENT:
        case SERVICE:
          // Skip if entity id is blank.
          // envId/serviceId is not checked with workflow envIds/serviceIds here, it is checked in
          // WorkflowExecutionService.populateArtifacts.
          if (StringUtils.isBlank(savElement.getEntityId())) {
            continue;
          }
          break;
        default:
          // Skip any other entity type.
          continue;
      }

      ArtifactVariable artifactVariable = ArtifactVariable.builder()
                                              .type(VariableType.ARTIFACT)
                                              .name(savElement.getArtifactVariableName())
                                              .value(savElement.getUuid())
                                              .entityType(entityType)
                                              .entityId(entityId)
                                              .allowedList(allowedList)
                                              .build();

      if (EntityType.ENVIRONMENT.equals(entityType)) {
        List<ArtifactVariable> overriddenArtifactVariables = new ArrayList<>();
        if (!StringUtils.isBlank(savElement.getServiceId())) {
          overriddenArtifactVariables.add(ArtifactVariable.builder()
                                              .type(VariableType.ARTIFACT)
                                              .name(savElement.getArtifactVariableName())
                                              .value(savElement.getUuid())
                                              .entityType(EntityType.SERVICE)
                                              .entityId(savElement.getServiceId())
                                              .allowedList(allowedList)
                                              .build());
        }
        artifactVariable.setOverriddenArtifactVariables(overriddenArtifactVariables);
      }

      // NOTE: collisions here should be fixed later as we some artifact variables might get filtered out because of
      // envId/serviceId resolved later.
      if (EntityType.WORKFLOW.equals(entityType)) {
        workflowVariablesMap.put(artifactVariable.getName(), artifactVariable);
      } else {
        artifactVariables.add(artifactVariable);
      }
    }

    for (Entry<String, ArtifactVariable> entry : workflowVariablesMap.entrySet()) {
      String name = entry.getKey();
      List<ArtifactVariable> overriddenArtifactVariables =
          artifactVariables.stream()
              .filter(artifactVariable -> artifactVariable.getName().equals(name))
              .collect(Collectors.toList());
      if (isNotEmpty(overriddenArtifactVariables)) {
        artifactVariables.removeIf(artifactVariable -> artifactVariable.getName().equals(name));
      }

      ArtifactVariable artifactVariable = entry.getValue();
      artifactVariable.setOverriddenArtifactVariables(overriddenArtifactVariables);
      artifactVariables.add(artifactVariable);
    }

    return artifactVariables;
  }

  private Map<String, String> populatePipelineVariables(
      Workflow workflow, WorkflowStandardParams workflowStandardParams) {
    return WorkflowServiceHelper.overrideWorkflowVariables(workflow.getOrchestrationWorkflow().getUserVariables(),
        workflowVariables, workflowStandardParams.getWorkflowVariables());
  }

  @Override
  public void parseProperties(Map<String, Object> properties) {
    boolean isDisabled = properties.get(EnvStateKeys.disable) != null && (boolean) properties.get(EnvStateKeys.disable);
    if (isDisabled && properties.get(EnvStateKeys.disableAssertion) == null) {
      properties.put(EnvStateKeys.disableAssertion, "true");
    }
    super.parseProperties(properties);
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {
    if (context == null || context.getStateExecutionData() == null) {
      return;
    }
    context.getStateExecutionData().setErrorMsg(
        "Workflow not completed within " + Misc.getDurationString(getTimeoutMillis()));
    try {
      EnvStateExecutionData envStateExecutionData = (EnvStateExecutionData) context.getStateExecutionData();
      if (envStateExecutionData != null && envStateExecutionData.getWorkflowExecutionId() != null) {
        ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                    .executionInterruptType(ExecutionInterruptType.ABORT_ALL)
                                                    .executionUuid(envStateExecutionData.getWorkflowExecutionId())
                                                    .appId(context.getAppId())
                                                    .build();
        executionService.triggerExecutionInterrupt(executionInterrupt);
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException exception) {
      logger.error("Could not abort workflows.", exception);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    EnvExecutionResponseData responseData = (EnvExecutionResponseData) response.values().iterator().next();
    ExecutionResponseBuilder executionResponseBuilder =
        ExecutionResponse.builder().executionStatus(responseData.getStatus());

    if (responseData.getStatus() != SUCCESS) {
      return executionResponseBuilder.build();
    }

    EnvStateExecutionData stateExecutionData = (EnvStateExecutionData) context.getStateExecutionData();
    if (stateExecutionData.getOrchestrationWorkflowType() == OrchestrationWorkflowType.BUILD) {
      if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, context.getAccountId())) {
        List<Artifact> artifacts =
            executionService.getArtifactsCollected(context.getAppId(), stateExecutionData.getWorkflowExecutionId());
        if (isNotEmpty(artifacts)) {
          List<ContextElement> artifactElements = new ArrayList<>();
          artifacts.forEach(artifact
              -> artifactElements.add(
                  ServiceArtifactElement.builder()
                      .uuid(artifact.getUuid())
                      .name(artifact.getDisplayName())
                      .serviceIds(artifactStreamServiceBindingService.listServiceIds(artifact.getArtifactStreamId()))
                      .build()));
          executionResponseBuilder.contextElements(artifactElements);
        }
      } else {
        List<StateExecutionInstance> allStateExecutionInstances = executionService.getStateExecutionInstances(
            context.getAppId(), stateExecutionData.getWorkflowExecutionId());
        if (isNotEmpty(allStateExecutionInstances)) {
          List<ContextElement> artifactElements = new ArrayList<>();

          allStateExecutionInstances.forEach(stateExecutionInstance -> {
            ArtifactCollectionExecutionData artifactCollectionExecutionData =
                (ArtifactCollectionExecutionData) stateExecutionInstance.fetchStateExecutionData();

            Artifact artifact = artifactService.get(artifactCollectionExecutionData.getArtifactId());
            artifactElements.add(ServiceArtifactVariableElement.builder()
                                     .uuid(artifact.getUuid())
                                     .name(artifact.getDisplayName())
                                     .entityType(artifactCollectionExecutionData.getEntityType())
                                     .entityId(artifactCollectionExecutionData.getEntityId())
                                     .serviceId(artifactCollectionExecutionData.getServiceId())
                                     .artifactVariableName(artifactCollectionExecutionData.getArtifactVariableName())
                                     .build());
          });

          executionResponseBuilder.contextElements(artifactElements);
        }
      }
    }

    return executionResponseBuilder.build();
  }

  public String getEnvId() {
    return envId;
  }
  public String getWorkflowId() {
    return workflowId;
  }
  public String getPipelineId() {
    return pipelineId;
  }
  public String getPipelineStageElementId() {
    return pipelineStageElementId;
  }
  public int getPipelineStageParallelIndex() {
    return pipelineStageParallelIndex;
  }
  public Map<String, String> getWorkflowVariables() {
    return workflowVariables;
  }

  public String getStageName() {
    return stageName;
  }

  @SchemaIgnore
  @Override
  public Integer getTimeoutMillis() {
    if (super.getTimeoutMillis() == null) {
      return ENV_STATE_TIMEOUT_MILLIS;
    }
    return super.getTimeoutMillis();
  }

  public void setWorkflowVariables(Map<String, String> workflowVariables) {
    this.workflowVariables = workflowVariables;
  }

  /**
   * The type Env execution response data.
   */
  public static class EnvExecutionResponseData implements ResponseData {
    private String workflowExecutionId;
    private ExecutionStatus status;

    /**
     * Instantiates a new Env execution response data.
     *
     * @param workflowExecutionId the workflow execution id
     * @param status              the status
     */
    public EnvExecutionResponseData(String workflowExecutionId, ExecutionStatus status) {
      this.workflowExecutionId = workflowExecutionId;
      this.status = status;
    }

    /**
     * Gets workflow execution id.
     *
     * @return the workflow execution id
     */
    public String getWorkflowExecutionId() {
      return workflowExecutionId;
    }

    /**
     * Sets workflow execution id.
     *
     * @param workflowExecutionId the workflow execution id
     */
    public void setWorkflowExecutionId(String workflowExecutionId) {
      this.workflowExecutionId = workflowExecutionId;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public ExecutionStatus getStatus() {
      return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(ExecutionStatus status) {
      this.status = status;
    }
  }
}

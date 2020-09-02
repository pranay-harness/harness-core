package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
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
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.logging.ExceptionLogger;
import io.harness.logging.Misc;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.api.EnvStateExecutionData;
import software.wings.api.artifact.ServiceArtifactElement;
import software.wings.api.artifact.ServiceArtifactElements;
import software.wings.api.artifact.ServiceArtifactVariableElement;
import software.wings.api.artifact.ServiceArtifactVariableElements;
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
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.stencils.Expand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A Env state to pause state machine execution.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@Attributes(title = "Env")
@Slf4j
@FieldNameConstants(innerTypeName = "EnvStateKeys")
public class EnvState extends State {
  public static final Integer ENV_STATE_TIMEOUT_MILLIS = 7 * 24 * 60 * 60 * 1000;

  // NOTE: This field should no longer be used. It contains incorrect/stale values.
  @Deprecated
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
  @Transient @Inject private SweepingOutputService sweepingOutputService;
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
        ExecutionContextImpl contextImpl = (ExecutionContextImpl) context;
        if (contextImpl.getStateExecutionInstance() != null
            && isNotEmpty(contextImpl.getStateExecutionInstance().getContextElements())) {
          WorkflowStandardParams stdParams =
              (WorkflowStandardParams) contextImpl.getStateExecutionInstance().getContextElements().get(0);
          if (stdParams.getWorkflowElement() != null) {
            stdParams.getWorkflowElement().setName(workflow.getName());
            stdParams.getWorkflowElement().setDescription(workflow.getDescription());
          }
        }
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
      } catch (JexlException je) {
        logger.error("Skip Assertion Evaluation Failed", je);
        String jexlError = Optional.ofNullable(je.getMessage()).orElse("");
        if (jexlError.contains(":")) {
          jexlError = jexlError.split(":")[1];
        }
        return ExecutionResponse.builder()
            .executionStatus(FAILED)
            .errorMessage("Skip Assertion Evaluation Failed : " + jexlError)
            .stateExecutionData(envStateExecutionData)
            .build();
      } catch (Exception e) {
        logger.error("Skip Assertion Evaluation Failed", e);
        return ExecutionResponse.builder()
            .executionStatus(FAILED)
            .errorMessage("Skip Assertion Evaluation Failed : " + (e.getMessage() != null ? e.getMessage() : ""))
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
          appId, null, workflowId, context.getWorkflowExecutionId(), executionArgs, null);
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
    List<ServiceArtifactVariableElement> artifactVariableElements = context.getArtifactVariableElements();
    if (isEmpty(artifactVariableElements)) {
      artifactVariables = workflowStandardParams.getWorkflowElement().getArtifactVariables();
      return artifactVariables == null ? new ArrayList<>() : artifactVariables;
    }

    artifactVariables = new ArrayList<>();
    Map<String, ArtifactVariable> workflowVariablesMap = new HashMap<>();
    for (ServiceArtifactVariableElement savElement : artifactVariableElements) {
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

      if (EntityType.ENVIRONMENT == entityType) {
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
      if (EntityType.WORKFLOW == entityType) {
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
        saveArtifactElements(context, stateExecutionData);
      } else {
        saveArtifactVariableElements(context, stateExecutionData);
      }
    }

    return executionResponseBuilder.build();
  }

  private void saveArtifactElements(ExecutionContext context, EnvStateExecutionData stateExecutionData) {
    List<Artifact> artifacts =
        executionService.getArtifactsCollected(context.getAppId(), stateExecutionData.getWorkflowExecutionId());
    if (isEmpty(artifacts)) {
      return;
    }

    List<ServiceArtifactElement> artifactElements = new ArrayList<>();
    artifacts.forEach(artifact
        -> artifactElements.add(
            ServiceArtifactElement.builder()
                .uuid(artifact.getUuid())
                .name(artifact.getDisplayName())
                .serviceIds(artifactStreamServiceBindingService.listServiceIds(artifact.getArtifactStreamId()))
                .build()));

    sweepingOutputService.save(
        context.prepareSweepingOutputBuilder(Scope.PIPELINE)
            .name(ServiceArtifactElements.SWEEPING_OUTPUT_NAME + context.getStateExecutionInstanceId())
            .value(ServiceArtifactElements.builder().artifactElements(artifactElements).build())
            .build());
  }

  private void saveArtifactVariableElements(ExecutionContext context, EnvStateExecutionData stateExecutionData) {
    List<StateExecutionInstance> allStateExecutionInstances =
        executionService.getStateExecutionInstances(context.getAppId(), stateExecutionData.getWorkflowExecutionId());
    if (isEmpty(allStateExecutionInstances)) {
      return;
    }

    List<ServiceArtifactVariableElement> artifactVariableElements = new ArrayList<>();
    allStateExecutionInstances.forEach(stateExecutionInstance -> {
      if (!(stateExecutionInstance.fetchStateExecutionData() instanceof ArtifactCollectionExecutionData)) {
        return;
      }

      ArtifactCollectionExecutionData artifactCollectionExecutionData =
          (ArtifactCollectionExecutionData) stateExecutionInstance.fetchStateExecutionData();
      Artifact artifact = artifactService.get(artifactCollectionExecutionData.getArtifactId());
      artifactVariableElements.add(ServiceArtifactVariableElement.builder()
                                       .uuid(artifact.getUuid())
                                       .name(artifact.getDisplayName())
                                       .entityType(artifactCollectionExecutionData.getEntityType())
                                       .entityId(artifactCollectionExecutionData.getEntityId())
                                       .serviceId(artifactCollectionExecutionData.getServiceId())
                                       .artifactVariableName(artifactCollectionExecutionData.getArtifactVariableName())
                                       .build());
    });

    sweepingOutputService.save(
        context.prepareSweepingOutputBuilder(Scope.PIPELINE)
            .name(ServiceArtifactVariableElements.SWEEPING_OUTPUT_NAME + context.getStateExecutionInstanceId())
            .value(ServiceArtifactVariableElements.builder().artifactVariableElements(artifactVariableElements).build())
            .build());
  }

  @Deprecated
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

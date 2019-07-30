package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static java.util.Arrays.asList;
import static software.wings.api.EnvStateExecutionData.Builder.anEnvStateExecutionData;
import static software.wings.api.ServiceArtifactElement.ServiceArtifactElementBuilder.aServiceArtifactElement;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.common.Constants.ENV_STATE_TIMEOUT_MILLIS;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

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
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.EnvStateExecutionData;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.impl.workflow.WorkflowServiceImpl;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.stencils.Expand;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A Env state to pause state machine execution.
 *
 * @author Rishi
 */
@Attributes(title = "Env")
@Slf4j
@FieldNameConstants(innerTypeName = "EnvStateKeys")
public class EnvState extends State {
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
  @Transient @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  @JsonIgnore private boolean disable;

  public boolean isDisable() {
    return disable;
  }

  public void setDisable(boolean disable) {
    this.disable = disable;
  }

  public EnvState(String name) {
    super(name, StateType.ENV_STATE.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();

    Workflow workflow = workflowService.readWorkflowWithoutServices(appId, workflowId);

    EnvStateExecutionData envStateExecutionData =
        anEnvStateExecutionData().withWorkflowId(workflowId).withEnvId(envId).build();
    if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage("Workflow does not exist")
          .withStateExecutionData(envStateExecutionData)
          .build();
    }

    if (disable) {
      return anExecutionResponse()
          .withExecutionStatus(SKIPPED)
          .withErrorMessage("Workflow [" + workflow.getName() + "] step is disabled. Execution has been skipped.")
          .withStateExecutionData(envStateExecutionData)
          .build();
    }

    List<Artifact> artifacts = ((DeploymentExecutionContext) context).getArtifacts();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setOrchestrationId(workflowId);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setArtifactVariables(workflowStandardParams.getWorkflowElement().getArtifactVariables());
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
      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(asList(execution.getUuid()))
          .withStateExecutionData(envStateExecutionData)
          .build();
    } catch (Exception e) {
      String message = ExceptionUtils.getMessage(e);
      return anExecutionResponse()
          .withExecutionStatus(FAILED)
          .withErrorMessage(message)
          .withStateExecutionData(envStateExecutionData)
          .build();
    }
  }

  private Map<String, String> populatePipelineVariables(
      Workflow workflow, WorkflowStandardParams workflowStandardParams) {
    return WorkflowServiceHelper.overrideWorkflowVariables(workflow.getOrchestrationWorkflow().getUserVariables(),
        workflowVariables, workflowStandardParams.getWorkflowVariables());
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
                                                    .withExecutionInterruptType(ExecutionInterruptType.ABORT_ALL)
                                                    .withExecutionUuid(envStateExecutionData.getWorkflowExecutionId())
                                                    .withAppId(context.getAppId())
                                                    .build();
        executionService.triggerExecutionInterrupt(executionInterrupt);
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException exception) {
      logger.error("Could not abort workflows.", exception);
    }
  }

  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    EnvExecutionResponseData responseData = (EnvExecutionResponseData) response.values().iterator().next();
    ExecutionResponse executionResponse = anExecutionResponse().withExecutionStatus(responseData.getStatus()).build();

    if (responseData.getStatus() != SUCCESS) {
      return executionResponse;
    }

    EnvStateExecutionData stateExecutionData = (EnvStateExecutionData) context.getStateExecutionData();
    if (stateExecutionData.getOrchestrationWorkflowType() == OrchestrationWorkflowType.BUILD) {
      List<Artifact> artifacts =
          executionService.getArtifactsCollected(context.getAppId(), stateExecutionData.getWorkflowExecutionId());
      if (isNotEmpty(artifacts)) {
        List<ContextElement> artifactElements = new ArrayList<>();
        artifacts.forEach(artifact
            -> artifactElements.add(
                aServiceArtifactElement()
                    .withUuid(artifact.getUuid())
                    .withName(artifact.getDisplayName())
                    .withServiceIds(artifactStreamServiceBindingService.listServiceIds(artifact.getArtifactStreamId()))
                    .build()));
        executionResponse.setContextElements(artifactElements);
      }
    }

    return executionResponse;
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

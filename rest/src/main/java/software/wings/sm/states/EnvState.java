package software.wings.sm.states;

import static java.util.Arrays.asList;
import static software.wings.api.EnvStateExecutionData.Builder.anEnvStateExecutionData;
import static software.wings.api.ServiceArtifactElement.ServiceArtifactElementBuilder.aServiceArtifactElement;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.EnvStateExecutionData;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.common.Constants;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.impl.WorkflowServiceImpl;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.stencils.Expand;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A Env state to pause state machine execution.
 *
 * @author Rishi
 */
@Attributes(title = "Env")
public class EnvState extends State {
  @Expand(dataProvider = EnvironmentServiceImpl.class)
  @EnumData(enumDataProvider = EnvironmentServiceImpl.class)
  @Attributes(required = true, title = "Environment")
  private String envId;

  @EnumData(enumDataProvider = WorkflowServiceImpl.class)
  @Attributes(required = true, title = "Workflow")
  private String workflowId;

  @SchemaIgnore private String pipelineId;

  @SchemaIgnore private Map<String, String> workflowVariables;

  @Transient @Inject private WorkflowService workflowService;

  @Transient @Inject private WorkflowExecutionService executionService;

  /**
   * Creates env state with given name.
   *
   * @param name name of the state.
   */
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

    List<Artifact> artifacts = ((DeploymentExecutionContext) context).getArtifacts();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setOrchestrationId(workflowId);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    executionArgs.setTriggeredFromPipeline(true);
    executionArgs.setPipelineId(pipelineId);
    executionArgs.setTriggeredBy(workflowStandardParams.getCurrentUser());
    executionArgs.setWorkflowVariables(populatePipelineVariables(workflowStandardParams));

    Workflow workflow = workflowService.readWorkflow(appId, workflowId);
    EnvStateExecutionData envStateExecutionData =
        anEnvStateExecutionData().withWorkflowId(workflowId).withEnvId(envId).build();
    if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage("Workflow does not exist")
          .withStateExecutionData(envStateExecutionData)
          .build();
    }

    envStateExecutionData.setOrchestrationWorkflowType(
        workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType());
    try {
      WorkflowExecution execution = executionService.triggerOrchestrationExecution(
          appId, envId, workflowId, context.getWorkflowExecutionId(), executionArgs);
      envStateExecutionData.setWorkflowExecutionId(execution.getUuid());
      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(asList(execution.getUuid()))
          .withStateExecutionData(envStateExecutionData)
          .build();
    } catch (Exception e) {
      String message = Misc.getMessage(e);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage(message)
          .withStateExecutionData(envStateExecutionData)
          .build();
    }
  }

  private Map<String, String> populatePipelineVariables(WorkflowStandardParams workflowStandardParams) {
    if (workflowStandardParams.getWorkflowVariables() == null) {
      return workflowVariables;
    }
    Map<String, String> variables = workflowStandardParams.getWorkflowVariables();
    if (MapUtils.isEmpty(workflowVariables)) {
      return variables;
    }
    workflowVariables.keySet().forEach(s -> {
      if (!variables.containsKey(s)) {
        variables.put(s, workflowVariables.get(s));
      }
    });
    return variables;
  }
  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    EnvExecutionResponseData responseData = (EnvExecutionResponseData) response.values().iterator().next();
    ExecutionResponse executionResponse = anExecutionResponse().withExecutionStatus(responseData.getStatus()).build();

    if (responseData.getStatus() != ExecutionStatus.SUCCESS) {
      return executionResponse;
    }

    EnvStateExecutionData stateExecutionData = (EnvStateExecutionData) context.getStateExecutionData();
    if (stateExecutionData.getOrchestrationWorkflowType() == OrchestrationWorkflowType.BUILD) {
      List<Artifact> artifacts =
          executionService.getArtifactsCollected(context.getAppId(), stateExecutionData.getWorkflowExecutionId());
      if (CollectionUtils.isNotEmpty(artifacts)) {
        List<ContextElement> artifactElements = new ArrayList<>();
        artifacts.forEach(artifact
            -> artifactElements.add(aServiceArtifactElement()
                                        .withUuid(artifact.getUuid())
                                        .withName(artifact.getDisplayName())
                                        .withServiceIds(artifact.getServiceIds())
                                        .build()));
        executionResponse.setContextElements(artifactElements);
      }
    }

    return executionResponse;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets workflow id.
   *
   * @return the workflow id
   */
  public String getWorkflowId() {
    return workflowId;
  }

  /**
   * Sets workflow id.
   *
   * @param workflowId the workflow id
   */
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  /**
   * Get PipelineId
   * @return
   */
  public String getPipelineId() {
    return pipelineId;
  }

  /**
   * Set PipelineId
   * @param pipelineId
   */
  public void setPipelineId(String pipelineId) {
    this.pipelineId = pipelineId;
  }

  /**
   * Get Workflow variables
   * @return
   */
  public Map<String, String> getWorkflowVariables() {
    return workflowVariables;
  }

  @SchemaIgnore
  @Override
  public Integer getTimeoutMillis() {
    return Constants.ENV_STATE_TIMEOUT_MILLIS;
  }

  /**
   * Set workflow variables
   * @param workflowVariables
   */
  public void setWorkflowVariables(Map<String, String> workflowVariables) {
    this.workflowVariables = workflowVariables;
  }

  /**
   * The type Env execution response data.
   */
  public static class EnvExecutionResponseData implements NotifyResponseData {
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

package software.wings.sm.states;

import static java.util.Arrays.asList;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.EnvStateExecutionData;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Builder;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.impl.WorkflowServiceImpl;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.stencils.Expand;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

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

  @Transient @Inject private WorkflowExecutionService executionService;

  @Transient @Inject private PipelineService pipelineService;

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
    String appId = ((ExecutionContextImpl) context).getApp().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    List<String> artifactIds = workflowStandardParams.getArtifactIds();
    List<Artifact> artifacts = artifactIds.stream()
                                   .map(artifactId -> Builder.anArtifact().withUuid(artifactId).build())
                                   .collect(Collectors.toList());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setOrchestrationId(workflowId);
    executionArgs.setArtifacts(artifacts);
    executionArgs.setExecutionCredential(aSSHExecutionCredential().withExecutionType(SSH).build());
    executionArgs.setTriggeredFromPipeline(true);
    executionArgs.setPipelineId(pipelineId);
    executionArgs.setTriggeredBy(workflowStandardParams.getCurrentUser());

    EnvStateExecutionData envStateExecutionData = new EnvStateExecutionData();
    envStateExecutionData.setWorkflowId(workflowId);
    envStateExecutionData.setEnvId(envId);

    WorkflowExecution execution =
        executionService.triggerOrchestrationExecution(appId, envId, workflowId, executionArgs);
    envStateExecutionData.setWorkflowExecutionId(execution.getUuid());
    pipelineService.refreshPipelineExecutionAsync(appId, context.getWorkflowExecutionId());
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(asList(execution.getUuid()))
        .withStateExecutionData(envStateExecutionData)
        .build();
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
    pipelineService.refreshPipelineExecutionAsync(
        ((ExecutionContextImpl) context).getApp().getUuid(), responseData.getWorkflowExecutionId());
    return anExecutionResponse().withExecutionStatus(responseData.getStatus()).build();
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

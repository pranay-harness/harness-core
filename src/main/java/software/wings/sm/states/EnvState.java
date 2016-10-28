package software.wings.sm.states;

import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.states.EnvStateCallback.Builder.anEnvStateCallback;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.EnvStateExecutionData;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Builder;
import software.wings.common.UUIDGenerator;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.impl.WorkflowServiceImpl;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * A Env state to pause state machine execution.
 *
 * @author Rishi
 */
@Attributes(title = "Env")
public class EnvState extends State {
  @EnumData(enumDataProvider = EnvironmentServiceImpl.class)
  @Attributes(required = true, title = "Environment")
  private String envId;

  @EnumData(enumDataProvider = WorkflowServiceImpl.class)
  @Attributes(required = true, title = "Workflow")
  private String workflowId;

  @Transient @Inject private WorkflowExecutionService executionService;

  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Transient @Inject private ExecutorService executorService;

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
    String appId = context.getApp().getUuid();
    String pipelineExecutionId = context.getWorkflowExecutionId();

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

    EnvStateExecutionData envStateExecutionData = new EnvStateExecutionData();
    envStateExecutionData.setWorkflowId(workflowId);
    envStateExecutionData.setEnvId(envId);
    envStateExecutionData.setCorrelationId(UUIDGenerator.getUuid());

    executorService.submit(() -> {
      WorkflowExecution execution = executionService.triggerEnvExecution(appId, envId, executionArgs);
      waitNotifyEngine.waitForAll(anEnvStateCallback()
                                      .withAppId(appId)
                                      .withPipelineExecutionId(pipelineExecutionId)
                                      .withCorrelationId(envStateExecutionData.getCorrelationId())
                                      .withWorkflowExecutionId(execution.getUuid())
                                      .build(),
          execution.getUuid());
    });

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(envStateExecutionData.getCorrelationId()))
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
}

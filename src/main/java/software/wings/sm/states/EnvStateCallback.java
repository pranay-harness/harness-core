package software.wings.sm.states;

import static software.wings.sm.ExecutionStatus.ExecutionStatusData.Builder.anExecutionStatusData;

import software.wings.service.intfc.PipelineService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExecutionStatus.ExecutionStatusData;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Map;
import javax.inject.Inject;

/**
 * Created by anubhaw on 10/27/16.
 */
public class EnvStateCallback implements NotifyCallback {
  @Inject private PipelineService pipelineService;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  private String correlationId;
  private String workflowExecutionId;
  private String appId;

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    if (response.containsKey(workflowExecutionId) && response.get(workflowExecutionId) instanceof ExecutionStatusData) {
      ExecutionStatus status = ((ExecutionStatusData) response.get(workflowExecutionId)).getExecutionStatus();
      waitNotifyEngine.notify(correlationId, anExecutionStatusData().withExecutionStatus(status).build());
      pipelineService.updatePipelineExecutionData(appId, workflowExecutionId, status);
    }
  }

  /**
   * Gets correlation id.
   *
   * @return the correlation id
   */
  public String getCorrelationId() {
    return correlationId;
  }

  /**
   * Sets correlation id.
   *
   * @param correlationId the correlation id
   */
  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
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
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private PipelineService pipelineService;
    private WaitNotifyEngine waitNotifyEngine;
    private String correlationId;
    private String workflowExecutionId;
    private String appId;

    private Builder() {}

    /**
     * An env state callback builder.
     *
     * @return the builder
     */
    public static Builder anEnvStateCallback() {
      return new Builder();
    }

    /**
     * With pipeline service builder.
     *
     * @param pipelineService the pipeline service
     * @return the builder
     */
    public Builder withPipelineService(PipelineService pipelineService) {
      this.pipelineService = pipelineService;
      return this;
    }

    /**
     * With wait notify engine builder.
     *
     * @param waitNotifyEngine the wait notify engine
     * @return the builder
     */
    public Builder withWaitNotifyEngine(WaitNotifyEngine waitNotifyEngine) {
      this.waitNotifyEngine = waitNotifyEngine;
      return this;
    }

    /**
     * With correlation id builder.
     *
     * @param correlationId the correlation id
     * @return the builder
     */
    public Builder withCorrelationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    /**
     * With workflow execution id builder.
     *
     * @param workflowExecutionId the workflow execution id
     * @return the builder
     */
    public Builder withWorkflowExecutionId(String workflowExecutionId) {
      this.workflowExecutionId = workflowExecutionId;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anEnvStateCallback()
          .withPipelineService(pipelineService)
          .withWaitNotifyEngine(waitNotifyEngine)
          .withCorrelationId(correlationId)
          .withWorkflowExecutionId(workflowExecutionId)
          .withAppId(appId);
    }

    /**
     * Build env state callback.
     *
     * @return the env state callback
     */
    public EnvStateCallback build() {
      EnvStateCallback envStateCallback = new EnvStateCallback();
      envStateCallback.setCorrelationId(correlationId);
      envStateCallback.setWorkflowExecutionId(workflowExecutionId);
      envStateCallback.setAppId(appId);
      envStateCallback.pipelineService = this.pipelineService;
      envStateCallback.waitNotifyEngine = this.waitNotifyEngine;
      return envStateCallback;
    }
  }
}

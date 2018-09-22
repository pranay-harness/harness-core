package software.wings.api;

import software.wings.beans.OrchestrationWorkflowType;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

/**
 * Created by anubhaw on 10/26/16.
 */
public class EnvStateExecutionData extends StateExecutionData {
  private String workflowId;
  private String workflowExecutionId;
  private String envId;
  private OrchestrationWorkflowType orchestrationWorkflowType;

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

  public OrchestrationWorkflowType getOrchestrationWorkflowType() {
    return orchestrationWorkflowType;
  }

  public void setOrchestrationWorkflowType(OrchestrationWorkflowType orchestrationWorkflowType) {
    this.orchestrationWorkflowType = orchestrationWorkflowType;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String workflowId;
    private String workflowExecutionId;
    private String envId;
    private OrchestrationWorkflowType orchestrationWorkflowType;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;

    private Builder() {}

    /**
     * An env state execution data builder.
     *
     * @return the builder
     */
    public static Builder anEnvStateExecutionData() {
      return new Builder();
    }

    /**
     * With workflow id builder.
     *
     * @param workflowId the workflow id
     * @return the builder
     */
    public Builder withWorkflowId(String workflowId) {
      this.workflowId = workflowId;
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
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param orchestrationWorkflowType the orchestrationWorkflowType
     * @return the builder
     */
    public Builder withOrchestrationWorkflowType(OrchestrationWorkflowType orchestrationWorkflowType) {
      this.orchestrationWorkflowType = orchestrationWorkflowType;
      return this;
    }

    /**
     * With state name builder.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With start ts builder.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts builder.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With error msg builder.
     *
     * @param errorMsg the error msg
     * @return the builder
     */
    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anEnvStateExecutionData()
          .withWorkflowId(workflowId)
          .withWorkflowExecutionId(workflowExecutionId)
          .withEnvId(envId)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg)
          .withOrchestrationWorkflowType(orchestrationWorkflowType);
    }

    /**
     * Build env state execution data.
     *
     * @return the env state execution data
     */
    public EnvStateExecutionData build() {
      EnvStateExecutionData envStateExecutionData = new EnvStateExecutionData();
      envStateExecutionData.setWorkflowId(workflowId);
      envStateExecutionData.setWorkflowExecutionId(workflowExecutionId);
      envStateExecutionData.setEnvId(envId);
      envStateExecutionData.setStateName(stateName);
      envStateExecutionData.setStartTs(startTs);
      envStateExecutionData.setEndTs(endTs);
      envStateExecutionData.setStatus(status);
      envStateExecutionData.setErrorMsg(errorMsg);
      envStateExecutionData.setOrchestrationWorkflowType(orchestrationWorkflowType);
      return envStateExecutionData;
    }
  }
}

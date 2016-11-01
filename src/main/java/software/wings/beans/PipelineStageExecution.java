package software.wings.beans;

import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 10/26/16.
 */
public class PipelineStageExecution {
  private ExecutionStatus status;
  private Long startTs;
  private Long endTs;
  private List<String> workflowExecutionIds = new ArrayList<>();
  private List<WorkflowExecution> workflowExecutions = new ArrayList<>();

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

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  /**
   * Gets workflow executions.
   *
   * @return the workflow executions
   */
  public List<WorkflowExecution> getWorkflowExecutions() {
    return workflowExecutions;
  }

  /**
   * Sets workflow executions.
   *
   * @param workflowExecutions the workflow executions
   */
  public void setWorkflowExecutions(List<WorkflowExecution> workflowExecutions) {
    this.workflowExecutions = workflowExecutions;
  }

  /**
   * Gets workflow execution ids.
   *
   * @return the workflow execution ids
   */
  public List<String> getWorkflowExecutionIds() {
    return workflowExecutionIds;
  }

  /**
   * Sets workflow execution ids.
   *
   * @param workflowExecutionIds the workflow execution ids
   */
  public void setWorkflowExecutionIds(List<String> workflowExecutionIds) {
    this.workflowExecutionIds = workflowExecutionIds;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private ExecutionStatus status;
    private Long startTs;
    private Long endTs;
    private List<String> workflowExecutionIds = new ArrayList<>();
    private List<WorkflowExecution> workflowExecutions = new ArrayList<>();

    private Builder() {}

    /**
     * A pipeline stage execution builder.
     *
     * @return the builder
     */
    public static Builder aPipelineStageExecution() {
      return new Builder();
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
     * With workflow execution ids builder.
     *
     * @param workflowExecutionIds the workflow execution ids
     * @return the builder
     */
    public Builder withWorkflowExecutionIds(List<String> workflowExecutionIds) {
      this.workflowExecutionIds = workflowExecutionIds;
      return this;
    }

    /**
     * With workflow executions builder.
     *
     * @param workflowExecutions the workflow executions
     * @return the builder
     */
    public Builder withWorkflowExecutions(List<WorkflowExecution> workflowExecutions) {
      this.workflowExecutions = workflowExecutions;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aPipelineStageExecution()
          .withStatus(status)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withWorkflowExecutionIds(workflowExecutionIds)
          .withWorkflowExecutions(workflowExecutions);
    }

    /**
     * Build pipeline stage execution.
     *
     * @return the pipeline stage execution
     */
    public PipelineStageExecution build() {
      PipelineStageExecution pipelineStageExecution = new PipelineStageExecution();
      pipelineStageExecution.setStatus(status);
      pipelineStageExecution.setStartTs(startTs);
      pipelineStageExecution.setEndTs(endTs);
      pipelineStageExecution.setWorkflowExecutionIds(workflowExecutionIds);
      pipelineStageExecution.setWorkflowExecutions(workflowExecutions);
      return pipelineStageExecution;
    }
  }
}

package software.wings.beans;

import com.google.common.base.MoreObjects;

import io.harness.beans.EmbeddedUser;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Version;
import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 10/26/16.
 */
@Entity(value = "pipelineExecutions", noClassnameStored = true)
public class PipelineExecution extends Base {
  public static final String PIPELINE_ID_KEY = "pipelineId";

  @Indexed private String pipelineId;
  @Indexed private String workflowExecutionId;
  private String stateMachineId;
  private Pipeline pipeline;
  private List<PipelineStageExecution> pipelineStageExecutions = new ArrayList<>();
  private String appName;
  @Indexed private WorkflowType workflowType;
  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;
  @Version private Long version;

  private String name;

  private Long startTs;
  private Long endTs;
  private Long estimatedTime;

  /**
   * Gets pipeline id.
   *
   * @return the pipeline id
   */
  public String getPipelineId() {
    return pipelineId;
  }

  /**
   * Sets pipeline id.
   *
   * @param pipelineId the pipeline id
   */
  public void setPipelineId(String pipelineId) {
    this.pipelineId = pipelineId;
  }

  /**
   * Gets pipeline.
   *
   * @return the pipeline
   */
  public Pipeline getPipeline() {
    return pipeline;
  }

  /**
   * Sets pipeline.
   *
   * @param pipeline the pipeline
   */
  public void setPipeline(Pipeline pipeline) {
    this.pipeline = pipeline;
  }

  /**
   * Gets pipeline stage executions.
   *
   * @return the pipeline stage executions
   */
  public List<PipelineStageExecution> getPipelineStageExecutions() {
    return pipelineStageExecutions;
  }

  /**
   * Sets pipeline stage executions.
   *
   * @param pipelineStageExecutions the pipeline stage executions
   */
  public void setPipelineStageExecutions(List<PipelineStageExecution> pipelineStageExecutions) {
    this.pipelineStageExecutions = pipelineStageExecutions;
  }

  /**
   * Gets app name.
   *
   * @return the app name
   */
  public String getAppName() {
    return appName;
  }

  /**
   * Sets app name.
   *
   * @param appName the app name
   */
  public void setAppName(String appName) {
    this.appName = appName;
  }

  /**
   * Gets workflow type.
   *
   * @return the workflow type
   */
  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  /**
   * Sets workflow type.
   *
   * @param workflowType the workflow type
   */
  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
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

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
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

  public String getStateMachineId() {
    return stateMachineId;
  }

  public void setStateMachineId(String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(pipelineId, workflowExecutionId, pipeline, pipelineStageExecutions, appName, workflowType,
              status, name, startTs, endTs);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final PipelineExecution other = (PipelineExecution) obj;
    return Objects.equals(this.pipelineId, other.pipelineId)
        && Objects.equals(this.workflowExecutionId, other.workflowExecutionId)
        && Objects.equals(this.pipeline, other.pipeline)
        && Objects.equals(this.pipelineStageExecutions, other.pipelineStageExecutions)
        && Objects.equals(this.appName, other.appName) && Objects.equals(this.workflowType, other.workflowType)
        && Objects.equals(this.status, other.status) && Objects.equals(this.name, other.name)
        && Objects.equals(this.startTs, other.startTs) && Objects.equals(this.endTs, other.endTs);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("pipelineId", pipelineId)
        .add("workflowExecutionId", workflowExecutionId)
        .add("pipeline", pipeline)
        .add("pipelineStageExecutions", pipelineStageExecutions)
        .add("appName", appName)
        .add("workflowType", workflowType)
        .add("status", status)
        .add("name", name)
        .add("startTs", startTs)
        .add("endTs", endTs)
        .toString();
  }

  /**
   * Gets estimated time.
   *
   * @return the estimated time
   */
  public Long getEstimatedTime() {
    return estimatedTime;
  }

  /**
   * Sets estimated time.
   *
   * @param estimatedTime the estimated time
   */
  public void setEstimatedTime(Long estimatedTime) {
    this.estimatedTime = estimatedTime;
  }

  public Long getVersion() {
    return version;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String pipelineId;
    private String workflowExecutionId;
    private String stateMachineId;
    private String artifactId;
    private String artifactName;
    private Pipeline pipeline;
    private ExecutionArgs executionArgs;
    private List<PipelineStageExecution> pipelineStageExecutions = new ArrayList<>();
    private String appName;
    private WorkflowType workflowType;
    private ExecutionStatus status = ExecutionStatus.NEW;
    private String name;
    private Long startTs;
    private Long endTs;
    private Long estimatedTime = Long.valueOf(5 * 60 * 1000);
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A pipeline execution builder.
     *
     * @return the builder
     */
    public static Builder aPipelineExecution() {
      return new Builder();
    }

    /**
     * With pipeline id builder.
     *
     * @param pipelineId the pipeline id
     * @return the builder
     */
    public Builder withPipelineId(String pipelineId) {
      this.pipelineId = pipelineId;
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
     * With state machine id builder.
     *
     * @param stateMachineId  the state machine id
     * @return the builder
     */
    public Builder withStateMachineId(String stateMachineId) {
      this.stateMachineId = stateMachineId;
      return this;
    }

    /**
     * With artifact id builder.
     *
     * @param artifactId the artifact id
     * @return the builder
     */
    public Builder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    /**
     * With artifact name builder.
     *
     * @param artifactName the artifact name
     * @return the builder
     */
    public Builder withArtifactName(String artifactName) {
      this.artifactName = artifactName;
      return this;
    }

    /**
     * With pipeline builder.
     *
     * @param pipeline the pipeline
     * @return the builder
     */
    public Builder withPipeline(Pipeline pipeline) {
      this.pipeline = pipeline;
      return this;
    }

    /**
     * With pipeline builder.
     *
     * @param executionArgs the executionArgs
     * @return the builder
     */
    public Builder withExecutionArgs(ExecutionArgs executionArgs) {
      this.executionArgs = executionArgs;
      return this;
    }

    /**
     * With pipeline stage executions builder.
     *
     * @param pipelineStageExecutions the pipeline stage executions
     * @return the builder
     */
    public Builder withPipelineStageExecutions(List<PipelineStageExecution> pipelineStageExecutions) {
      this.pipelineStageExecutions = pipelineStageExecutions;
      return this;
    }

    /**
     * With app name builder.
     *
     * @param appName the app name
     * @return the builder
     */
    public Builder withAppName(String appName) {
      this.appName = appName;
      return this;
    }

    /**
     * With workflow type builder.
     *
     * @param workflowType the workflow type
     * @return the builder
     */
    public Builder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
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
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
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
     * With estimated time builder.
     *
     * @param estimatedTime the estimated time
     * @return the builder
     */
    public Builder withEstimatedTime(Long estimatedTime) {
      this.estimatedTime = estimatedTime;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
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
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aPipelineExecution()
          .withPipelineId(pipelineId)
          .withWorkflowExecutionId(workflowExecutionId)
          .withArtifactId(artifactId)
          .withArtifactName(artifactName)
          .withPipeline(pipeline)
          .withPipelineStageExecutions(pipelineStageExecutions)
          .withAppName(appName)
          .withWorkflowType(workflowType)
          .withStatus(status)
          .withName(name)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withEstimatedTime(estimatedTime)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withStateMachineId(stateMachineId)
          .withExecutionArgs(executionArgs);
    }

    /**
     * Build pipeline execution.
     *
     * @return the pipeline execution
     */
    public PipelineExecution build() {
      PipelineExecution pipelineExecution = new PipelineExecution();
      pipelineExecution.setPipelineId(pipelineId);
      pipelineExecution.setWorkflowExecutionId(workflowExecutionId);
      pipelineExecution.setPipeline(pipeline);
      pipelineExecution.setPipelineStageExecutions(pipelineStageExecutions);
      pipelineExecution.setAppName(appName);
      pipelineExecution.setWorkflowType(workflowType);
      pipelineExecution.setStatus(status);
      pipelineExecution.setName(name);
      pipelineExecution.setStartTs(startTs);
      pipelineExecution.setEndTs(endTs);
      pipelineExecution.setEstimatedTime(estimatedTime);
      pipelineExecution.setUuid(uuid);
      pipelineExecution.setAppId(appId);
      pipelineExecution.setCreatedBy(createdBy);
      pipelineExecution.setCreatedAt(createdAt);
      pipelineExecution.setLastUpdatedBy(lastUpdatedBy);
      pipelineExecution.setLastUpdatedAt(lastUpdatedAt);
      pipelineExecution.setStateMachineId(stateMachineId);
      return pipelineExecution;
    }
  }
}

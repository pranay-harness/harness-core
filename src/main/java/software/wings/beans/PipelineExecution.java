package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.sm.ExecutionStatus;

import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 10/26/16.
 */
@Entity(value = "pipelineExecutions", noClassnameStored = true)
public class PipelineExecution extends Base {
  @Indexed private String pipelineId;
  @Transient private Pipeline pipeline;
  @Transient private Artifact artifact;
  private List<PipelineStageExecution> pipelineStageExecutions;
  @Indexed private String envId;
  private String appName;
  private String envName;
  private EnvironmentType envType;
  @Indexed private WorkflowType workflowType;
  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;

  private String name;

  private Long startTs;
  private Long endTs;

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
   * Gets artifact.
   *
   * @return the artifact
   */
  public Artifact getArtifact() {
    return artifact;
  }

  /**
   * Sets artifact.
   *
   * @param artifact the artifact
   */
  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
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
   * Gets env name.
   *
   * @return the env name
   */
  public String getEnvName() {
    return envName;
  }

  /**
   * Sets env name.
   *
   * @param envName the env name
   */
  public void setEnvName(String envName) {
    this.envName = envName;
  }

  /**
   * Gets env type.
   *
   * @return the env type
   */
  public EnvironmentType getEnvType() {
    return envType;
  }

  /**
   * Sets env type.
   *
   * @param envType the env type
   */
  public void setEnvType(EnvironmentType envType) {
    this.envType = envType;
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

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(pipelineId, pipeline, artifact, pipelineStageExecutions, envId, appName, envName, envType,
              workflowType, status, name, startTs, endTs);
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
    return Objects.equals(this.pipelineId, other.pipelineId) && Objects.equals(this.pipeline, other.pipeline)
        && Objects.equals(this.artifact, other.artifact)
        && Objects.equals(this.pipelineStageExecutions, other.pipelineStageExecutions)
        && Objects.equals(this.envId, other.envId) && Objects.equals(this.appName, other.appName)
        && Objects.equals(this.envName, other.envName) && Objects.equals(this.envType, other.envType)
        && Objects.equals(this.workflowType, other.workflowType) && Objects.equals(this.status, other.status)
        && Objects.equals(this.name, other.name) && Objects.equals(this.startTs, other.startTs)
        && Objects.equals(this.endTs, other.endTs);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("pipelineId", pipelineId)
        .add("pipeline", pipeline)
        .add("artifact", artifact)
        .add("pipelineStageExecutions", pipelineStageExecutions)
        .add("envId", envId)
        .add("appName", appName)
        .add("envName", envName)
        .add("envType", envType)
        .add("workflowType", workflowType)
        .add("status", status)
        .add("name", name)
        .add("startTs", startTs)
        .add("endTs", endTs)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String pipelineId;
    private Pipeline pipeline;
    private Artifact artifact;
    private List<PipelineStageExecution> pipelineStageExecutions;
    private String envId;
    private String appName;
    private String envName;
    private EnvironmentType envType;
    private WorkflowType workflowType;
    private ExecutionStatus status = ExecutionStatus.NEW;
    private String name;
    private Long startTs;
    private Long endTs;
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
     * With artifact builder.
     *
     * @param artifact the artifact
     * @return the builder
     */
    public Builder withArtifact(Artifact artifact) {
      this.artifact = artifact;
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
     * With env name builder.
     *
     * @param envName the env name
     * @return the builder
     */
    public Builder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    /**
     * With env type builder.
     *
     * @param envType the env type
     * @return the builder
     */
    public Builder withEnvType(EnvironmentType envType) {
      this.envType = envType;
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
          .withPipeline(pipeline)
          .withArtifact(artifact)
          .withPipelineStageExecutions(pipelineStageExecutions)
          .withEnvId(envId)
          .withAppName(appName)
          .withEnvName(envName)
          .withEnvType(envType)
          .withWorkflowType(workflowType)
          .withStatus(status)
          .withName(name)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build pipeline execution.
     *
     * @return the pipeline execution
     */
    public PipelineExecution build() {
      PipelineExecution pipelineExecution = new PipelineExecution();
      pipelineExecution.setPipelineId(pipelineId);
      pipelineExecution.setPipeline(pipeline);
      pipelineExecution.setArtifact(artifact);
      pipelineExecution.setPipelineStageExecutions(pipelineStageExecutions);
      pipelineExecution.setEnvId(envId);
      pipelineExecution.setAppName(appName);
      pipelineExecution.setEnvName(envName);
      pipelineExecution.setEnvType(envType);
      pipelineExecution.setWorkflowType(workflowType);
      pipelineExecution.setStatus(status);
      pipelineExecution.setName(name);
      pipelineExecution.setStartTs(startTs);
      pipelineExecution.setEndTs(endTs);
      pipelineExecution.setUuid(uuid);
      pipelineExecution.setAppId(appId);
      pipelineExecution.setCreatedBy(createdBy);
      pipelineExecution.setCreatedAt(createdAt);
      pipelineExecution.setLastUpdatedBy(lastUpdatedBy);
      pipelineExecution.setLastUpdatedAt(lastUpdatedAt);
      return pipelineExecution;
    }
  }
}

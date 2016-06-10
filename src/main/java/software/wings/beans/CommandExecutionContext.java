package software.wings.beans;

import com.google.common.base.MoreObjects;

/**
 * Created by peeyushaggarwal on 6/9/16.
 */
public class CommandExecutionContext {
  private Artifact artifact;
  private String activityId;
  private String runtimePath;
  private String stagingPath;
  private String backupPath;
  private ExecutionCredential executionCredential;

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
   * Gets activity id.
   *
   * @return the activity id
   */
  public String getActivityId() {
    return activityId;
  }

  /**
   * Sets activity id.
   *
   * @param activityId the activity id
   */
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  /**
   * Gets runtime path.
   *
   * @return the runtime path
   */
  public String getRuntimePath() {
    return runtimePath;
  }

  /**
   * Sets runtime path.
   *
   * @param runtimePath the runtime path
   */
  public void setRuntimePath(String runtimePath) {
    this.runtimePath = runtimePath;
  }

  /**
   * Gets staging path.
   *
   * @return the staging path
   */
  public String getStagingPath() {
    return stagingPath;
  }

  /**
   * Sets staging path.
   *
   * @param stagingPath the staging path
   */
  public void setStagingPath(String stagingPath) {
    this.stagingPath = stagingPath;
  }

  /**
   * Gets backup path.
   *
   * @return the backup path
   */
  public String getBackupPath() {
    return backupPath;
  }

  /**
   * Sets backup path.
   *
   * @param backupPath the backup path
   */
  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  /**
   * Gets execution credential.
   *
   * @return the execution credential
   */
  public ExecutionCredential getExecutionCredential() {
    return executionCredential;
  }

  /**
   * Sets execution credential.
   *
   * @param executionCredential the execution credential
   */
  public void setExecutionCredential(ExecutionCredential executionCredential) {
    this.executionCredential = executionCredential;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("artifact", artifact)
        .add("activityId", activityId)
        .add("runtimePath", runtimePath)
        .add("stagingPath", stagingPath)
        .add("backupPath", backupPath)
        .add("executionCredential", executionCredential)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private Artifact artifact;
    private String activityId;
    private String runtimePath;
    private String stagingPath;
    private String backupPath;
    private ExecutionCredential executionCredential;

    /** Do not instantiate Builder. */
    private Builder() {}

    /**
     * A command execution context builder.
     *
     * @return the builder
     */
    public static Builder aCommandExecutionContext() {
      return new Builder();
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
     * With activity id builder.
     *
     * @param activityId the activity id
     * @return the builder
     */
    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    /**
     * With runtime path builder.
     *
     * @param runtimePath the runtime path
     * @return the builder
     */
    public Builder withRuntimePath(String runtimePath) {
      this.runtimePath = runtimePath;
      return this;
    }

    /**
     * With staging path builder.
     *
     * @param stagingPath the staging path
     * @return the builder
     */
    public Builder withStagingPath(String stagingPath) {
      this.stagingPath = stagingPath;
      return this;
    }

    /**
     * With backup path builder.
     *
     * @param backupPath the backup path
     * @return the builder
     */
    public Builder withBackupPath(String backupPath) {
      this.backupPath = backupPath;
      return this;
    }

    /**
     * With execution credential builder.
     *
     * @param executionCredential the execution credential
     * @return the builder
     */
    public Builder withExecutionCredential(ExecutionCredential executionCredential) {
      this.executionCredential = executionCredential;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCommandExecutionContext()
          .withArtifact(artifact)
          .withActivityId(activityId)
          .withRuntimePath(runtimePath)
          .withStagingPath(stagingPath)
          .withBackupPath(backupPath)
          .withExecutionCredential(executionCredential);
    }

    /**
     * Build command execution context.
     *
     * @return the command execution context
     */
    public CommandExecutionContext build() {
      CommandExecutionContext commandExecutionContext = new CommandExecutionContext();
      commandExecutionContext.setArtifact(artifact);
      commandExecutionContext.setActivityId(activityId);
      commandExecutionContext.setRuntimePath(runtimePath);
      commandExecutionContext.setStagingPath(stagingPath);
      commandExecutionContext.setBackupPath(backupPath);
      commandExecutionContext.setExecutionCredential(executionCredential);
      return commandExecutionContext;
    }
  }
}

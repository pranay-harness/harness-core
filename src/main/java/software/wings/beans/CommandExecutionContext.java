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

  public Artifact getArtifact() {
    return artifact;
  }

  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public String getRuntimePath() {
    return runtimePath;
  }

  public void setRuntimePath(String runtimePath) {
    this.runtimePath = runtimePath;
  }

  public String getStagingPath() {
    return stagingPath;
  }

  public void setStagingPath(String stagingPath) {
    this.stagingPath = stagingPath;
  }

  public String getBackupPath() {
    return backupPath;
  }

  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("artifact", artifact)
        .add("activityId", activityId)
        .add("runtimePath", runtimePath)
        .add("stagingPath", stagingPath)
        .add("backupPath", backupPath)
        .toString();
  }

  public static final class Builder {
    private Artifact artifact;
    private String activityId;
    private String runtimePath;
    private String stagingPath;
    private String backupPath;

    private Builder() {}

    public static Builder aCommandExecutionContext() {
      return new Builder();
    }

    public Builder withArtifact(Artifact artifact) {
      this.artifact = artifact;
      return this;
    }

    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    public Builder withRuntimePath(String runtimePath) {
      this.runtimePath = runtimePath;
      return this;
    }

    public Builder withStagingPath(String stagingPath) {
      this.stagingPath = stagingPath;
      return this;
    }

    public Builder withBackupPath(String backupPath) {
      this.backupPath = backupPath;
      return this;
    }

    public Builder but() {
      return aCommandExecutionContext()
          .withArtifact(artifact)
          .withActivityId(activityId)
          .withRuntimePath(runtimePath)
          .withStagingPath(stagingPath)
          .withBackupPath(backupPath);
    }

    public CommandExecutionContext build() {
      CommandExecutionContext commandExecutionContext = new CommandExecutionContext();
      commandExecutionContext.setArtifact(artifact);
      commandExecutionContext.setActivityId(activityId);
      commandExecutionContext.setRuntimePath(runtimePath);
      commandExecutionContext.setStagingPath(stagingPath);
      commandExecutionContext.setBackupPath(backupPath);
      return commandExecutionContext;
    }
  }
}

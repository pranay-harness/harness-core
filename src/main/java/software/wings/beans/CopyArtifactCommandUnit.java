package software.wings.beans;

import com.google.common.base.MoreObjects;

import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;
import software.wings.service.intfc.FileService.FileBucket;

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
public class CopyArtifactCommandUnit extends ScpCommandUnit {
  private String artifactId;

  public CopyArtifactCommandUnit() {
    super(CommandUnitType.COPY_ARTIFACT);
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("artifactId", artifactId).toString();
  }

  public static final class Builder {
    private String artifactId;
    private String fileId;
    private FileBucket fileBucket;
    private String destinationFilePath;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;

    private Builder() {}

    public static Builder aCopyArtifactCommandUnit() {
      return new Builder();
    }

    public Builder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public Builder withFileId(String fileId) {
      this.fileId = fileId;
      return this;
    }

    public Builder withFileBucket(FileBucket fileBucket) {
      this.fileBucket = fileBucket;
      return this;
    }

    public Builder withDestinationFilePath(String destinationFilePath) {
      this.destinationFilePath = destinationFilePath;
      return this;
    }

    public Builder withCommandUnitType(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
      return this;
    }

    public Builder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    public Builder but() {
      return aCopyArtifactCommandUnit()
          .withArtifactId(artifactId)
          .withFileId(fileId)
          .withFileBucket(fileBucket)
          .withDestinationFilePath(destinationFilePath)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult);
    }

    public CopyArtifactCommandUnit build() {
      CopyArtifactCommandUnit copyArtifactCommandUnit = new CopyArtifactCommandUnit();
      copyArtifactCommandUnit.setArtifactId(artifactId);
      copyArtifactCommandUnit.setFileId(fileId);
      copyArtifactCommandUnit.setFileBucket(fileBucket);
      copyArtifactCommandUnit.setDestinationFilePath(destinationFilePath);
      copyArtifactCommandUnit.setCommandUnitType(commandUnitType);
      copyArtifactCommandUnit.setExecutionResult(executionResult);
      return copyArtifactCommandUnit;
    }
  }
}

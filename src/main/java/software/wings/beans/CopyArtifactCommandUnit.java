package software.wings.beans;

import com.google.common.base.MoreObjects;

import software.wings.service.intfc.FileService.FileBucket;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
public class CopyArtifactCommandUnit extends CopyCommandUnit {
  private String artifactId;

  /**
   * Instantiates a new copy artifact command unit.
   */
  public CopyArtifactCommandUnit() {
    super(CommandUnitType.COPY_ARTIFACT);
  }

  /**
   * Gets artifact id.
   *
   * @return the artifact id
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Sets artifact id.
   *
   * @param artifactId the artifact id
   */
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("artifactId", artifactId).toString();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String artifactId;
    private String fileId;
    private FileBucket fileBucket;
    private String destinationFilePath;
    private String name;
    private String serviceId;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;

    private Builder() {}

    /**
     * A copy artifact command unit.
     *
     * @return the builder
     */
    public static Builder aCopyArtifactCommandUnit() {
      return new Builder();
    }

    /**
     * With artifact id.
     *
     * @param artifactId the artifact id
     * @return the builder
     */
    public Builder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    /**
     * With file id.
     *
     * @param fileId the file id
     * @return the builder
     */
    public Builder withFileId(String fileId) {
      this.fileId = fileId;
      return this;
    }

    /**
     * With file bucket.
     *
     * @param fileBucket the file bucket
     * @return the builder
     */
    public Builder withFileBucket(FileBucket fileBucket) {
      this.fileBucket = fileBucket;
      return this;
    }

    /**
     * With destination file path.
     *
     * @param destinationFilePath the destination file path
     * @return the builder
     */
    public Builder withDestinationFilePath(String destinationFilePath) {
      this.destinationFilePath = destinationFilePath;
      return this;
    }

    /**
     * With name.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With service id.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With command unit type.
     *
     * @param commandUnitType the command unit type
     * @return the builder
     */
    public Builder withCommandUnitType(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
      return this;
    }

    /**
     * With execution result.
     *
     * @param executionResult the execution result
     * @return the builder
     */
    public Builder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return aCopyArtifactCommandUnit()
          .withArtifactId(artifactId)
          .withFileId(fileId)
          .withFileBucket(fileBucket)
          .withDestinationFilePath(destinationFilePath)
          .withName(name)
          .withServiceId(serviceId)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult);
    }

    /**
     * Builds the.
     *
     * @return the copy artifact command unit
     */
    public CopyArtifactCommandUnit build() {
      CopyArtifactCommandUnit copyArtifactCommandUnit = new CopyArtifactCommandUnit();
      copyArtifactCommandUnit.setArtifactId(artifactId);
      copyArtifactCommandUnit.setFileId(fileId);
      copyArtifactCommandUnit.setFileBucket(fileBucket);
      copyArtifactCommandUnit.setDestinationFilePath(destinationFilePath);
      copyArtifactCommandUnit.setName(name);
      copyArtifactCommandUnit.setServiceId(serviceId);
      copyArtifactCommandUnit.setCommandUnitType(commandUnitType);
      copyArtifactCommandUnit.setExecutionResult(executionResult);
      return copyArtifactCommandUnit;
    }
  }
}

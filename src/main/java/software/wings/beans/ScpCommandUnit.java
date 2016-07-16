package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.Objects;

/**
 * Created by anubhaw on 7/14/16.
 */
public class ScpCommandUnit extends CopyCommandUnit {
  @Attributes(title = "Artifacts", enums = {"Application Stack", "Application Artifacts", "Configurations"})
  /* Use enum data and and ScpFileCategory */
  private String fileCategory;

  @Attributes(title = "Destination path", description = "Relative to ${RuntimePath}") private String relativeFilePath;

  /**
   * The enum Scp file category.
   */
  public enum ScpFileCategory {
    /**
     * Artifacts scp file category.
     */
    ARTIFACTS("Application Artifacts"),
    /**
     * The Application stack.
     */
    APPLICATION_STACK("Application Stack"),
    /**
     * Configurations scp file category.
     */
    CONFIGURATIONS("Configurations");

    private String name;

    ScpFileCategory(String name) {
      this.name = name;
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
  }

  /**
   * Instantiates a new Scp command unit.
   */
  public ScpCommandUnit() {
    super(CommandUnitType.SCP);
  }

  @Override
  public void setup(CommandExecutionContext context) {
    switch (fileCategory) {
      case "Application Artifacts":
        setFileBucket(FileBucket.ARTIFACTS);
        ArtifactFile artifactFile =
            context.getArtifact().getArtifactFiles().get(0); // TODO: support list of artifact files
        setFileId(artifactFile.getFileUuid());
        setDestinationFilePath(constructPath(context.getRuntimePath(), relativeFilePath, artifactFile.getName()));
        break;
      case "Configurations":
        throw new WingsException(
            ErrorCodes.INVALID_REQUEST, "message", "Scp configuration not supported by server yet");
      case "Application Stack":
        AppContainer appContainer = context.getServiceInstance().getServiceTemplate().getService().getAppContainer();
        setFileBucket(FileBucket.PLATFORMS);
        setFileId(appContainer.getFileUuid());
        setDestinationFilePath(constructPath(context.getRuntimePath(), relativeFilePath, appContainer.getName()));
        break;
      default:
        throw new WingsException(
            ErrorCodes.INVALID_REQUEST, "message", "Unsupported file category for scp command unit");
    }
  }

  private String constructPath(String absolutePath, String relativePath, String filename) {
    return absolutePath.trim() + (Strings.isNullOrEmpty(relativePath) ? "/" : "/" + relativePath.trim() + "/")
        + filename.trim(); // TODO:: handle error cases
  }

  /**
   * Gets file category.
   *
   * @return the file category
   */
  public String getFileCategory() {
    return fileCategory;
  }

  /**
   * Sets file category.
   *
   * @param fileCategory the file category
   */
  public void setFileCategory(String fileCategory) {
    this.fileCategory = fileCategory;
  }

  @SchemaIgnore
  @Override
  public boolean isArtifactNeeded() {
    return fileCategory.contains("Artifacts");
  }

  /**
   * Gets relative file path.
   *
   * @return the relative file path
   */
  public String getRelativeFilePath() {
    return relativeFilePath;
  }

  /**
   * Sets relative file path.
   *
   * @param relativeFilePath the relative file path
   */
  public void setRelativeFilePath(String relativeFilePath) {
    this.relativeFilePath = relativeFilePath;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileCategory, relativeFilePath);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ScpCommandUnit other = (ScpCommandUnit) obj;
    return Objects.equals(this.fileCategory, other.fileCategory)
        && Objects.equals(this.relativeFilePath, other.relativeFilePath);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fileCategory", fileCategory)
        .add("relativeFilePath", relativeFilePath)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    /* Use enum data and and ScpFileCategory */
    private String fileCategory;
    private String relativeFilePath;
    private String fileId;
    private FileBucket fileBucket;
    private String destinationFilePath;
    private String name;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;
    private boolean artifactNeeded = false;
    private boolean processCommandOutput = false;

    private Builder() {}

    /**
     * A scp command unit builder.
     *
     * @return the builder
     */
    public static Builder aScpCommandUnit() {
      return new Builder();
    }

    /**
     * With file category builder.
     *
     * @param fileCategory the file category
     * @return the builder
     */
    public Builder withFileCategory(String fileCategory) {
      this.fileCategory = fileCategory;
      return this;
    }

    /**
     * With relative file path builder.
     *
     * @param relativeFilePath the relative file path
     * @return the builder
     */
    public Builder withRelativeFilePath(String relativeFilePath) {
      this.relativeFilePath = relativeFilePath;
      return this;
    }

    /**
     * With file id builder.
     *
     * @param fileId the file id
     * @return the builder
     */
    public Builder withFileId(String fileId) {
      this.fileId = fileId;
      return this;
    }

    /**
     * With file bucket builder.
     *
     * @param fileBucket the file bucket
     * @return the builder
     */
    public Builder withFileBucket(FileBucket fileBucket) {
      this.fileBucket = fileBucket;
      return this;
    }

    /**
     * With destination file path builder.
     *
     * @param destinationFilePath the destination file path
     * @return the builder
     */
    public Builder withDestinationFilePath(String destinationFilePath) {
      this.destinationFilePath = destinationFilePath;
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
     * With command unit type builder.
     *
     * @param commandUnitType the command unit type
     * @return the builder
     */
    public Builder withCommandUnitType(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
      return this;
    }

    /**
     * With execution result builder.
     *
     * @param executionResult the execution result
     * @return the builder
     */
    public Builder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    /**
     * With artifact needed builder.
     *
     * @param artifactNeeded the artifact needed
     * @return the builder
     */
    public Builder withArtifactNeeded(boolean artifactNeeded) {
      this.artifactNeeded = artifactNeeded;
      return this;
    }

    /**
     * With process command output builder.
     *
     * @param processCommandOutput the process command output
     * @return the builder
     */
    public Builder withProcessCommandOutput(boolean processCommandOutput) {
      this.processCommandOutput = processCommandOutput;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aScpCommandUnit()
          .withFileCategory(fileCategory)
          .withRelativeFilePath(relativeFilePath)
          .withFileId(fileId)
          .withFileBucket(fileBucket)
          .withDestinationFilePath(destinationFilePath)
          .withName(name)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult)
          .withArtifactNeeded(artifactNeeded)
          .withProcessCommandOutput(processCommandOutput);
    }

    /**
     * Build scp command unit.
     *
     * @return the scp command unit
     */
    public ScpCommandUnit build() {
      ScpCommandUnit scpCommandUnit = new ScpCommandUnit();
      scpCommandUnit.setFileCategory(fileCategory);
      scpCommandUnit.setRelativeFilePath(relativeFilePath);
      scpCommandUnit.setFileId(fileId);
      scpCommandUnit.setFileBucket(fileBucket);
      scpCommandUnit.setDestinationFilePath(destinationFilePath);
      scpCommandUnit.setName(name);
      scpCommandUnit.setCommandUnitType(commandUnitType);
      scpCommandUnit.setExecutionResult(executionResult);
      scpCommandUnit.setArtifactNeeded(artifactNeeded);
      scpCommandUnit.setProcessCommandOutput(processCommandOutput);
      return scpCommandUnit;
    }
  }
}

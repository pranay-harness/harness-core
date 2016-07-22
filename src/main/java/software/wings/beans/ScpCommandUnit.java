package software.wings.beans;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceTemplateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by anubhaw on 7/14/16.
 */
public class ScpCommandUnit extends CommandUnit {
  @Attributes(title = "Artifact Type", enums = {"Application Stack", "Application Artifacts", "Configurations"})
  /* Use enum data and and ScpFileCategory */ private String fileCategory;

  @Attributes(title = "Destination path", description = "Relative to ${RuntimePath}") private String relativeFilePath;

  @Inject @Transient private transient ServiceTemplateService serviceTemplateService;

  @SchemaIgnore private List<String> fileIds = new ArrayList<>();
  @SchemaIgnore private FileBucket fileBucket;
  @SchemaIgnore private String destinationDirectoryPath;

  /**
   * Instantiates a new Scp command unit.
   */
  public ScpCommandUnit() {
    super(CommandUnitType.SCP);
  }

  @Override
  public void setup(CommandExecutionContext context) {
    destinationDirectoryPath = constructPath(context.getRuntimePath(), relativeFilePath);
    switch (fileCategory) {
      case "Application Artifacts":
        fileBucket = FileBucket.ARTIFACTS;
        context.getArtifact().getArtifactFiles().forEach(artifactFile -> fileIds.add(artifactFile.getFileUuid()));
        break;
      case "Configurations":
        fileBucket = FileBucket.CONFIGS;

        ServiceTemplate serviceTemplate = context.getServiceInstance().getServiceTemplate();
        Map<String, List<ConfigFile>> computedConfigFiles = serviceTemplateService.computedConfigFiles(
            serviceTemplate.getAppId(), serviceTemplate.getEnvId(), serviceTemplate.getUuid());
        List<ConfigFile> configFiles = computedConfigFiles.get(context.getServiceInstance().getHost().getUuid());

        if (configFiles != null) {
          configFiles.forEach(configFile -> fileIds.add(configFile.getFileUuid()));
        }
        break;
      case "Application Stack":
        setFileBucket(FileBucket.PLATFORMS);
        AppContainer appContainer = context.getServiceInstance().getServiceTemplate().getService().getAppContainer();
        fileIds.add(appContainer.getFileUuid());
        break;
      default:
        throw new WingsException(
            ErrorCodes.INVALID_REQUEST, "message", "Unsupported file category for scp command unit");
    }
  }

  private String constructPath(String absolutePath, String relativePath) {
    return isNullOrEmpty(relativePath) ? absolutePath.trim()
                                       : absolutePath.trim() + "/" + relativePath.trim(); // TODO:: handle error cases
  }

  @SchemaIgnore
  @Override
  public boolean isArtifactNeeded() {
    return fileCategory.contains("Artifacts");
  }

  /**
   * Gets file category.
   *
   * @return the file category
   */
  @SchemaIgnore
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

  /**
   * Gets file ids.
   *
   * @return the file ids
   */
  @SchemaIgnore
  public List<String> getFileIds() {
    return fileIds;
  }

  /**
   * Sets file ids.
   *
   * @param fileIds the file ids
   */
  public void setFileIds(List<String> fileIds) {
    this.fileIds = fileIds;
  }

  /**
   * Gets file bucket.
   *
   * @return the file bucket
   */
  @SchemaIgnore
  public FileBucket getFileBucket() {
    return fileBucket;
  }

  /**
   * Sets file bucket.
   *
   * @param fileBucket the file bucket
   */
  public void setFileBucket(FileBucket fileBucket) {
    this.fileBucket = fileBucket;
  }

  /**
   * Gets destination directory path.
   *
   * @return the destination directory path
   */
  @SchemaIgnore
  public String getDestinationDirectoryPath() {
    return destinationDirectoryPath;
  }

  /**
   * Sets destination directory path.
   *
   * @param destinationDirectoryPath the destination directory path
   */
  public void setDestinationDirectoryPath(String destinationDirectoryPath) {
    this.destinationDirectoryPath = destinationDirectoryPath;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileCategory, relativeFilePath, fileIds, fileBucket, destinationDirectoryPath);
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
        && Objects.equals(this.relativeFilePath, other.relativeFilePath) && Objects.equals(this.fileIds, other.fileIds)
        && Objects.equals(this.fileBucket, other.fileBucket)
        && Objects.equals(this.destinationDirectoryPath, other.destinationDirectoryPath);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fileCategory", fileCategory)
        .add("relativeFilePath", relativeFilePath)
        .add("fileIds", fileIds)
        .add("fileBucket", fileBucket)
        .add("destinationDirectoryPath", destinationDirectoryPath)
        .toString();
  }

  /**
   * The enum Scp file category.
   */
  public enum ScpFileCategory {
    /**
     * Artifacts scp file category.
     */
    ARTIFACTS("Application Artifacts"), /**
                                         * The Application stack.
                                         */
    APPLICATION_STACK("Application Stack"), /**
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
   * The type Builder.
   */
  public static final class Builder {
    /* Use enum data and and ScpFileCategory */
    private String fileCategory;
    private String relativeFilePath;
    private List<String> fileIds;
    private FileBucket fileBucket;
    private String destinationDirectoryPath;
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
     * With file ids builder.
     *
     * @param fileIds the file ids
     * @return the builder
     */
    public Builder withFileIds(List<String> fileIds) {
      this.fileIds = fileIds;
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
     * With destination directory path builder.
     *
     * @param destinationDirectoryPath the destination directory path
     * @return the builder
     */
    public Builder withDestinationDirectoryPath(String destinationDirectoryPath) {
      this.destinationDirectoryPath = destinationDirectoryPath;
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
          .withFileIds(fileIds)
          .withFileBucket(fileBucket)
          .withDestinationDirectoryPath(destinationDirectoryPath)
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
      scpCommandUnit.setFileIds(fileIds);
      scpCommandUnit.setFileBucket(fileBucket);
      scpCommandUnit.setDestinationDirectoryPath(destinationDirectoryPath);
      scpCommandUnit.setName(name);
      scpCommandUnit.setCommandUnitType(commandUnitType);
      scpCommandUnit.setExecutionResult(executionResult);
      scpCommandUnit.setArtifactNeeded(artifactNeeded);
      scpCommandUnit.setProcessCommandOutput(processCommandOutput);
      return scpCommandUnit;
    }
  }
}

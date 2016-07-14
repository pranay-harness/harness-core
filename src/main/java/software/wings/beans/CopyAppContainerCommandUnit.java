package software.wings.beans;

import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.service.impl.AppContainerServiceImpl;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.stencils.EnumData;

/**
 * Created by anubhaw on 6/10/16.
 */
@Attributes(title = "App Container")
public class CopyAppContainerCommandUnit extends CopyCommandUnit {
  @Inject @Transient private transient AppContainerService appContainerService;

  @EnumData(enumDataProvider = AppContainerServiceImpl.class)
  @Attributes(title = "Application Container")
  private String appContainerId;

  /**
   * Instantiates a new copy command unit.
   */
  public CopyAppContainerCommandUnit() {
    super(CommandUnitType.COPY_APP_CONTAINER);
  }

  @Override
  public void setup(CommandExecutionContext context) {
    AppContainer appContainer = appContainerService.get(context.getAppId(), appContainerId);
    setFileBucket(PLATFORMS);
    setFileId(appContainer.getFileUuid());
    setDestinationFilePath(context.getRuntimePath() + "/" + appContainer.getName());
  }

  /**
   * Gets app container id.
   *
   * @return the app container id
   */
  public String getAppContainerId() {
    return appContainerId;
  }

  /**
   * Sets app container id.
   *
   * @param appContainerId the app container id
   */
  public void setAppContainerId(String appContainerId) {
    this.appContainerId = appContainerId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("appContainerId", appContainerId).toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String appContainerId;
    private String fileId;
    private FileBucket fileBucket;
    private String destinationFilePath;
    private String name;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;
    private boolean artifactNeeded;

    private Builder() {}

    /**
     * A copy app container command unit builder.
     *
     * @return the builder
     */
    public static Builder aCopyAppContainerCommandUnit() {
      return new Builder();
    }

    /**
     * With app container id builder.
     *
     * @param appContainerId the app container id
     * @return the builder
     */
    public Builder withAppContainerId(String appContainerId) {
      this.appContainerId = appContainerId;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCopyAppContainerCommandUnit()
          .withAppContainerId(appContainerId)
          .withFileId(fileId)
          .withFileBucket(fileBucket)
          .withDestinationFilePath(destinationFilePath)
          .withName(name)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult)
          .withArtifactNeeded(artifactNeeded);
    }

    /**
     * Build copy app container command unit.
     *
     * @return the copy app container command unit
     */
    public CopyAppContainerCommandUnit build() {
      CopyAppContainerCommandUnit copyAppContainerCommandUnit = new CopyAppContainerCommandUnit();
      copyAppContainerCommandUnit.setAppContainerId(appContainerId);
      copyAppContainerCommandUnit.setFileId(fileId);
      copyAppContainerCommandUnit.setFileBucket(fileBucket);
      copyAppContainerCommandUnit.setDestinationFilePath(destinationFilePath);
      copyAppContainerCommandUnit.setName(name);
      copyAppContainerCommandUnit.setCommandUnitType(commandUnitType);
      copyAppContainerCommandUnit.setExecutionResult(executionResult);
      copyAppContainerCommandUnit.setArtifactNeeded(artifactNeeded);
      return copyAppContainerCommandUnit;
    }
  }
}

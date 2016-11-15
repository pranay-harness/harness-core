package software.wings.beans.command;

import static java.util.stream.Collectors.toMap;
import static software.wings.beans.ErrorCodes.INVALID_REQUEST;
import static software.wings.beans.command.ScpCommandUnit.ScpFileCategory.ARTIFACTS;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.AppContainer;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.stencils.DataProvider;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Created by anubhaw on 7/14/16.
 */
@JsonTypeName("SCP")
public class ScpCommandUnit extends AbstractCommandUnit {
  @Attributes(title = "Source")
  @EnumData(enumDataProvider = ScpCommandDataProvider.class)
  private ScpFileCategory fileCategory;

  @Attributes(title = "Destination Path") @DefaultValue("$WINGS_RUNTIME_PATH") private String destinationDirectoryPath;

  @Inject @Transient private transient ServiceTemplateService serviceTemplateService;

  /**
   * Instantiates a new Scp command unit.
   */
  public ScpCommandUnit() {
    super(CommandUnitType.SCP);
  }

  @Override
  public ExecutionResult execute(CommandExecutionContext context) {
    List<Pair<String, String>> fileIds = Lists.newArrayList();
    FileBucket fileBucket = null;
    switch (fileCategory) {
      case ARTIFACTS:
        fileBucket = FileBucket.ARTIFACTS;
        context.getArtifact().getArtifactFiles().forEach(
            artifactFile -> fileIds.add(Pair.of(artifactFile.getFileUuid(), null)));
        break;
      case APPLICATION_STACK:
        fileBucket = FileBucket.PLATFORMS;
        AppContainer appContainer = context.getServiceTemplate().getService().getAppContainer();
        fileIds.add(Pair.of(appContainer.getFileUuid(), null));
        break;
      default:
        throw new WingsException(INVALID_REQUEST, "message", "Unsupported file category for copy step");
    }
    return context.copyGridFsFiles(destinationDirectoryPath, fileBucket, fileIds);
  }

  @SchemaIgnore
  @Override
  public boolean isArtifactNeeded() {
    return fileCategory.equals(ARTIFACTS);
  }

  /**
   * Gets file category.
   *
   * @return the file category
   */
  @SchemaIgnore
  public ScpFileCategory getFileCategory() {
    return fileCategory;
  }

  /**
   * Sets file category.
   *
   * @param fileCategory the file category
   */
  public void setFileCategory(ScpFileCategory fileCategory) {
    this.fileCategory = fileCategory;
  }

  /**
   * Gets destination directory path.
   *
   * @return the destination directory path
   */
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
    return Objects.hash(fileCategory, destinationDirectoryPath);
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
        && Objects.equals(this.destinationDirectoryPath, other.destinationDirectoryPath);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fileCategory", fileCategory)
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
    APPLICATION_STACK("Application Stack");

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
    private ScpFileCategory fileCategory;
    private String destinationDirectoryPath;
    private String name;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;
    private boolean artifactNeeded = false;

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
    public Builder withFileCategory(ScpFileCategory fileCategory) {
      this.fileCategory = fileCategory;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aScpCommandUnit()
          .withFileCategory(fileCategory)
          .withDestinationDirectoryPath(destinationDirectoryPath)
          .withName(name)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult)
          .withArtifactNeeded(artifactNeeded);
    }

    /**
     * Build scp command unit.
     *
     * @return the scp command unit
     */
    public ScpCommandUnit build() {
      ScpCommandUnit scpCommandUnit = new ScpCommandUnit();
      scpCommandUnit.setFileCategory(fileCategory);
      scpCommandUnit.setDestinationDirectoryPath(destinationDirectoryPath);
      scpCommandUnit.setName(name);
      scpCommandUnit.setCommandUnitType(commandUnitType);
      scpCommandUnit.setExecutionResult(executionResult);
      scpCommandUnit.setArtifactNeeded(artifactNeeded);
      return scpCommandUnit;
    }
  }

  /**
   * The type Scp command data provider.
   */
  public static class ScpCommandDataProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, String... params) {
      return Stream.of(ScpFileCategory.values()).collect(toMap(ScpFileCategory::name, ScpFileCategory::getName));
    }
  }
}

package software.wings.beans.command;

import static java.util.stream.Collectors.toMap;
import static software.wings.beans.command.ScpCommandUnit.ScpFileCategory.ARTIFACTS;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.InvalidRequestException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppContainer;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.stencils.DataProvider;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Created by anubhaw on 7/14/16.
 */
@JsonTypeName("SCP")
public class ScpCommandUnit extends SshCommandUnit {
  private static final Logger logger = LoggerFactory.getLogger(ScpCommandUnit.class);

  @Attributes(title = "Source")
  @EnumData(enumDataProvider = ScpCommandDataProvider.class)
  private ScpFileCategory fileCategory;

  @Attributes(title = "Destination Path") @DefaultValue("$WINGS_RUNTIME_PATH") private String destinationDirectoryPath;

  /**
   * Instantiates a new Scp command unit.
   */
  public ScpCommandUnit() {
    super(CommandUnitType.SCP);
  }

  @Override
  protected CommandExecutionStatus executeInternal(ShellCommandExecutionContext context) {
    List<Pair<String, String>> fileIds = Lists.newArrayList();
    FileBucket fileBucket;
    switch (fileCategory) {
      case ARTIFACTS:
        fileBucket = FileBucket.ARTIFACTS;
        // TODO: remove artifactStreamType check once code path added for all artifact stream types.
        final ArtifactStreamAttributes artifactStreamAttributes = context.getArtifactStreamAttributes();
        final String artifactStreamType = artifactStreamAttributes.getArtifactStreamType();
        if (artifactStreamAttributes.isMetadataOnly()) {
          if (artifactStreamType.equalsIgnoreCase(ArtifactStreamType.AMAZON_S3.name())) {
            return context.copyFiles(destinationDirectoryPath, artifactStreamAttributes, context.getAccountId(),
                context.getAppId(), context.getActivityId(), getName(),
                context.getHost() == null ? null : context.getHost().getPublicDns());
          } else if (artifactStreamType.equalsIgnoreCase(ArtifactStreamType.ARTIFACTORY.name())) {
            if (isArtifactTypeAllowedForArtifactory(artifactStreamAttributes.getArtifactType())) {
              if (artifactStreamAttributes.isCopyArtifactEnabledForArtifactory()) {
                return context.copyFiles(destinationDirectoryPath, artifactStreamAttributes, context.getAccountId(),
                    context.getAppId(), context.getActivityId(), getName(),
                    context.getHost() == null ? null : context.getHost().getPublicDns());
              } else {
                logger.info("Feature flag for copy artifact for artifactory not enabled.");
                return CommandExecutionStatus.SUCCESS;
              }
            } else {
              logger.info("Copy Artifact is not supported for artifact stream type: " + artifactStreamType
                  + " and artifact type: " + artifactStreamAttributes.getArtifactType());
              return CommandExecutionStatus.SUCCESS;
            }
          }
        } else {
          context.getArtifactFiles().forEach(artifactFile -> fileIds.add(Pair.of(artifactFile.getFileUuid(), null)));
        }
        break;
      case APPLICATION_STACK:
        fileBucket = FileBucket.PLATFORMS;
        AppContainer appContainer = context.getAppContainer();
        fileIds.add(Pair.of(appContainer.getFileUuid(), null));
        break;
      default:
        throw new InvalidRequestException("Unsupported file category for copy step");
    }
    return context.copyGridFsFiles(destinationDirectoryPath, fileBucket, fileIds);
  }

  private boolean isArtifactTypeAllowedForArtifactory(ArtifactType artifactType) {
    if (ArtifactType.JAR.equals(artifactType) || ArtifactType.TAR.equals(artifactType)
        || ArtifactType.WAR.equals(artifactType) || ArtifactType.ZIP.equals(artifactType)
        || ArtifactType.IIS.equals(artifactType) || ArtifactType.IIS_APP.equals(artifactType)
        || ArtifactType.IIS_VirtualDirectory.equals(artifactType) || ArtifactType.OTHER.equals(artifactType)) {
      return true;
    }
    return false;
  }

  @SchemaIgnore
  @Override
  public boolean isArtifactNeeded() {
    return fileCategory != null && fileCategory.equals(ARTIFACTS);
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
    ARTIFACTS("Application Artifacts"),
    /**
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
    @SuppressFBWarnings("ME_ENUM_FIELD_SETTER")
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
    private CommandExecutionStatus commandExecutionStatus;
    private boolean artifactNeeded;

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
     * @param commandExecutionStatus the execution result
     * @return the builder
     */
    public Builder withExecutionResult(CommandExecutionStatus commandExecutionStatus) {
      this.commandExecutionStatus = commandExecutionStatus;
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
          .withExecutionResult(commandExecutionStatus)
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
      scpCommandUnit.setCommandExecutionStatus(commandExecutionStatus);
      scpCommandUnit.setArtifactNeeded(artifactNeeded);
      return scpCommandUnit;
    }
  }

  /**
   * The type Scp command data provider.
   */
  @Singleton
  public static class ScpCommandDataProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, Map<String, String> params) {
      return Stream.of(ScpFileCategory.values()).collect(toMap(ScpFileCategory::name, ScpFileCategory::getName));
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("SCP")
  public static class Yaml extends SshCommandUnit.Yaml {
    // maps to fileCategory
    private String source;
    private String destinationDirectoryPath;

    public Yaml() {
      super(CommandUnitType.SCP.name());
    }

    @lombok.Builder
    public Yaml(String name, String deploymentType, String source, String destinationDirectoryPath) {
      super(name, CommandUnitType.SCP.name(), deploymentType);
      this.source = source;
      this.destinationDirectoryPath = destinationDirectoryPath;
    }
  }
}

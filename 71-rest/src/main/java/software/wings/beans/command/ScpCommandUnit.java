package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.WARN;
import static software.wings.beans.command.ScpCommandUnit.ScpFileCategory.ARTIFACTS;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.AppContainer;
import software.wings.beans.BambooConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackageFileInfo;
import software.wings.helpers.ext.azure.devops.AzureArtifactsService;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.service.impl.jenkins.JenkinsUtils;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.stencils.DataProvider;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.ArtifactType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by anubhaw on 7/14/16.
 */
@JsonTypeName("SCP")
@Slf4j
public class ScpCommandUnit extends SshCommandUnit {
  private static final String ARTIFACT_STRING = "artifact/";
  @Inject @Transient private transient DelegateLogService delegateLogService;
  @Inject @Transient private AzureArtifactsService azureArtifactsService;
  @Inject @Transient private EncryptionService encryptionService;
  @Inject @Transient private JenkinsUtils jenkinsUtil;
  @Inject @Transient private BambooService bambooService;
  @Inject @Transient private NexusService nexusService;

  @Attributes(title = "Source")
  @EnumData(enumDataProvider = ScpCommandDataProvider.class)
  private ScpFileCategory fileCategory;

  @Attributes(title = "Destination Path") @DefaultValue("$WINGS_RUNTIME_PATH") private String destinationDirectoryPath;
  private String artifactVariableName = ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME;

  /**
   * Instantiates a new Scp command unit.
   */
  public ScpCommandUnit() {
    super(CommandUnitType.SCP);
  }

  @Override
  protected CommandExecutionStatus executeInternal(ShellCommandExecutionContext context) {
    List<Pair<String, String>> fileIds = Lists.newArrayList();
    Artifact artifact = null;
    FileBucket fileBucket;
    switch (fileCategory) {
      case ARTIFACTS:
        fileBucket = FileBucket.ARTIFACTS;
        ArtifactStreamAttributes artifactStreamAttributes = null;
        if (context.isMultiArtifact()) {
          Map<String, Artifact> multiArtifactMap = context.getMultiArtifactMap();
          Map<String, ArtifactStreamAttributes> artifactStreamAttributesMap = context.getArtifactStreamAttributesMap();
          artifact = multiArtifactMap.get(artifactVariableName);
          if (artifact == null) {
            throw new InvalidRequestException(
                format(
                    "Artifact with variable name [%s] does not exist. Please check artifact variable name provided in Copy Artifact Command.",
                    artifactVariableName),
                WingsException.USER);
          }

          artifactStreamAttributes = artifactStreamAttributesMap.get(artifact.getUuid());

        } else {
          artifactStreamAttributes = context.getArtifactStreamAttributes();
        }
        // TODO: remove artifactStreamType check once code path added for all artifact stream types.

        final String artifactStreamType = artifactStreamAttributes.getArtifactStreamType();
        if (artifactStreamAttributes.isMetadataOnly()) {
          if (artifactStreamType.equalsIgnoreCase(ArtifactStreamType.AMAZON_S3.name())) {
            return context.copyFiles(destinationDirectoryPath, artifactStreamAttributes, context.getAccountId(),
                context.getAppId(), context.getActivityId(), getName(),
                context.getHost() == null ? null : context.getHost().getPublicDns());
          } else if (artifactStreamType.equalsIgnoreCase(ArtifactStreamType.ARTIFACTORY.name())) {
            if (isArtifactTypeAllowedForArtifactory(artifactStreamAttributes.getArtifactType())) {
              if (artifactStreamAttributes.isCopyArtifactEnabled()) {
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
          } else if (artifactStreamType.equalsIgnoreCase(ArtifactStreamType.AZURE_ARTIFACTS.name())) {
            List<AzureArtifactsPackageFileInfo> fileInfos = azureArtifactsService.listFiles(
                (AzureArtifactsConfig) artifactStreamAttributes.getServerSetting().getValue(),
                artifactStreamAttributes.getArtifactServerEncryptedDataDetails(), artifactStreamAttributes,
                artifactStreamAttributes.getMetadata(), false);
            if (isEmpty(fileInfos)) {
              return SUCCESS;
            }

            Map<String, String> metadata = artifactStreamAttributes.getMetadata();
            if (metadata == null) {
              metadata = new HashMap<>();
            }

            for (AzureArtifactsPackageFileInfo fileInfo : fileInfos) {
              metadata.put(ArtifactMetadataKeys.artifactFileName, fileInfo.getName());
              metadata.put(ArtifactMetadataKeys.artifactFileSize, Long.toString(fileInfo.getSize()));
              artifactStreamAttributes.setMetadata(metadata);
              CommandExecutionStatus executionStatus = context.copyFiles(destinationDirectoryPath,
                  artifactStreamAttributes, context.getAccountId(), context.getAppId(), context.getActivityId(),
                  getName(), context.getHost() == null ? null : context.getHost().getPublicDns());
              if (FAILURE == executionStatus) {
                return executionStatus;
              }
            }
            return SUCCESS;
          } else if (artifactStreamType.equalsIgnoreCase(ArtifactStreamType.JENKINS.name())) {
            if (artifactStreamAttributes.isCopyArtifactEnabled()) {
              if (isEmpty(artifactStreamAttributes.getArtifactFileMetadata())) {
                saveExecutionLog(context, WARN, "There are no artifacts to copy");
                return SUCCESS;
              }
              Map<String, String> metadata = artifactStreamAttributes.getMetadata();
              if (metadata == null) {
                throw new InvalidRequestException(
                    "No metadata found for artifact stream. Cannot proceed with copy artifact");
              }
              JenkinsConfig jenkinsConfig = (JenkinsConfig) artifactStreamAttributes.getServerSetting().getValue();
              encryptionService.decrypt(
                  jenkinsConfig, artifactStreamAttributes.getArtifactServerEncryptedDataDetails());
              Jenkins jenkins = jenkinsUtil.getJenkins(jenkinsConfig);

              for (ArtifactFileMetadata artifactFileMetadata : artifactStreamAttributes.getArtifactFileMetadata()) {
                metadata.put(ArtifactMetadataKeys.artifactFileName, artifactFileMetadata.getFileName());
                metadata.put(ArtifactMetadataKeys.artifactPath, artifactFileMetadata.getUrl());
                String artifactPathRegex = artifactFileMetadata.getUrl().substring(
                    artifactFileMetadata.getUrl().lastIndexOf(ARTIFACT_STRING) + ARTIFACT_STRING.length());
                metadata.put(ArtifactMetadataKeys.artifactFileSize,
                    String.valueOf(jenkins.getFileSize(artifactStreamAttributes.getJobName(),
                        metadata.get(ArtifactMetadataKeys.buildNo), artifactPathRegex)));
                artifactStreamAttributes.setMetadata(metadata);
                CommandExecutionStatus executionStatus = context.copyFiles(destinationDirectoryPath,
                    artifactStreamAttributes, context.getAccountId(), context.getAppId(), context.getActivityId(),
                    getName(), context.getHost() == null ? null : context.getHost().getPublicDns());
                if (FAILURE == executionStatus) {
                  saveExecutionLog(context, ERROR,
                      format("Copy Artifact failed for artifact %s", artifactFileMetadata.getFileName()));
                  return executionStatus;
                }
              }
            } else {
              logger.info("Feature flag for copy artifact for Jenkins not enabled.");
            }
            return SUCCESS;
          } else if (artifactStreamType.equalsIgnoreCase(ArtifactStreamType.BAMBOO.name())) {
            if (artifactStreamAttributes.isCopyArtifactEnabled()) {
              if (isEmpty(artifactStreamAttributes.getArtifactFileMetadata())) {
                saveExecutionLog(context, WARN, "There are no artifacts to copy");
                return SUCCESS;
              }
              Map<String, String> metadata = artifactStreamAttributes.getMetadata();
              if (metadata == null) {
                throw new InvalidRequestException(
                    "No metadata found for artifact stream. Cannot proceed with copy artifact");
              }
              BambooConfig bambooConfig = (BambooConfig) artifactStreamAttributes.getServerSetting().getValue();
              for (ArtifactFileMetadata artifactFileMetadata : artifactStreamAttributes.getArtifactFileMetadata()) {
                metadata.put(ArtifactMetadataKeys.artifactFileName, artifactFileMetadata.getFileName());
                metadata.put(ArtifactMetadataKeys.artifactPath, artifactFileMetadata.getUrl());
                metadata.put(ArtifactMetadataKeys.artifactFileSize,
                    String.valueOf(bambooService.getFileSize(bambooConfig,
                        artifactStreamAttributes.getArtifactServerEncryptedDataDetails(),
                        artifactFileMetadata.getFileName(), artifactFileMetadata.getUrl())));
                artifactStreamAttributes.setMetadata(metadata);
                CommandExecutionStatus executionStatus = context.copyFiles(destinationDirectoryPath,
                    artifactStreamAttributes, context.getAccountId(), context.getAppId(), context.getActivityId(),
                    getName(), context.getHost() == null ? null : context.getHost().getPublicDns());
                if (FAILURE == executionStatus) {
                  saveExecutionLog(context, ERROR,
                      format("Copy Artifact failed for artifact %s", artifactFileMetadata.getFileName()));
                  return executionStatus;
                }
              }
            } else {
              logger.info("Feature flag for copy artifact for Bamboo not enabled.");
            }
            return SUCCESS;
          } else if (artifactStreamType.equalsIgnoreCase(ArtifactStreamType.NEXUS.name())) {
            if (artifactStreamAttributes.isCopyArtifactEnabled()) {
              if (isEmpty(artifactStreamAttributes.getArtifactFileMetadata())) {
                saveExecutionLog(context, WARN, "There are no artifacts to copy");
                return SUCCESS;
              }
              Map<String, String> metadata = artifactStreamAttributes.getMetadata();
              if (metadata == null) {
                throw new InvalidRequestException(
                    "No metadata found for artifact stream. Cannot proceed with copy artifact");
              }
              NexusConfig nexusConfig = (NexusConfig) artifactStreamAttributes.getServerSetting().getValue();
              for (ArtifactFileMetadata artifactFileMetadata : artifactStreamAttributes.getArtifactFileMetadata()) {
                metadata.put(ArtifactMetadataKeys.artifactFileName, artifactFileMetadata.getFileName());
                metadata.put(ArtifactMetadataKeys.artifactPath, artifactFileMetadata.getUrl());
                metadata.put(ArtifactMetadataKeys.artifactFileSize,
                    String.valueOf(nexusService.getFileSize(nexusConfig,
                        artifactStreamAttributes.getArtifactServerEncryptedDataDetails(),
                        artifactFileMetadata.getFileName(), artifactFileMetadata.getUrl())));
                artifactStreamAttributes.setMetadata(metadata);
                CommandExecutionStatus executionStatus = context.copyFiles(destinationDirectoryPath,
                    artifactStreamAttributes, context.getAccountId(), context.getAppId(), context.getActivityId(),
                    getName(), context.getHost() == null ? null : context.getHost().getPublicDns());
                if (FAILURE == executionStatus) {
                  saveExecutionLog(context, ERROR,
                      format("Copy Artifact failed for artifact %s", artifactFileMetadata.getFileName()));
                  return executionStatus;
                }
              }
            } else {
              logger.info("Feature flag for copy artifact for Nexus not enabled.");
            }
            return SUCCESS;
          } else if (artifactStreamType.equalsIgnoreCase(ArtifactStreamType.CUSTOM.name())) {
            saveExecutionLog(context, ERROR, "Copy Artifact is not supported for Custom Repository artifacts");
            throw new InvalidRequestException("Copy Artifact is not supported for Custom Repository artifacts");
          }
        } else {
          if (context.isMultiArtifact()) {
            if (artifact != null && isNotEmpty(artifact.getArtifactFiles())) {
              artifact.getArtifactFiles().forEach(
                  artifactFile -> fileIds.add(Pair.of(artifactFile.getFileUuid(), null)));
            } else {
              throw new InvalidRequestException(
                  format("No artifact files to copy for artifact %s", artifactVariableName));
            }
          } else {
            context.getArtifactFiles().forEach(artifactFile -> fileIds.add(Pair.of(artifactFile.getFileUuid(), null)));
          }
        }
        break;
      case APPLICATION_STACK:
        fileBucket = FileBucket.PLATFORMS;
        AppContainer appContainer = context.getAppContainer();
        fileIds.add(Pair.of(appContainer.getFileUuid(), null));
        break;
      default:
        saveExecutionLog(context, ERROR, "Unsupported file category for copy step");
        throw new InvalidRequestException("Unsupported file category for copy step");
    }
    return context.copyGridFsFiles(destinationDirectoryPath, fileBucket, fileIds);
  }

  private boolean isArtifactTypeAllowedForArtifactory(ArtifactType artifactType) {
    if (ArtifactType.JAR == artifactType || ArtifactType.TAR == artifactType || ArtifactType.WAR == artifactType
        || ArtifactType.ZIP == artifactType || ArtifactType.IIS == artifactType || ArtifactType.IIS_APP == artifactType
        || ArtifactType.IIS_VirtualDirectory == artifactType || ArtifactType.OTHER == artifactType) {
      return true;
    }
    return false;
  }

  private void saveExecutionLog(ShellCommandExecutionContext context, LogLevel logLevel, String line) {
    delegateLogService.save(context.getAccountId(),
        aLog()
            .withAppId(context.getAppId())
            .withActivityId(context.getActivityId())
            .withHostName(context.getHost().getPublicDns())
            .withLogLevel(logLevel)
            .withCommandUnitName(getName())
            .withLogLine(line)
            .withExecutionResult(RUNNING)
            .build());
  }

  @SchemaIgnore
  @Override
  public boolean isArtifactNeeded() {
    return fileCategory != null && fileCategory == ARTIFACTS;
  }

  @SchemaIgnore
  @Override
  public void updateServiceArtifactVariableNames(Set<String> serviceArtifactVariableNames) {
    if (isArtifactNeeded()) {
      serviceArtifactVariableNames.add(getArtifactVariableName());
    }
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

  public String getArtifactVariableName() {
    return artifactVariableName == null ? ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME : artifactVariableName;
  }

  public void setArtifactVariableName(String artifactVariableName) {
    this.artifactVariableName = artifactVariableName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileCategory, destinationDirectoryPath, getArtifactVariableName());
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
    return this.fileCategory == other.fileCategory
        && Objects.equals(this.destinationDirectoryPath, other.destinationDirectoryPath)
        && Objects.equals(this.getArtifactVariableName(), other.getArtifactVariableName());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fileCategory", fileCategory)
        .add("destinationDirectoryPath", destinationDirectoryPath)
        .add("artifactVariableName", this.getArtifactVariableName())
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
    private String artifactVariableName;

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

    public Builder withArtifactVariableName(String artifactVariableName) {
      this.artifactVariableName = artifactVariableName;
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
          .withArtifactNeeded(artifactNeeded)
          .withArtifactVariableName(artifactVariableName);
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
      scpCommandUnit.setArtifactVariableName(artifactVariableName);
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
    private String artifactVariableName;

    public Yaml() {
      super(CommandUnitType.SCP.name());
    }

    @lombok.Builder
    public Yaml(String name, String deploymentType, String source, String destinationDirectoryPath,
        String artifactVariableName) {
      super(name, CommandUnitType.SCP.name(), deploymentType);
      this.source = source;
      this.destinationDirectoryPath = destinationDirectoryPath;
      this.artifactVariableName = artifactVariableName;
    }
  }
}

package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ConfigFile;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateConfigService;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.stencils.DefaultValue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 7/14/16.
 */
@JsonTypeName("COPY_CONFIGS")
public class CopyConfigCommandUnit extends SshCommandUnit {
  private static final Logger logger = LoggerFactory.getLogger(CopyConfigCommandUnit.class);

  @Attributes(title = "Destination Parent Path")
  @DefaultValue("$WINGS_RUNTIME_PATH")
  private String destinationParentPath;

  @Inject @Transient private transient DelegateConfigService delegateConfigService;

  @Inject @Transient private transient DelegateFileManager delegateFileManager;

  @Inject @Transient private transient DelegateLogService delegateLogService;

  /**
   * Instantiates a new Scp command unit.
   */
  public CopyConfigCommandUnit() {
    super(CommandUnitType.COPY_CONFIGS);
  }

  @Override
  public CommandExecutionStatus executeInternal(ShellCommandExecutionContext context) {
    List<ConfigFile> configFiles = null;
    try {
      configFiles = delegateConfigService.getConfigFiles(context.getAppId(), context.getEnvId(),
          context.getServiceTemplateId(), context.getHost().getUuid(), context.getAccountId());
    } catch (IOException e) {
      delegateLogService.save(context.getAccountId(),
          aLog()
              .withAppId(context.getAppId())
              .withActivityId(context.getActivityId())
              .withHostName(context.getHost().getPublicDns())
              .withLogLevel(ERROR)
              .withCommandUnitName(getName())
              .withLogLine("Unable to fetch config file information")
              .withExecutionResult(FAILURE)
              .build());
      logger.error("Unable to fetch log file information", e);
      return FAILURE;
    }

    CommandExecutionStatus result = CommandExecutionStatus.SUCCESS;
    if (isNotEmpty(configFiles)) {
      for (ConfigFile configFile : configFiles) {
        File destFile = new File(configFile.getRelativeFilePath());
        String path = destinationParentPath + "/" + (isNotBlank(destFile.getParent()) ? destFile.getParent() : "");
        try {
          delegateFileManager.getFileIdByVersion(FileBucket.CONFIGS, configFile.getUuid(),
              configFile.getVersionForEnv(context.getEnvId()), context.getAccountId());
        } catch (IOException e) {
          String message = "Unable to get config file for entityId: " + configFile.getUuid()
              + ", version: " + configFile.getVersionForEnv(context.getEnvId());
          logger.error(message, e);
          delegateLogService.save(context.getAccountId(),
              aLog()
                  .withAppId(context.getAppId())
                  .withActivityId(context.getActivityId())
                  .withHostName(context.getHost().getPublicDns())
                  .withLogLevel(ERROR)
                  .withCommandUnitName(getName())
                  .withLogLine(message)
                  .withExecutionResult(FAILURE)
                  .build());
          result = FAILURE;
          break;
        }
        ConfigFileMetaData configFileMetaData = ConfigFileMetaData.builder()
                                                    .destinationDirectoryPath(path)
                                                    .fileId(configFile.getUuid())
                                                    .filename(destFile.getName())
                                                    .length(configFile.getSize())
                                                    .encrypted(configFile.isEncrypted())
                                                    .activityId(context.getActivityId())
                                                    .build();
        result = context.copyConfigFiles(configFileMetaData) == FAILURE ? FAILURE : CommandExecutionStatus.SUCCESS;
        if (FAILURE == result) {
          break;
        }
      }
    }
    return result;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class ConfigFileMetaData {
    private String fileId;
    private Long length;
    private String filename;
    private String destinationDirectoryPath;
    private FileBucket fileBucket;
    private boolean encrypted;
    private String activityId;
  }

  /**
   * Gets destination parent path.
   *
   * @return the destination parent path
   */
  public String getDestinationParentPath() {
    return destinationParentPath;
  }

  /**
   * Sets destination parent path.
   *
   * @param destinationParentPath the destination parent path
   */
  public void setDestinationParentPath(String destinationParentPath) {
    this.destinationParentPath = destinationParentPath;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("destinationParentPath", destinationParentPath).toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(destinationParentPath);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CopyConfigCommandUnit other = (CopyConfigCommandUnit) obj;
    return Objects.equals(this.destinationParentPath, other.destinationParentPath);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("COPY_CONFIGS")
  public static class Yaml extends AbstractCommandUnit.Yaml {
    private String destinationParentPath;

    public Yaml() {
      super(CommandUnitType.COPY_CONFIGS.name());
    }

    @Builder
    public Yaml(String name, String deploymentType, String destinationParentPath) {
      super(name, CommandUnitType.COPY_CONFIGS.name(), deploymentType);
      this.destinationParentPath = destinationParentPath;
    }
  }
}

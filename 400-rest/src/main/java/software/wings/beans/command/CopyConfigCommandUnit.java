package software.wings.beans.command;

import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;

import static software.wings.beans.Log.Builder.aLog;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.FileBucket;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.ConfigFile;
import software.wings.beans.Log;
import software.wings.delegatetasks.DelegateConfigService;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.stencils.DefaultValue;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;

/**
 * Created by anubhaw on 7/14/16.
 */
@JsonTypeName("COPY_CONFIGS")
@Slf4j
public class CopyConfigCommandUnit extends SshCommandUnit {
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
    List<ConfigFile> configFiles;
    String hostName = context.getHost() == null ? null : context.getHost().getPublicDns();
    try {
      configFiles =
          delegateConfigService.getConfigFiles(context.getAppId(), context.getEnvId(), context.getServiceTemplateId(),
              context.getHost() == null ? null : context.getHost().getUuid(), context.getAccountId());
    } catch (IOException e) {
      Log.Builder logBuilder = aLog()
                                   .appId(context.getAppId())
                                   .activityId(context.getActivityId())
                                   .logLevel(ERROR)
                                   .commandUnitName(getName())
                                   .logLine("Unable to fetch config file information")
                                   .executionResult(FAILURE);
      if (hostName != null) {
        logBuilder.hostName(hostName);
      }
      delegateLogService.save(context.getAccountId(), logBuilder.build());
      log.error("Unable to fetch log file information", e);
      return FAILURE;
    }

    CommandExecutionStatus result = CommandExecutionStatus.SUCCESS;
    if (hasSome(configFiles)) {
      for (ConfigFile configFile : configFiles) {
        File destFile = new File(configFile.getRelativeFilePath());
        String path = destinationParentPath + "/" + (isNotBlank(destFile.getParent()) ? destFile.getParent() : "");
        try {
          delegateFileManager.getFileIdByVersion(FileBucket.CONFIGS, configFile.getUuid(),
              configFile.getVersionForEnv(context.getEnvId()), context.getAccountId());
        } catch (IOException e) {
          String message = "Unable to get config file for entityId: " + configFile.getUuid()
              + ", version: " + configFile.getVersionForEnv(context.getEnvId());
          log.error(message, e);
          Log.Builder logBuilder = aLog()
                                       .appId(context.getAppId())
                                       .activityId(context.getActivityId())
                                       .logLevel(ERROR)
                                       .commandUnitName(getName())
                                       .logLine(message)
                                       .executionResult(FAILURE);
          if (hostName != null) {
            logBuilder.hostName(hostName);
          }
          delegateLogService.save(context.getAccountId(), logBuilder.build());
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
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
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

package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.Collections;
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
  public CommandExecutionStatus executeInternal(SshCommandExecutionContext context) {
    List<ConfigFile> configFiles = null;
    try {
      configFiles = delegateConfigService.getConfigFiles(context.getAppId(), context.getEnvId(),
          context.getServiceTemplateId(), context.getHost().getUuid(), context.getAccountId());
    } catch (IOException e) {
      delegateLogService.save(context.getAccountId(),
          aLog()
              .withAppId(context.getAppId())
              .withActivityId(context.getActivityId())
              .withHostName(context.getHost().getHostName())
              .withLogLevel(ERROR)
              .withCommandUnitName(getName())
              .withLogLine("Unable to fetch config file information")
              .build());
      logger.error("Unable to fetch log file information ", e);
      return CommandExecutionStatus.FAILURE;
    }

    CommandExecutionStatus result = CommandExecutionStatus.SUCCESS;
    if (!isEmpty(configFiles)) {
      for (ConfigFile configFile : configFiles) {
        File destFile = new File(configFile.getRelativeFilePath());
        String path = destinationParentPath + "/" + (isNotBlank(destFile.getParent()) ? destFile.getParent() : "");
        String fileId = null;
        try {
          fileId = delegateFileManager.getFileIdByVersion(FileBucket.CONFIGS, configFile.getUuid(),
              configFile.getVersionForEnv(context.getEnvId()), context.getAccountId());
        } catch (IOException e) {
          String message = "Unable to get config file for entityId: " + configFile.getUuid()
              + ", version: " + configFile.getVersionForEnv(context.getEnvId());
          logger.error(message, e);
          delegateLogService.save(context.getAccountId(),
              aLog()
                  .withAppId(context.getAppId())
                  .withActivityId(context.getActivityId())
                  .withHostName(context.getHost().getHostName())
                  .withLogLevel(ERROR)
                  .withCommandUnitName(getName())
                  .withLogLine(message)
                  .build());
          result = CommandExecutionStatus.FAILURE;
          break;
        }
        result = (context).copyGridFsFiles(
                     path, FileBucket.CONFIGS, Collections.singletonList(Pair.of(fileId, destFile.getName())))
                == CommandExecutionStatus.FAILURE
            ? CommandExecutionStatus.FAILURE
            : CommandExecutionStatus.SUCCESS;
        if (CommandExecutionStatus.FAILURE == result) {
          break;
        }
      }
    }
    return result;
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
}

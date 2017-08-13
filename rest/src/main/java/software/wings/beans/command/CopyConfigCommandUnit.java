package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
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
import software.wings.utils.Misc;

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
                  .withHostName(context.getHost().getPublicDns())
                  .withLogLevel(ERROR)
                  .withCommandUnitName(getName())
                  .withLogLine(message)
                  .withExecutionResult(FAILURE)
                  .build());
          result = FAILURE;
          break;
        }
        //        result = (context).copyGridFsFiles(path, FileBucket.CONFIGS, Collections.singletonList(Pair.of(fileId,
        //        destFile.getName()))) == FAILURE ?
        //            FAILURE :
        //            CommandExecutionStatus.SUCCESS;
        ConfigFileMetaData configFileMetaData = ConfigFileMetaData.newBuilder()
                                                    .withDestinationDirectoryPath(path)
                                                    .withFileBucket(FileBucket.CONFIGS)
                                                    .withFileId(fileId)
                                                    .withFilename(destFile.getName())
                                                    .withLength(configFile.getSize())
                                                    .withEncrypted(configFile.isEncrypted())
                                                    .build();
        result = (context).copyConfigFiles(configFileMetaData) == FAILURE ? FAILURE : CommandExecutionStatus.SUCCESS;
        if (FAILURE == result) {
          break;
        }
      }
    }
    return result;
  }

  public static class ConfigFileMetaData {
    private String fileId;
    private Long length;
    private String filename;
    private String destinationDirectoryPath;
    private FileBucket fileBucket;
    private boolean encrypted;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("fileId", fileId)
          .add("length", length)
          .add("filename", filename)
          .add("destinationDirectoryPath", destinationDirectoryPath)
          .add("fileBucket", fileBucket)
          .add("encrypted", encrypted)
          .toString();
    }

    private ConfigFileMetaData(Builder builder) {
      setFileId(builder.fileId);
      setLength(builder.length);
      setFilename(builder.filename);
      setDestinationDirectoryPath(builder.destinationDirectoryPath);
      setFileBucket(builder.fileBucket);
      setEncrypted(builder.encrypted);
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public static Builder newBuilder(ConfigFileMetaData copy) {
      Builder builder = new Builder();
      builder.fileId = copy.fileId;
      builder.length = copy.length;
      builder.filename = copy.filename;
      builder.destinationDirectoryPath = copy.destinationDirectoryPath;
      builder.fileBucket = copy.fileBucket;
      builder.encrypted = copy.encrypted;
      return builder;
    }

    public String getFileId() {
      return fileId;
    }

    public void setFileId(String fileId) {
      this.fileId = fileId;
    }

    public Long getLength() {
      return length;
    }

    public void setLength(Long length) {
      this.length = length;
    }

    public String getFilename() {
      return filename;
    }

    public void setFilename(String filename) {
      this.filename = filename;
    }

    public String getDestinationDirectoryPath() {
      return destinationDirectoryPath;
    }

    public void setDestinationDirectoryPath(String destinationDirectoryPath) {
      this.destinationDirectoryPath = destinationDirectoryPath;
    }

    public FileBucket getFileBucket() {
      return fileBucket;
    }

    public void setFileBucket(FileBucket fileBucket) {
      this.fileBucket = fileBucket;
    }

    public boolean isEncrypted() {
      return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
      this.encrypted = encrypted;
    }

    public static final class Builder {
      private String fileId;
      private Long length;
      private String filename;
      private String destinationDirectoryPath;
      private FileBucket fileBucket;
      private boolean encrypted;

      private Builder() {}

      public Builder withFileId(String fileId) {
        this.fileId = fileId;
        return this;
      }

      public Builder withLength(Long length) {
        this.length = length;
        return this;
      }

      public Builder withFilename(String filename) {
        this.filename = filename;
        return this;
      }

      public Builder withDestinationDirectoryPath(String destinationDirectoryPath) {
        this.destinationDirectoryPath = destinationDirectoryPath;
        return this;
      }

      public Builder withFileBucket(FileBucket fileBucket) {
        this.fileBucket = fileBucket;
        return this;
      }

      public Builder withEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
        return this;
      }

      public ConfigFileMetaData build() {
        return new ConfigFileMetaData(this);
      }
    }
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

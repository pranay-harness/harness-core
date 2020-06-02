package software.wings.core.ssh.executors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogLevel.WARN;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 2019-03-11.
 */

@ValidateOnExecution
@Slf4j
public abstract class AbstractScriptExecutor implements ScriptExecutor {
  static final String UUID = generateUuid();
  static final String HARNESS_START_TOKEN = "harness_start_token_" + UUID;
  static final String HARNESS_END_TOKEN = "harness_end_token_" + UUID;

  /**
   * The Config.
   */

  /**
   * The Log service.
   */
  protected DelegateLogService logService;
  /**
   * The File service.
   */
  protected DelegateFileManager delegateFileManager;

  /**
   * Instantiates a new abstract ssh executor.
   *
   * @param delegateFileManager the file service
   * @param logService          the log service
   */
  @Inject
  public AbstractScriptExecutor(DelegateFileManager delegateFileManager, DelegateLogService logService) {
    this.logService = logService;
    this.delegateFileManager = delegateFileManager;
  }

  @Override
  public abstract CommandExecutionStatus executeCommandString(
      String command, StringBuffer output, boolean displayCommand);

  @Override
  public abstract CommandExecutionResult executeCommandString(String command, List<String> envVariablesToCollect);

  public abstract String getAccountId();

  public abstract String getCommandUnitName();

  public abstract String getAppId();

  public abstract String getExecutionId();

  public abstract String getHost();

  public abstract CommandExecutionStatus scpOneFile(String remoteFilePath, FileProvider fileProvider);

  @Override
  public CommandExecutionStatus executeCommandString(String command) {
    return executeCommandString(command, null, false);
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command, boolean displayCommand) {
    return executeCommandString(command, null, displayCommand);
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command, StringBuffer output) {
    return executeCommandString(command, output, false);
  }

  @Override
  public CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds) {
    if (isEmpty(fileNamesIds)) {
      saveExecutionLog("There are no artifacts to copy.");
      return CommandExecutionStatus.SUCCESS;
    }

    return fileNamesIds.stream()
        .map(fileNamesId
            -> scpOneFile(destinationDirectoryPath,
                new FileProvider() {
                  @Override
                  public Pair<String, Long> getInfo() throws IOException {
                    DelegateFile delegateFile;
                    try {
                      delegateFile = delegateFileManager.getMetaInfo(fileBucket, fileNamesId.getKey(), getAccountId());
                    } catch (WingsException e) {
                      saveExecutionLogError(e.getMessage());
                      throw e;
                    }
                    return ImmutablePair.of(
                        isBlank(fileNamesId.getRight()) ? delegateFile.getFileName() : fileNamesId.getRight(),
                        delegateFile.getLength());
                  }

                  @Override
                  public void downloadToStream(OutputStream outputStream) throws IOException, ExecutionException {
                    try (InputStream inputStream = delegateFileManager.downloadArtifactByFileId(
                             fileBucket, fileNamesId.getKey(), getAccountId())) {
                      IOUtils.copy(inputStream, outputStream);
                    }
                  }
                }))
        .filter(commandExecutionStatus -> commandExecutionStatus == FAILURE)
        .findFirst()
        .orElse(CommandExecutionStatus.SUCCESS);
  }

  @Override
  public CommandExecutionStatus copyConfigFiles(ConfigFileMetaData configFileMetaData) {
    if (isBlank(configFileMetaData.getFileId()) || isBlank(configFileMetaData.getFilename())) {
      saveExecutionLog("There are no artifacts to copy. " + configFileMetaData.toString());
      return CommandExecutionStatus.SUCCESS;
    }
    return scpOneFile(configFileMetaData.getDestinationDirectoryPath(), new FileProvider() {
      @Override
      public Pair<String, Long> getInfo() {
        return ImmutablePair.of(configFileMetaData.getFilename(), configFileMetaData.getLength());
      }

      @Override
      public void downloadToStream(OutputStream outputStream) throws IOException {
        try (InputStream inputStream = delegateFileManager.downloadByConfigFileId(
                 configFileMetaData.getFileId(), getAccountId(), getAppId(), configFileMetaData.getActivityId())) {
          IOUtils.copy(inputStream, outputStream);
        }
      }
    });
  }

  @Override
  public CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files) {
    return files.stream()
        .map(file
            -> scpOneFile(destinationDirectoryPath,
                new FileProvider() {
                  @Override
                  public Pair<String, Long> getInfo() throws IOException {
                    File file1 = new File(file);
                    return ImmutablePair.of(file1.getName(), file1.length());
                  }

                  @Override
                  public void downloadToStream(OutputStream outputStream) throws IOException {
                    try (FileInputStream fis = new FileInputStream(file)) {
                      IOUtils.copy(fis, outputStream);
                    }
                  }
                }))
        .filter(commandExecutionStatus -> commandExecutionStatus == CommandExecutionStatus.FAILURE)
        .findFirst()
        .orElse(CommandExecutionStatus.SUCCESS);
  }

  @Override
  public CommandExecutionStatus copyFiles(String destinationDirectoryPath,
      ArtifactStreamAttributes artifactStreamAttributes, String accountId, String appId, String activityId,
      String commandUnitName, String hostName) {
    Map<String, String> metadata = artifactStreamAttributes.getMetadata();
    return scpOneFile(destinationDirectoryPath, new FileProvider() {
      @Override
      public Pair<String, Long> getInfo() {
        if (!metadata.containsKey(ArtifactMetadataKeys.artifactFileSize)) {
          Long artifactFileSize = delegateFileManager.getArtifactFileSize(artifactStreamAttributes);
          metadata.put(ArtifactMetadataKeys.artifactFileSize, String.valueOf(artifactFileSize));
        }
        String fileName = metadata.get(ArtifactMetadataKeys.artifactFileName);
        int lastIndexOfSlash = fileName.lastIndexOf('/');
        if (lastIndexOfSlash > 0) {
          saveExecutionLogWarn("Filename contains slashes. Stripping off the portion before last slash.");
          logger.warn("Filename contains slashes. Stripping off the portion before last slash.");
          fileName = fileName.substring(lastIndexOfSlash + 1);
          saveExecutionLogWarn("Got filename: " + fileName);
          logger.warn("Got filename: " + fileName);
        }

        return ImmutablePair.of(fileName, Long.parseLong(metadata.get(ArtifactMetadataKeys.artifactFileSize)));
      }

      @Override
      public void downloadToStream(OutputStream outputStream) throws IOException, ExecutionException {
        try (InputStream inputStream = delegateFileManager.downloadArtifactAtRuntime(
                 artifactStreamAttributes, accountId, appId, activityId, commandUnitName, hostName)) {
          IOUtils.copy(inputStream, outputStream);
        }
      }
    });
  }

  protected String addEnvVariablesCollector(
      String command, List<String> envVariablesToCollect, String envVariablesOutputFilePath) {
    StringBuilder wrapperCommand = new StringBuilder(command);
    wrapperCommand.append('\n');
    String redirect = ">";
    for (String env : envVariablesToCollect) {
      wrapperCommand.append("echo ")
          .append(HARNESS_START_TOKEN)
          .append(' ')
          .append(env)
          .append("=\"$")
          .append(env)
          .append("\" ")
          .append(HARNESS_END_TOKEN)
          .append(' ')
          .append(redirect)
          .append(envVariablesOutputFilePath)
          .append('\n');
      redirect = ">>";
    }
    return wrapperCommand.toString();
  }

  protected void saveExecutionLog(String line) {
    saveExecutionLog(line, RUNNING);
  }

  protected void processScriptOutputFile(@NotNull Map<String, String> envVariablesMap, @NotNull BufferedReader br)
      throws IOException {
    saveExecutionLog("Script Output: ");
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
      sb.append('\n');
      if (line.endsWith(HARNESS_END_TOKEN)) {
        String envVar = sb.toString();
        envVar = StringUtils.substringBetween(envVar, HARNESS_START_TOKEN, HARNESS_END_TOKEN);
        int index = envVar.indexOf('=');
        if (index != -1) {
          String key = envVar.substring(0, index).trim();
          String value = envVar.substring(index + 1).trim();
          if (StringUtils.isNotBlank(key)) {
            envVariablesMap.put(key, value);
            saveExecutionLog(key + "=" + value);
          }
          sb = new StringBuilder();
        }
      }
    }
  }

  protected void saveExecutionLog(String line, CommandExecutionStatus commandExecutionStatus) {
    logService.save(getAccountId(),
        aLog()
            .withAppId(getAppId())
            .withActivityId(getExecutionId())
            .withLogLevel(INFO)
            .withCommandUnitName(getCommandUnitName())
            .withHostName(getHost())
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }

  protected void saveExecutionLogError(String line) {
    logService.save(getAccountId(),
        aLog()
            .withAppId(getAppId())
            .withActivityId(getExecutionId())
            .withLogLevel(ERROR)
            .withCommandUnitName(getCommandUnitName())
            .withHostName(getHost())
            .withLogLine(line)
            .withExecutionResult(RUNNING)
            .build());
  }

  protected void saveExecutionLogWarn(String line) {
    logService.save(getAccountId(),
        aLog()
            .withAppId(getAppId())
            .withActivityId(getExecutionId())
            .withLogLevel(WARN)
            .withCommandUnitName(getCommandUnitName())
            .withHostName(getHost())
            .withLogLine(line)
            .withExecutionResult(RUNNING)
            .build());
  }

  /**
   * The interface File provider.
   */
  public interface FileProvider {
    /**
     * Gets info.
     *
     * @return the info
     * @throws IOException the io exception
     */
    Pair<String, Long> getInfo() throws IOException;

    /**
     * Download to stream.
     *
     * @param outputStream the output stream
     * @throws IOException the io exception
     */
    void downloadToStream(OutputStream outputStream) throws IOException, ExecutionException;
  }
}

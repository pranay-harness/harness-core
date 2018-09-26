package software.wings.core.ssh.executors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ERROR_IN_GETTING_CHANNEL_STREAMS;
import static io.harness.eraro.ErrorCode.INVALID_EXECUTION_ID;
import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogLevel.WARN;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.common.Constants.ARTIFACT_FILE_NAME;
import static software.wings.common.Constants.ARTIFACT_FILE_SIZE;
import static software.wings.utils.Misc.getMessage;
import static software.wings.utils.SshHelperUtil.normalizeError;

import com.google.inject.Inject;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.WingsException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CopyConfigCommandUnit.ConfigFileMetaData;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.Misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;
/**
 * Created by anubhaw on 2/10/16.
 */
@ValidateOnExecution
public abstract class AbstractSshExecutor implements SshExecutor {
  /**
   * The constant DEFAULT_SUDO_PROMPT_PATTERN.
   */
  public static final String DEFAULT_SUDO_PROMPT_PATTERN = "^\\[sudo\\] password for .+: .*";
  /**
   * The constant LINE_BREAK_PATTERN.
   */
  public static final String LINE_BREAK_PATTERN = "\\R+";
  /**
   * The constant logger.
   */
  protected static final Logger logger = LoggerFactory.getLogger(AbstractSshExecutor.class);
  private static final int MAX_BYTES_READ_PER_CHANNEL =
      1024 * 1024 * 1024; // TODO: Read from config. 1 GB per channel for now.
  private static ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
  /**
   * The Config.
   */
  protected SshSessionConfig config;

  /**
   * The Log service.
   */
  protected DelegateLogService logService;
  /**
   * The File service.
   */
  protected DelegateFileManager delegateFileManager;
  private Pattern sudoPasswordPromptPattern = Pattern.compile(DEFAULT_SUDO_PROMPT_PATTERN);
  private Pattern lineBreakPattern = Pattern.compile(LINE_BREAK_PATTERN);

  /**
   * Instantiates a new abstract ssh executor.
   *
   * @param delegateFileManager the file service
   * @param logService          the log service
   */
  @Inject
  public AbstractSshExecutor(DelegateFileManager delegateFileManager, DelegateLogService logService) {
    this.logService = logService;
    this.delegateFileManager = delegateFileManager;
  }

  /**
   * Evict and disconnect cached session.
   *
   * @param executionId the execution id
   * @param hostName    the host name
   */
  public static void evictAndDisconnectCachedSession(String executionId, String hostName) {
    logger.info("Clean up session for executionId : {}, hostName: {} ", executionId, hostName);
    String key = executionId + "~" + hostName.trim();
    Session session = sessions.remove(key);
    if (session != null && session.isConnected()) {
      logger.info("Found cached session. disconnecting the session");
      session.disconnect();
      logger.info("Session disconnected successfully");
    } else {
      logger.info("No cached session found for executionId : {}, hostName: {} ", executionId, hostName);
    }
  }

  /* (non-Javadoc)
   * @see software.wings.core.ssh.executors.SshExecutor#init(software.wings.core.ssh.executors.SshSessionConfig)
   */
  @Override
  public void init(@Valid SshSessionConfig config) {
    if (isEmpty(config.getExecutionId())) {
      throw new WingsException(INVALID_EXECUTION_ID);
    }
    this.config = config;
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command) {
    return executeCommandString(command, null, true);
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command, boolean displayCommand) {
    return executeCommandString(command, null, displayCommand);
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION") // TODO
  @Override
  public CommandExecutionStatus executeCommandString(String command, StringBuffer output) {
    return executeCommandString(command, output, true);
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION") // TODO
  @Override
  public CommandExecutionStatus executeCommandString(String command, StringBuffer output, boolean displayCommand) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    Channel channel = null;
    long start = System.currentTimeMillis();
    try {
      saveExecutionLog(format("Initializing SSH connection to %s ....", config.getHost()));
      channel = getCachedSession(this.config).openChannel("exec");
      logger.info("Session fetched in " + (System.currentTimeMillis() - start) + " ms");

      ((ChannelExec) channel).setPty(true);
      try (OutputStream outputStream = channel.getOutputStream(); InputStream inputStream = channel.getInputStream()) {
        ((ChannelExec) channel).setCommand(command);
        saveExecutionLog(format("Connecting to %s ....", config.getHost()));
        channel.connect();
        saveExecutionLog(format("Connection to %s established", config.getHost()));
        if (displayCommand) {
          saveExecutionLog(format("Executing command %s ...", command));
        } else {
          saveExecutionLog("Executing command ...");
        }

        int totalBytesRead = 0;
        byte[] byteBuffer = new byte[1024];
        String text = "";

        while (true) {
          while (inputStream.available() > 0) {
            int numOfBytesRead = inputStream.read(byteBuffer, 0, 1024);
            if (numOfBytesRead < 0) {
              break;
            }
            totalBytesRead += numOfBytesRead;
            if (totalBytesRead >= MAX_BYTES_READ_PER_CHANNEL) {
              // TODO: better error reporting
              throw new WingsException(UNKNOWN_ERROR);
            }
            String dataReadFromTheStream = new String(byteBuffer, 0, numOfBytesRead, UTF_8);
            if (output != null) {
              output.append(dataReadFromTheStream);
            }

            text += dataReadFromTheStream;
            text = processStreamData(text, false, outputStream);
          }

          if (text.length() > 0) {
            text = processStreamData(text, true, outputStream); // finished reading. update logs
          }

          if (channel.isClosed()) {
            commandExecutionStatus = channel.getExitStatus() == 0 ? SUCCESS : FAILURE;
            saveExecutionLog("Command finished with status " + commandExecutionStatus, commandExecutionStatus);
            return commandExecutionStatus;
          }
          sleep(Duration.ofSeconds(1));
        }
      }
    } catch (Exception ex) {
      logger.error("ex-Session fetched in " + (System.currentTimeMillis() - start) / 1000);
      logger.error("Command execution failed with error", ex);
      RuntimeException rethrow = null;
      if (ex instanceof JSchException) {
        saveExecutionLogError("Command execution failed with error " + normalizeError((JSchException) ex));
      } else if (ex instanceof IOException) {
        logger.error("Exception in reading InputStream", ex);
      } else if (ex instanceof RuntimeException) {
        rethrow = (RuntimeException) ex;
      }
      int i = 0;
      Throwable t = ex;
      while (t != null && i++ < Misc.MAX_CAUSES) {
        String msg = getMessage(t);
        if (isNotBlank(msg)) {
          saveExecutionLogError(msg);
        }
        t = t instanceof JSchException ? null : t.getCause();
      }
      if (rethrow != null) {
        throw rethrow;
      }
      return commandExecutionStatus;
    } finally {
      if (channel != null && !channel.isClosed()) {
        logger.info("Disconnect channel if still open post execution command");
        channel.disconnect();
      }
    }
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
                    DelegateFile delegateFile =
                        delegateFileManager.getMetaInfo(fileBucket, fileNamesId.getKey(), config.getAccountId());
                    return ImmutablePair.of(
                        isBlank(fileNamesId.getRight()) ? delegateFile.getFileName() : fileNamesId.getRight(),
                        delegateFile.getLength());
                  }

                  @Override
                  public void downloadToStream(OutputStream outputStream) throws IOException, ExecutionException {
                    try (InputStream inputStream = delegateFileManager.downloadArtifactByFileId(
                             fileBucket, fileNamesId.getKey(), config.getAccountId(), false)) {
                      IOUtils.copy(inputStream, outputStream);
                    }
                  }
                }))
        .filter(commandExecutionStatus -> commandExecutionStatus == CommandExecutionStatus.FAILURE)
        .findFirst()
        .orElse(CommandExecutionStatus.SUCCESS);
  }

  @Override
  public CommandExecutionStatus copyGridFsFiles(ConfigFileMetaData configFileMetaData) {
    if (isBlank(configFileMetaData.getFileId()) || isBlank(configFileMetaData.getFilename())) {
      saveExecutionLog("There are no artifacts to copy. " + configFileMetaData.toString());
      return CommandExecutionStatus.SUCCESS;
    }
    return scpOneFile(configFileMetaData.getDestinationDirectoryPath(), new FileProvider() {
      @Override
      public Pair<String, Long> getInfo() throws IOException {
        return ImmutablePair.of(configFileMetaData.getFilename(), configFileMetaData.getLength());
      }

      @Override
      public void downloadToStream(OutputStream outputStream) throws IOException, ExecutionException {
        try (InputStream inputStream = delegateFileManager.downloadArtifactByFileId(configFileMetaData.getFileBucket(),
                 configFileMetaData.getFileId(), config.getAccountId(), configFileMetaData.isEncrypted())) {
          IOUtils.copy(inputStream, outputStream);
        }
      }
    });
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
        try (InputStream inputStream = delegateFileManager.downloadByConfigFileId(configFileMetaData.getFileId(),
                 config.getAccountId(), config.getAppId(), configFileMetaData.getActivityId())) {
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
        if (!metadata.containsKey(ARTIFACT_FILE_SIZE)) {
          Long artifactFileSize = delegateFileManager.getArtifactFileSize(artifactStreamAttributes);
          metadata.put(ARTIFACT_FILE_SIZE, String.valueOf(artifactFileSize));
        }
        String fileName = metadata.get(ARTIFACT_FILE_NAME);
        int lastIndexOfSlash = fileName.lastIndexOf('/');
        if (lastIndexOfSlash > 0) {
          saveExecutionLogWarn("Filename contains slashes. Stripping off the portion before last slash.");
          logger.warn("Filename contains slashes. Stripping off the portion before last slash.");
          fileName = fileName.substring(lastIndexOfSlash + 1);
          saveExecutionLogWarn("Got filename: " + fileName);
          logger.warn("Got filename: " + fileName);
        }

        return ImmutablePair.of(fileName, Long.parseLong(metadata.get(ARTIFACT_FILE_SIZE)));
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

  @SuppressFBWarnings("DMI_INVOKING_TOSTRING_ON_ARRAY") // TODO
  private void passwordPromptResponder(String line, OutputStream outputStream) throws IOException {
    if (matchesPasswordPromptPattern(line)) {
      outputStream.write((config.getSudoAppPassword() + "\n").getBytes(UTF_8));
      outputStream.flush();
    }
  }

  private boolean matchesPasswordPromptPattern(String line) {
    return sudoPasswordPromptPattern.matcher(line).find();
  }

  private String processStreamData(String text, boolean finishedReading, OutputStream outputStream) throws IOException {
    if (text == null || text.length() == 0) {
      return text;
    }

    String[] lines = lineBreakPattern.split(text);
    if (lines.length == 0) {
      return "";
    }

    for (int i = 0; i < lines.length - 1; i++) { // Ignore last line.
      saveExecutionLog(lines[i]);
    }

    String lastLine = lines[lines.length - 1];
    // last line is processed only if it ends with new line char or stream closed
    if (textEndsAtNewLineChar(text, lastLine) || finishedReading) {
      passwordPromptResponder(lastLine, outputStream);
      saveExecutionLog(lastLine);
      return ""; // nothing left to carry over
    }
    return lastLine;
  }

  private void saveExecutionLog(String line) {
    saveExecutionLog(line, RUNNING);
  }

  private void saveExecutionLog(String line, CommandExecutionStatus commandExecutionStatus) {
    logService.save(config.getAccountId(),
        aLog()
            .withAppId(config.getAppId())
            .withActivityId(config.getExecutionId())
            .withLogLevel(INFO)
            .withCommandUnitName(config.getCommandUnitName())
            .withHostName(config.getHost())
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }

  private void saveExecutionLogError(String line) {
    logService.save(config.getAccountId(),
        aLog()
            .withAppId(config.getAppId())
            .withActivityId(config.getExecutionId())
            .withLogLevel(ERROR)
            .withCommandUnitName(config.getCommandUnitName())
            .withHostName(config.getHost())
            .withLogLine(line)
            .withExecutionResult(RUNNING)
            .build());
  }

  private void saveExecutionLogWarn(String line) {
    logService.save(config.getAccountId(),
        aLog()
            .withAppId(config.getAppId())
            .withActivityId(config.getExecutionId())
            .withLogLevel(WARN)
            .withCommandUnitName(config.getCommandUnitName())
            .withHostName(config.getHost())
            .withLogLine(line)
            .withExecutionResult(RUNNING)
            .build());
  }

  private boolean textEndsAtNewLineChar(String text, String lastLine) {
    return lastLine.charAt(lastLine.length() - 1) != text.charAt(text.length() - 1);
  }

  /**
   * Gets the session.
   *
   * @param config the config
   * @return the session
   * @throws JSchException the jsch exception
   */
  public abstract Session getSession(SshSessionConfig config);

  /**
   * Gets cached session.
   *
   * @param config the config
   * @return the cached session
   */
  public synchronized Session getCachedSession(SshSessionConfig config) {
    String key = config.getExecutionId() + "~" + config.getHost().trim();
    logger.info("Fetch session for executionId : {}, hostName: {} ", config.getExecutionId(), config.getHost());

    Session cachedSession = sessions.computeIfAbsent(key, s -> {
      logger.info("No session found. Create new session for executionId : {}, hostName: {}", config.getExecutionId(),
          config.getHost());
      return getSession(this.config);
    });

    // Unnecessary but required test before session reuse.
    // test channel. http://stackoverflow.com/questions/16127200/jsch-how-to-keep-the-session-alive-and-up
    try {
      ChannelExec testChannel = (ChannelExec) cachedSession.openChannel("exec");
      testChannel.setCommand("true");
      testChannel.connect();
      testChannel.disconnect();
      logger.info("Session connection test successful");
    } catch (Exception exception) {
      logger.error("Session connection test failed. Reopen new session", exception);
      cachedSession = sessions.merge(key, cachedSession, (session1, session2) -> getSession(this.config));
    }
    return cachedSession;
  }

  private CommandExecutionStatus scpOneFile(String remoteFilePath, FileProvider fileProvider) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    Channel channel = null;
    try {
      Pair<String, Long> fileInfo = fileProvider.getInfo();
      //      String command = "scp -r -d -t '" + remoteFilePath + "'";
      String command = format("mkdir -p \"%s\" && scp -r -d -t '%s'", remoteFilePath, remoteFilePath);
      channel = getCachedSession(config).openChannel("exec");
      ((ChannelExec) channel).setCommand(command);

      // get I/O streams for remote scp
      try (OutputStream out = channel.getOutputStream(); InputStream in = channel.getInputStream()) {
        saveExecutionLog(format("Connecting to %s ....", config.getHost()));
        channel.connect();

        if (checkAck(in) != 0) {
          saveExecutionLogError("SCP connection initiation failed");
          return FAILURE;
        } else {
          saveExecutionLog(format("Connection to %s established", config.getHost()));
        }

        // send "C0644 filesize filename", where filename should not include '/'
        command = "C0644 " + fileInfo.getValue() + " " + fileInfo.getKey() + "\n";

        out.write(command.getBytes(UTF_8));
        out.flush();
        if (checkAck(in) != 0) {
          saveExecutionLogError("SCP connection initiation failed");
          return commandExecutionStatus;
        }
        saveExecutionLog("Begin file transfer " + fileInfo.getKey() + " to " + config.getHost() + ":" + remoteFilePath);
        fileProvider.downloadToStream(out);
        out.write(new byte[1], 0, 1);
        out.flush();

        if (checkAck(in) != 0) {
          saveExecutionLogError("File transfer failed");
          return commandExecutionStatus;
        }
        commandExecutionStatus = SUCCESS;
        saveExecutionLog("File successfully transferred");
        channel.disconnect();
      }
    } catch (IOException | ExecutionException | JSchException ex) {
      if (ex instanceof FileNotFoundException) {
        saveExecutionLogError("File not found");
      } else if (ex instanceof JSchException) {
        logger.error("Command execution failed with error", ex);
        saveExecutionLogError("Command execution failed with error " + normalizeError((JSchException) ex));
      } else {
        throw new WingsException(ERROR_IN_GETTING_CHANNEL_STREAMS, ex);
      }
      return commandExecutionStatus;
    } finally {
      if (channel != null && !channel.isClosed()) {
        logger.info("Disconnect channel if still open post execution command");
        channel.disconnect();
      }
    }
    return commandExecutionStatus;
  }

  /**
   * Check ack.
   *
   * @param in the in
   * @return the int
   * @throws IOException Signals that an I/O exception has occurred.
   */
  int checkAck(InputStream in) throws IOException {
    int b = in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if (b == 0) {
      return b;
    } else if (b == -1) {
      return b;
    } else { // error or echoed string on session initiation from remote host
      StringBuilder sb = new StringBuilder();
      if (b > 2) {
        sb.append((char) b);
      }

      int c;
      do {
        c = in.read();
        sb.append((char) c);
      } while (c != '\n');

      if (b <= 2) {
        saveExecutionLogError(sb.toString());
        return 1;
      }
      logger.error(sb.toString());
      return 0;
    }
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

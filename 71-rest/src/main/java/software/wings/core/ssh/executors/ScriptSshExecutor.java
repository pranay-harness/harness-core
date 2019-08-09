package software.wings.core.ssh.executors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.eraro.ErrorCode.ERROR_IN_GETTING_CHANNEL_STREAMS;
import static io.harness.eraro.ErrorCode.INVALID_EXECUTION_ID;
import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;
import static io.harness.eraro.ErrorCode.UNKNOWN_EXECUTOR_TYPE_ERROR;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SU_APP_USER;
import static software.wings.core.ssh.executors.ScriptExecutor.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.ScriptExecutor.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.ScriptExecutor.ExecutorType.PASSWORD_AUTH;
import static software.wings.utils.SshHelperUtils.normalizeError;

import com.google.common.base.Charsets;
import com.google.inject.Inject;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionResultBuilder;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.stream.BoundedInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.command.ShellExecutionData.ShellExecutionDataBuilder;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.utils.Misc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 2/10/16.
 */
@ValidateOnExecution
@Slf4j
public class ScriptSshExecutor extends AbstractScriptExecutor {
  public static final int CHUNK_SIZE = 10 * 1024; // 10KB
  public static final int ALLOWED_BYTES = 1024 * 1024; // 1MB
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
  private static final int MAX_BYTES_READ_PER_CHANNEL =
      1024 * 1024 * 1024; // TODO: Read from config. 1 GB per channel for now.
  private static ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();

  private Pattern sudoPasswordPromptPattern = Pattern.compile(DEFAULT_SUDO_PROMPT_PATTERN);
  private Pattern lineBreakPattern = Pattern.compile(LINE_BREAK_PATTERN);

  protected SshSessionConfig config;

  /**
   * Instantiates a new abstract ssh executor.
   *
   * @param delegateFileManager the file service
   * @param logService          the log service
   */
  @Inject
  public ScriptSshExecutor(
      DelegateFileManager delegateFileManager, DelegateLogService logService, ScriptExecutionContext config) {
    super(delegateFileManager, logService);
    if (isEmpty(((SshSessionConfig) config).getExecutionId())) {
      throw new WingsException(INVALID_EXECUTION_ID);
    }
    this.config = (SshSessionConfig) config;
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
        channel.connect(config.getSocketConnectTimeout());
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
    } catch (RuntimeException | JSchException | IOException ex) {
      handleException(ex);
      logger.error("ex-Session fetched in " + (System.currentTimeMillis() - start) / 1000);
      logger.error("Command execution failed with error", ex);
      return commandExecutionStatus;
    } finally {
      if (channel != null && !channel.isClosed()) {
        logger.info("Disconnect channel if still open post execution command");
        channel.disconnect();
      }
    }
  }

  public CommandExecutionResult executeCommandString(String command, List<String> envVariablesToCollect) {
    ShellExecutionDataBuilder executionDataBuilder = ShellExecutionData.builder();
    CommandExecutionResultBuilder commandExecutionResult = CommandExecutionResult.builder();
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    Channel channel = null;
    long start = System.currentTimeMillis();
    Map<String, String> envVariablesMap = new HashMap<>();
    try {
      saveExecutionLog(format("Initializing SSH connection to %s ....", config.getHost()));
      channel = getCachedSession(this.config).openChannel("exec");
      logger.info("Session fetched in " + (System.currentTimeMillis() - start) + " ms");

      ((ChannelExec) channel).setPty(true);

      String directoryPath = this.config.getWorkingDirectory() + "/";
      String envVariablesFilename = null;
      command = "cd " + directoryPath + "\n" + command;
      if (!envVariablesToCollect.isEmpty()) {
        envVariablesFilename = "harness-" + this.config.getExecutionId() + ".out";
        command = addEnvVariablesCollector(command, envVariablesToCollect, directoryPath + envVariablesFilename);
      }

      try (OutputStream outputStream = channel.getOutputStream(); InputStream inputStream = channel.getInputStream()) {
        ((ChannelExec) channel).setCommand(command);
        saveExecutionLog(format("Connecting to %s ....", config.getHost()));
        channel.connect(config.getSocketConnectTimeout());
        saveExecutionLog(format("Connection to %s established", config.getHost()));
        saveExecutionLog("Executing command...");

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
            text += dataReadFromTheStream;
            text = processStreamData(text, false, outputStream);
          }

          if (text.length() > 0) {
            text = processStreamData(text, true, outputStream); // finished reading. update logs
          }

          if (channel.isClosed()) {
            commandExecutionStatus = channel.getExitStatus() == 0 ? SUCCESS : FAILURE;
            saveExecutionLog("Command finished with status " + commandExecutionStatus, commandExecutionStatus);

            if (commandExecutionStatus == SUCCESS && envVariablesFilename != null) {
              BufferedReader br = null;
              try {
                channel = getCachedSession(this.config).openChannel("sftp");
                channel.connect(config.getSocketConnectTimeout());
                ((ChannelSftp) channel).cd(directoryPath);
                BoundedInputStream stream =
                    new BoundedInputStream(((ChannelSftp) channel).get(envVariablesFilename), CHUNK_SIZE);
                br = new BufferedReader(new InputStreamReader(stream, Charsets.UTF_8));
                processScriptOutputFile(envVariablesMap, br);
              } catch (JSchException | SftpException | IOException e) {
                logger.error("Exception occurred during reading file from SFTP server due to " + e.getMessage(), e);
              } finally {
                if (br != null) {
                  br.close();
                }
                try {
                  ((ChannelSftp) channel).rm(directoryPath + envVariablesFilename);
                } catch (SftpException e) {
                  logger.error("Failed to delete file " + envVariablesFilename, e);
                }
              }
            }
            executionDataBuilder.sweepingOutputEnvVariables(envVariablesMap);
            commandExecutionResult.status(commandExecutionStatus);
            commandExecutionResult.commandExecutionData(executionDataBuilder.build());
            return commandExecutionResult.build();
          }
          sleep(Duration.ofSeconds(1));
        }
      }
    } catch (RuntimeException | JSchException | IOException ex) {
      handleException(ex);
      logger.error("ex-Session fetched in " + (System.currentTimeMillis() - start) / 1000);
      logger.error("Command execution failed with error", ex);
      executionDataBuilder.sweepingOutputEnvVariables(envVariablesMap);
      commandExecutionResult.status(commandExecutionStatus);
      commandExecutionResult.commandExecutionData(executionDataBuilder.build());
      return commandExecutionResult.build();
    } finally {
      if (channel != null && !channel.isClosed()) {
        logger.info("Disconnect channel if still open post execution command");
        channel.disconnect();
      }
    }
  }

  @Override
  public String getAccountId() {
    return config.getAccountId();
  }

  @Override
  public String getCommandUnitName() {
    return config.getCommandUnitName();
  }

  @Override
  public String getAppId() {
    return config.getAppId();
  }

  @Override
  public String getExecutionId() {
    return config.getExecutionId();
  }

  @Override
  public String getHost() {
    return config.getHost();
  }

  private void handleException(Exception ex) {
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
      String msg = ExceptionUtils.getMessage(t);
      if (isNotBlank(msg)) {
        saveExecutionLogError(msg);
      }
      t = t instanceof JSchException ? null : t.getCause();
    }
    if (rethrow != null) {
      throw rethrow;
    }
  }

  private void passwordPromptResponder(String line, OutputStream outputStream) throws IOException {
    if (matchesPasswordPromptPattern(line)) {
      outputStream.write((new String(config.getSudoAppPassword()) + "\n").getBytes(UTF_8));
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

  private boolean textEndsAtNewLineChar(String text, String lastLine) {
    return lastLine.charAt(lastLine.length() - 1) != text.charAt(text.length() - 1);
  }

  private Session getSession(SshSessionConfig config) {
    return getSession(config,
        new ExecutionLogCallback(logService, config.getAccountId(), config.getAppId(), config.getExecutionId(),
            config.getCommandUnitName()));
  }

  private Session getSession(SshSessionConfig config, ExecutionLogCallback executionLogCallback) {
    if (config.getExecutorType() == null) {
      if (config.getBastionHostConfig() != null) {
        config.setExecutorType(BASTION_HOST);
      } else {
        if (config.getAccessType().equals(HostConnectionAttributes.AccessType.KEY)
            || config.getAccessType().equals(KEY_SU_APP_USER) || config.getAccessType().equals(KEY_SUDO_APP_USER)) {
          config.setExecutorType(KEY_AUTH);
        } else {
          config.setExecutorType(PASSWORD_AUTH);
        }
      }
    }

    try {
      switch (config.getExecutorType()) {
        case PASSWORD_AUTH:
        case KEY_AUTH:
          return SshSessionFactory.getSSHSession(config, executionLogCallback);
        case BASTION_HOST:
          return SshSessionFactory.getSSHSessionWithJumpbox(config, executionLogCallback);
        default:
          throw new WingsException(
              UNKNOWN_EXECUTOR_TYPE_ERROR, new Throwable("Unknown executor type: " + config.getExecutorType()));
      }
    } catch (JSchException jschEx) {
      throw new WingsException(normalizeError(jschEx), normalizeError(jschEx).name(), jschEx);
    }
  }

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
      testChannel.connect(this.config.getSocketConnectTimeout());
      testChannel.disconnect();
      logger.info("Session connection test successful");
    } catch (Exception exception) {
      logger.error("Session connection test failed. Reopen new session", exception);
      cachedSession = sessions.merge(key, cachedSession, (session1, session2) -> getSession(this.config));
    }
    return cachedSession;
  }

  @Override
  public CommandExecutionStatus scpOneFile(String remoteFilePath, AbstractScriptExecutor.FileProvider fileProvider) {
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
        channel.connect(config.getSocketConnectTimeout());

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
      int totalBytesRead = 0;
      do {
        c = in.read();
        if (c == -1) {
          break;
        }
        totalBytesRead++;
        sb.append((char) c);
      } while (c != '\n' || totalBytesRead <= ALLOWED_BYTES);

      if (b <= 2) {
        saveExecutionLogError(sb.toString());
        return 1;
      }
      logger.error(sb.toString());
      return 0;
    }
  }
}

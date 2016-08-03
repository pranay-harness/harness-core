package software.wings.core.ssh.executors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static software.wings.beans.ErrorCodes.ERROR_IN_GETTING_CHANNEL_STREAMS;
import static software.wings.beans.ErrorCodes.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCodes.INVALID_EXECUTION_ID;
import static software.wings.beans.ErrorCodes.INVALID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_KEYPATH;
import static software.wings.beans.ErrorCodes.INVALID_PORT;
import static software.wings.beans.ErrorCodes.SOCKET_CONNECTION_ERROR;
import static software.wings.beans.ErrorCodes.SOCKET_CONNECTION_TIMEOUT;
import static software.wings.beans.ErrorCodes.SSH_SESSION_TIMEOUT;
import static software.wings.beans.ErrorCodes.UNKNOWN_ERROR;
import static software.wings.beans.ErrorCodes.UNKNOWN_HOST;
import static software.wings.beans.ErrorCodes.UNREACHABLE_HOST;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.command.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.utils.Misc.quietSleep;

import com.google.common.base.Strings;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCodes;
import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.LogService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import javax.inject.Inject;
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
  protected LogService logService;
  /**
   * The File service.
   */
  protected FileService fileService;
  private Pattern sudoPasswordPromptPattern = Pattern.compile(DEFAULT_SUDO_PROMPT_PATTERN);
  private Pattern lineBreakPattern = Pattern.compile(LINE_BREAK_PATTERN);

  /**
   * Instantiates a new abstract ssh executor.
   *
   * @param fileService the file service
   * @param logService  the log service
   */
  @Inject
  public AbstractSshExecutor(FileService fileService, LogService logService) {
    this.logService = logService;
    this.fileService = fileService;
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
    if (Strings.isNullOrEmpty(config.getExecutionId())) {
      throw new WingsException(INVALID_EXECUTION_ID);
    }
    this.config = config;
  }

  @Override
  public ExecutionResult executeCommandString(String command) {
    return executeCommandString(command, null);
  }

  @Override
  public ExecutionResult executeCommandString(String command, StringBuffer output) {
    ExecutionResult executionResult = FAILURE;
    Channel channel = null;
    try {
      channel = getCachedSession(this.config).openChannel("exec");
      ((ChannelExec) channel).setPty(true);
      OutputStream outputStream = channel.getOutputStream();
      InputStream inputStream = channel.getInputStream();
      ((ChannelExec) channel).setCommand(command);
      saveExecutionLog(format("Connecting to %s ....", config.getHost()));
      channel.connect();
      saveExecutionLog(format("Connection to %s established", config.getHost()));
      saveExecutionLog(format("Executing command %s ...", command));

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
            throw new WingsException(UNKNOWN_ERROR); // TODO: better error reporting
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
          executionResult = channel.getExitStatus() == 0 ? SUCCESS : FAILURE;
          saveExecutionLog("Command finished with status " + executionResult);
          return executionResult;
        }
        quietSleep(1000);
      }
    } catch (JSchException | IOException ex) {
      if (ex instanceof JSchException) {
        logger.error("Command execution failed with error " + ex.getMessage());
        saveExecutionLog("Command execution failed with error " + normalizeError((JSchException) ex));
      } else {
        logger.error("Exception in reading InputStream " + ex.getMessage());
        saveExecutionLog("Command execution failed with error " + UNKNOWN_ERROR);
      }
      return executionResult;
    } finally {
      if (channel != null && !channel.isClosed()) {
        logger.info("Disconnect channel if still open post execution command");
        channel.disconnect();
      }
    }
  }

  @Override
  public ExecutionResult copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<String> gridFsFileIds) {
    return gridFsFileIds.stream()
        .map(fileId
            -> scpOneFile(destinationDirectoryPath,
                new FileProvider() {
                  @Override
                  public Pair<String, Long> getInfo() throws IOException {
                    GridFSFile gridFSFile = fileService.getGridFsFile(fileId, fileBucket);
                    return ImmutablePair.of(gridFSFile.getFilename(), gridFSFile.getLength());
                  }

                  @Override
                  public void downloadToStream(OutputStream outputStream) throws IOException {
                    fileService.downloadToStream(fileId, outputStream, fileBucket);
                  }
                }))
        .filter(executionResult -> executionResult == ExecutionResult.FAILURE)
        .findFirst()
        .orElse(ExecutionResult.SUCCESS);
  }

  @Override
  public ExecutionResult copyFiles(String destinationDirectoryPath, List<String> files) {
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
        .filter(executionResult -> executionResult == ExecutionResult.FAILURE)
        .findFirst()
        .orElse(ExecutionResult.SUCCESS);
  }

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
    logger.info(line);
    logService.save(aLog()
                        .withAppId(config.getAppId())
                        .withActivityId(config.getExecutionId())
                        .withHostName(config.getHost())
                        .withLogLevel(INFO)
                        .withCommandUnitName(config.getCommandUnitName())
                        .withLogLine(line)
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

    Session cahcedSession = sessions.computeIfAbsent(key, s -> {
      logger.info("No session found. Create new session for executionId : {}, hostName: {}", config.getExecutionId(),
          config.getHost());
      return getSession(this.config);
    });

    // Unnecessary but required test before session reuse.
    // test channel. http://stackoverflow.com/questions/16127200/jsch-how-to-keep-the-session-alive-and-up
    try {
      ChannelExec testChannel = (ChannelExec) cahcedSession.openChannel("exec");
      testChannel.setCommand("true");
      testChannel.connect();
      testChannel.disconnect();
      logger.info("Session connection test successful");
    } catch (Throwable throwable) {
      logger.error("Session connection test failed. Reopen new session");
      cahcedSession = sessions.merge(key, cahcedSession, (session1, session2) -> getSession(this.config));
    }
    return cahcedSession;
  }

  /**
   * Normalize error.
   *
   * @param jschexception the jschexception
   * @return the string
   */
  protected ErrorCodes normalizeError(JSchException jschexception) {
    String message = jschexception.getMessage();
    Throwable cause = jschexception.getCause();

    ErrorCodes errorConst = UNKNOWN_ERROR;

    if (cause != null) { // TODO: Refactor use enums, maybe ?
      if (cause instanceof NoRouteToHostException) {
        errorConst = UNREACHABLE_HOST;
      } else if (cause instanceof UnknownHostException) {
        errorConst = UNKNOWN_HOST;
      } else if (cause instanceof SocketTimeoutException) {
        errorConst = SOCKET_CONNECTION_TIMEOUT;
      } else if (cause instanceof ConnectException) {
        errorConst = INVALID_PORT;
      } else if (cause instanceof SocketException) {
        errorConst = SOCKET_CONNECTION_ERROR;
      } else if (cause instanceof FileNotFoundException) {
        errorConst = INVALID_KEYPATH;
      }
    } else {
      if (message.startsWith("invalid privatekey")) {
        errorConst = INVALID_KEY;
      } else if (message.contains("Auth fail") || message.contains("Auth cancel")
          || message.contains("USERAUTH fail")) {
        errorConst = INVALID_CREDENTIAL;
      } else if (message.startsWith("timeout: socket is not established")
          || message.contains("SocketTimeoutException")) {
        errorConst = SOCKET_CONNECTION_TIMEOUT;
      } else if (message.equals("session is down")) {
        errorConst = SSH_SESSION_TIMEOUT;
      }
    }
    return errorConst;
  }

  private ExecutionResult scpOneFile(String remoteFilePath, FileProvider fileProvider) {
    ExecutionResult executionResult = FAILURE;
    Channel channel = null;
    try {
      String command = "scp -r -d -t " + remoteFilePath;
      channel = getCachedSession(config).openChannel("exec");
      ((ChannelExec) channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out = channel.getOutputStream();
      InputStream in = channel.getInputStream();

      saveExecutionLog(format("Connecting to %s ....", config.getHost()));
      channel.connect();

      if (checkAck(in) != 0) {
        logger.error("SCP connection initiation failed");
        saveExecutionLog("SCP connection initiation failed");
        return FAILURE;
      } else {
        saveExecutionLog(format("Connection to %s established", config.getHost()));
      }

      Pair<String, Long> fileInfo = fileProvider.getInfo();

      // send "C0644 filesize filename", where filename should not include '/'
      command = "C0644 " + fileInfo.getValue() + " " + fileInfo.getKey() + "\n";

      out.write(command.getBytes(UTF_8));
      out.flush();
      if (checkAck(in) != 0) {
        saveExecutionLog("SCP connection initiation failed");
        return executionResult;
      }
      saveExecutionLog("Begin file transfer " + fileInfo.getKey() + " to " + config.getHost() + ":" + remoteFilePath);
      fileProvider.downloadToStream(out);
      out.write(new byte[1], 0, 1);
      out.flush();

      if (checkAck(in) != 0) {
        saveExecutionLog("File transfer failed");
        logger.error("File transfer failed");
        return executionResult;
      }
      executionResult = SUCCESS;
      saveExecutionLog("File successfully transferred");
      out.close();
      channel.disconnect();
    } catch (IOException | JSchException ex) {
      if (ex instanceof FileNotFoundException) {
        logger.error("file could not be found");
        saveExecutionLog("File not found");
      } else if (ex instanceof JSchException) {
        logger.error("Command execution failed with error ", ex);
        saveExecutionLog("Command execution failed with error " + normalizeError((JSchException) ex));
      } else {
        throw new WingsException(ERROR_IN_GETTING_CHANNEL_STREAMS);
      }
      return executionResult;
    } finally {
      if (channel != null && !channel.isClosed()) {
        logger.info("Disconnect channel if still open post execution command");
        channel.disconnect();
      }
    }
    return executionResult;
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
        throw new WingsException(UNKNOWN_ERROR, new Throwable(sb.toString()));
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
    void downloadToStream(OutputStream outputStream) throws IOException;
  }
}

package software.wings.core.ssh.executors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static software.wings.beans.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.beans.ErrorConstants.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorConstants.INVALID_KEY;
import static software.wings.beans.ErrorConstants.INVALID_KEYPATH;
import static software.wings.beans.ErrorConstants.INVALID_PORT;
import static software.wings.beans.ErrorConstants.SOCKET_CONNECTION_ERROR;
import static software.wings.beans.ErrorConstants.SOCKET_CONNECTION_TIMEOUT;
import static software.wings.beans.ErrorConstants.SSH_SESSION_TIMEOUT;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR;
import static software.wings.beans.ErrorConstants.UNKNOWN_HOST;
import static software.wings.beans.ErrorConstants.UNREACHABLE_HOST;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.utils.Misc.quietSleep;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.LogService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 2/10/16.
 */
@ValidateOnExecution
public abstract class AbstractSshExecutor implements SshExecutor {
  public static final String DEFAULT_SUDO_PROMPT_PATTERN = "^\\[sudo\\] password for .+: .*";
  public static final String LINE_BREAK_PATTERN = "\\R+";
  private static final int MAX_BYTES_READ_PER_CHANNEL =
      1024 * 1024 * 1024; // TODO: Read from config. 1 GB per channel for now.
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  protected Session session;
  protected Channel channel;
  protected SshSessionConfig config;
  protected OutputStream outputStream;
  protected InputStream inputStream;
  protected LogService logService;
  protected FileService fileService;
  private Pattern sudoPasswordPromptPattern = Pattern.compile(DEFAULT_SUDO_PROMPT_PATTERN);
  private Pattern lineBreakPattern = Pattern.compile(LINE_BREAK_PATTERN);

  /**
   * Instantiates a new abstract ssh executor.
   *
   * @param logService  the log service
   * @param fileService the file service
   */
  @Inject
  public AbstractSshExecutor(FileService fileService, LogService logService) {
    this.logService = logService;
    this.fileService = fileService;
  }

  /* (non-Javadoc)
   * @see software.wings.core.ssh.executors.SshExecutor#init(software.wings.core.ssh.executors.SshSessionConfig)
   */
  @Override
  public void init(@Valid SshSessionConfig config) {
    if (null == config.getExecutionId() || config.getExecutionId().length() == 0) {
      throw new WingsException(UNKNOWN_ERROR, new Throwable("INVALID_EXECUTION_ID"));
    }

    this.config = config;
    try {
      session = getSession(config);
      channel = session.openChannel("exec");
      ((ChannelExec) channel).setPty(true);
      ((ChannelExec) channel).setErrStream(System.err, true);
      outputStream = channel.getOutputStream();
      inputStream = channel.getInputStream();
    } catch (JSchException ex) {
      logger.error("Failed to initialize executor " + ex);
      throw new WingsException(normalizeError(ex), ex.getCause());
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  /* (non-Javadoc)
   * @see software.wings.core.ssh.executors.SshExecutor#execute(java.lang.String)
   */
  @Override
  public ExecutionResult execute(String command) {
    return genericExecute(command);
  }

  private ExecutionResult genericExecute(String command) {
    ExecutionResult executionResult = FAILURE;
    try {
      ((ChannelExec) channel).setCommand(command);
      saveExecutionLog(format("Connecting to %s ....", config.getHost()));
      channel.connect();
      saveExecutionLog(format("Connection to %s established", config.getHost()));
      saveExecutionLog(format("Execution started for command %s ...", command));

      int totalBytesRead = 0;
      byte[] byteBuffer = new byte[1024]; // FIXME: Improve stream reading/writing logic
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
          text += new String(byteBuffer, 0, numOfBytesRead, UTF_8);
          text = processStreamData(text, false);
        }

        if (text.length() > 0) {
          text = processStreamData(text, true); // finished reading. update logs
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
      destroy();
    }
  }

  private void passwordPromtResponder(String line) throws IOException {
    if (matchesPasswordPromptPattern(line)) {
      outputStream.write((config.getSudoAppPassword() + "\n").getBytes(UTF_8));
      outputStream.flush();
    }
  }

  private boolean matchesPasswordPromptPattern(String line) {
    return sudoPasswordPromptPattern.matcher(line).find();
  }

  private String processStreamData(String text, boolean finishedReading) throws IOException {
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
      passwordPromtResponder(lastLine);
      saveExecutionLog(lastLine);
      return ""; // nothing left to carry over
    }
    return lastLine;
  }

  private void saveExecutionLog(String line) {
    logService.save(aLog()
                        .withAppId(config.getAppId())
                        .withActivityId(config.getExecutionId())
                        .withLogLevel(INFO)
                        .withLogLine(line)
                        .build());
  }

  private boolean textEndsAtNewLineChar(String text, String lastLine) {
    return lastLine.charAt(lastLine.length() - 1) != text.charAt(text.length() - 1);
  }

  /* (non-Javadoc)
   * @see software.wings.core.ssh.executors.SshExecutor#destroy()
   */
  @Override
  public void destroy() {
    logger.info("Disconnecting ssh session");
    if (null != channel) {
      channel.disconnect();
    }
    if (null != session) {
      session.disconnect();
    }
  }

  /* (non-Javadoc)
   * @see software.wings.core.ssh.executors.SshExecutor#abort()
   */
  @Override
  public void abort() {
    try {
      outputStream.write(3); // Send ^C command
      outputStream.flush();
    } catch (IOException ex) {
      logger.error("Abort command failed " + ex);
    }
  }

  /**
   * Gets the session.
   *
   * @param config the config
   * @return the session
   * @throws JSchException the j sch exception
   */
  public abstract Session getSession(SshSessionConfig config) throws JSchException;

  /**
   * Normalize error.
   *
   * @param jschexception the jschexception
   * @return the string
   */
  protected String normalizeError(JSchException jschexception) {
    String message = jschexception.getMessage();
    Throwable cause = jschexception.getCause();

    String errorConst = null;

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
      } else {
        errorConst = UNKNOWN_ERROR;
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

  /**
   * **
   * SCP.
   * **
   *
   * @param gridFsFileId   the grid fs file id
   * @param remoteFilePath the remote file path
   * @param gridFsBucket   the grid fs bucket
   * @return the execution result
   */
  @Override
  public ExecutionResult transferFile(String gridFsFileId, String remoteFilePath, FileBucket gridFsBucket) {
    ExecutionResult executionResult = FAILURE;
    try {
      String command = "scp -t " + remoteFilePath;
      Channel channel = session.openChannel("exec");
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
      GridFSFile fileMetaData = fileService.getGridFsFile(gridFsFileId, gridFsBucket);

      // send "C0644 filesize filename", where filename should not include '/'
      long filesize = fileMetaData.getLength();
      String fileName = fileMetaData.getFilename();
      if (fileName.lastIndexOf('/') > 0) {
        fileName += fileName.substring(fileName.lastIndexOf('/') + 1);
      }
      command = "C0644 " + filesize + " " + fileName + "\n";

      out.write(command.getBytes(UTF_8));
      out.flush();
      if (checkAck(in) != 0) {
        saveExecutionLog("SCP connection initiation failed");
        return executionResult;
      }
      saveExecutionLog("Begin file transfer");
      fileService.downloadToStream(gridFsFileId, out, gridFsBucket);
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
      session.disconnect();
    } catch (IOException | JSchException ex) {
      if (ex instanceof FileNotFoundException) {
        logger.error("file [" + gridFsFileId + "] could not be found");
        saveExecutionLog("File not found");
      } else {
        logger.error("Command execution failed with error ", ex);
        saveExecutionLog("Command execution failed with error " + normalizeError((JSchException) ex));
      }
      return executionResult;
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
}

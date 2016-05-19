package software.wings.core.ssh.executors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.wings.beans.ErrorConstants.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorConstants.INVALID_KEY;
import static software.wings.beans.ErrorConstants.INVALID_KEYPATH;
import static software.wings.beans.ErrorConstants.INVALID_PORT;
import static software.wings.beans.ErrorConstants.SOCKET_CONNECTION_TIMEOUT;
import static software.wings.beans.ErrorConstants.SOCKET_CONNECTION_ERROR;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR;
import static software.wings.beans.ErrorConstants.UNKNOWN_HOST;
import static software.wings.beans.ErrorConstants.UNREACHABLE_HOST;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.FAILURE;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.SUCCESS;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;
import static software.wings.utils.Misc.quietSleep;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;

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
import javax.inject.Inject;

/**
 * Created by anubhaw on 2/10/16.
 */
public abstract class AbstractSshExecutor implements SshExecutor {
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  protected Session session;
  protected Channel channel;
  protected SshSessionConfig config;
  protected OutputStream outputStream;
  protected InputStream inputStream;
  private final int MAX_BYTES_READ_PER_CHANNEL =
      1024 * 1024 * 1024; // TODO: Read from config. 1 GB per channel for now.
  protected ExecutionLogs executionLogs;
  protected FileService fileService;
  public static final String DEFAULT_SUDO_PROMPT_PATTERN = "^\\[sudo\\] password for .+: .*";

  @Inject
  public AbstractSshExecutor(ExecutionLogs executionLogs, FileService fileService) {
    this.executionLogs = executionLogs;
    this.fileService = fileService;
  }

  @Override
  public void init(SshSessionConfig config) {
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

  @Override
  public ExecutionResult execute(String command) {
    return genericExecute(command);
  }

  private ExecutionResult genericExecute(String command) {
    try {
      ((ChannelExec) channel).setCommand(command);
      channel.connect();

      int totalBytesRead = 0;
      byte[] tmp = new byte[1024]; // FIXME: Improve stream reading/writing logic
      while (true) {
        while (inputStream.available() > 0) {
          int numOfBytesRead = inputStream.read(tmp, 0, 1024);
          if (numOfBytesRead < 0) {
            break;
          }
          totalBytesRead += numOfBytesRead;
          if (totalBytesRead >= MAX_BYTES_READ_PER_CHANNEL) {
            throw new WingsException(UNKNOWN_ERROR);
          }
          String line = new String(tmp, 0, numOfBytesRead, UTF_8);
          if (line.matches(DEFAULT_SUDO_PROMPT_PATTERN)) {
            outputStream.write((config.getSudoUserPassword() + "\n").getBytes(UTF_8));
            outputStream.flush();
          }
          executionLogs.appendLogs(config.getExecutionId(), line);
        }
        if (channel.isClosed()) {
          return channel.getExitStatus() == 0 ? SUCCESS : FAILURE;
        }
        quietSleep(1000);
      }
    } catch (JSchException ex) {
      logger.error("Command execution failed with error " + ex.getMessage());
      throw new WingsException(normalizeError(ex), ex.getCause());
    } catch (IOException ex) {
      logger.error("Exception in reading InputStream");
      throw new WingsException(UNKNOWN_ERROR, ex.getCause());
    } finally {
      destroy();
    }
  }

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

  @Override
  public void abort() {
    try {
      outputStream.write(3); // Send ^C command
      outputStream.flush();
    } catch (IOException ex) {
      logger.error("Abort command failed " + ex);
    }
  }

  public abstract Session getSession(SshSessionConfig config) throws JSchException;

  public void postChannelConnect() {}

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
      } else if (message.startsWith("timeout: socket is not established")) {
        errorConst = SOCKET_CONNECTION_ERROR;
      }
    }
    return errorConst;
  }

  /****
   * SCP.
   ****/
  @Override
  public ExecutionResult transferFile(String localFilePath, String remoteFilePath) {
    FileInputStream fis = null;
    try {
      String command = "scp -t " + remoteFilePath;
      Channel channel = session.openChannel("exec");
      ((ChannelExec) channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out = channel.getOutputStream();
      InputStream in = channel.getInputStream();
      channel.connect();

      if (checkAck(in) != 0) {
        logger.error("SCP connection initiation failed");
        return FAILURE;
      }
      GridFSFile fileMetaData = fileService.getGridFsFile(localFilePath, ARTIFACTS);

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
        return FAILURE;
      }
      fileService.downloadToStream(localFilePath, out, ARTIFACTS);
      out.write(new byte[1], 0, 1);
      out.flush();

      if (checkAck(in) != 0) {
        logger.error("SCP connection initiation failed");
        return FAILURE;
      }
      out.close();
      channel.disconnect();
      session.disconnect();
    } catch (FileNotFoundException ex) {
      logger.error("file [" + localFilePath + "] could not be found");
      throw new WingsException(UNKNOWN_ERROR, ex.getCause());
    } catch (IOException ex) {
      logger.error("Exception in reading InputStream");
      throw new WingsException(UNKNOWN_ERROR, ex.getCause());
    } catch (JSchException ex) {
      logger.error("Command execution failed with errorCode ", ex);
      throw new WingsException(normalizeError(ex), ex.getCause());
    }
    return SUCCESS;
  }

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

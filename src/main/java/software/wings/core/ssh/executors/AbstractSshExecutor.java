package software.wings.core.ssh.executors;

import static software.wings.beans.ErrorConstants.INVALID_CREDENTIAL_ERROR_CODE;
import static software.wings.beans.ErrorConstants.INVALID_CREDENTIAL_ERROR_MSG;
import static software.wings.beans.ErrorConstants.INVALID_KEYPATH_ERROR_CODE;
import static software.wings.beans.ErrorConstants.INVALID_KEYPATH_ERROR_MSG;
import static software.wings.beans.ErrorConstants.INVALID_KEY_ERROR_CODE;
import static software.wings.beans.ErrorConstants.INVALID_KEY_ERROR_MSG;
import static software.wings.beans.ErrorConstants.SSH_SOCKET_CONNECTION_ERROR_CODE;
import static software.wings.beans.ErrorConstants.SSH_SOCKET_CONNECTION_ERROR_MSG;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_CODE;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_MEG;
import static software.wings.beans.ErrorConstants.UNKNOWN_HOST_ERROR_CODE;
import static software.wings.beans.ErrorConstants.UNKNOWN_HOST_ERROR_MSG;
import static software.wings.core.ssh.ExecutionLogs.getInstance;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.FAILURE;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.SUCCESS;
import static software.wings.utils.Misc.quietSleep;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.mongodb.client.gridfs.model.GridFSFile;

import software.wings.app.WingsBootstrap;
import software.wings.core.ssh.ExecutionLogs;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService;

/**
 * Created by anubhaw on 2/10/16.
 */
public abstract class AbstractSshExecutor implements SshExecutor {
  public static String DEFAULT_SUDO_PROMPT_PATTERN = "^\\[sudo\\] password for .+: .*";
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  protected Session session;
  protected Channel channel;
  protected SshSessionConfig config;
  protected OutputStream outputStream;
  protected InputStream inputStream;
  protected ExecutionLogs executionLogs = getInstance();

  @Override
  public void init(SshSessionConfig config) {
    if (null == config.getExecutionID() || config.getExecutionID().length() == 0) {
      throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, new Throwable("INVALID_EXECUTION_ID"));
    }

    this.config = config;
    try {
      session = getSession(config);
      channel = session.openChannel("exec");
      ((ChannelExec) channel).setPty(true);
      ((ChannelExec) channel).setErrStream(System.err, true);
      outputStream = channel.getOutputStream();
      inputStream = channel.getInputStream();
    } catch (JSchException e) {
      logger.error("Failed to initialize executor");
      SshException shEx = extractSshException(e);
      throw new WingsException(shEx.code, shEx.msg, e.getCause());
    } catch (IOException e) {
      e.printStackTrace();
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

      byte[] tmp = new byte[1024]; // FIXME: Improve stream reading/writing logic
      while (true) {
        while (inputStream.available() > 0) {
          int i = inputStream.read(tmp, 0, 1024);
          if (i < 0)
            break;
          String line = new String(tmp, 0, i);
          if (line.matches(DEFAULT_SUDO_PROMPT_PATTERN)) {
            outputStream.write((config.getSudoUserPassword() + "\n").getBytes());
            outputStream.flush();
          }
          executionLogs.appendLogs(config.getExecutionID(), line);
        }
        if (channel.isClosed()) {
          return channel.getExitStatus() == 0 ? SUCCESS : FAILURE;
        }
        quietSleep(1000);
      }
    } catch (JSchException e) {
      SshException shEx = extractSshException(e);
      logger.error("Command execution failed with error " + e.getMessage());
      throw new WingsException(shEx.code, shEx.msg, e.getCause());
    } catch (IOException e) {
      logger.error("Exception in reading InputStream");
      throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, e.getCause());
    } finally {
      destroy();
    }
  }

  /****
   * SCP
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
      FileService fileService = WingsBootstrap.lookup(FileService.class);
      GridFSFile fileMetaData = fileService.getGridFsFile(localFilePath);

      // send "C0644 filesize filename", where filename should not include '/'
      long filesize = fileMetaData.getLength();
      String fileName = fileMetaData.getFilename();
      if (fileName.lastIndexOf('/') > 0) {
        fileName += fileName.substring(fileName.lastIndexOf('/') + 1);
      }
      command = "C0644 " + filesize + " " + fileName + "\n";

      out.write(command.getBytes());
      out.flush();
      if (checkAck(in) != 0) {
        return FAILURE;
      }
      fileService.downloadToStream(localFilePath, out);
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
      throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, ex.getCause());
    } catch (IOException e) {
      logger.error("Exception in reading InputStream");
      throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, e.getCause());
    } catch (JSchException e) {
      SshException shEx = extractSshException(e);
      logger.error("Command execution failed with error " + e.getMessage());
      throw new WingsException(shEx.getCode(), shEx.getMsg(), e.getCause());
    }
    return SUCCESS;
  }

  @Override
  public void abort() {
    try {
      outputStream.write(3); // Send ^C command
      outputStream.flush();
    } catch (IOException e) {
      logger.error("Abort command failed ", e);
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

  int checkAck(InputStream in) throws IOException {
    int b = in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if (b == 0)
      return b;
    else if (b == -1)
      return b;
    else { // error or echoed string on session initiation from remote host
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
        throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, new Throwable(sb.toString()));
      }
      logger.error(sb.toString());
      return 0;
    }
  }

  ;

  public abstract Session getSession(SshSessionConfig config) throws JSchException;

  protected SshException extractSshException(JSchException jSchException) {
    String message = jSchException.getMessage();
    Throwable cause = jSchException.getCause();

    String customMessage = null;
    String customCode = null;

    if (null != cause) {
      if (cause instanceof NoRouteToHostException || cause instanceof UnknownHostException) {
        customMessage = UNKNOWN_HOST_ERROR_MSG;
        customCode = UNKNOWN_HOST_ERROR_CODE;
      } else if (cause instanceof SocketTimeoutException) {
        customMessage = UNKNOWN_HOST_ERROR_MSG;
        customCode = UNKNOWN_HOST_ERROR_CODE;
      } else if (cause instanceof SocketException) {
        customMessage = SSH_SOCKET_CONNECTION_ERROR_MSG;
        customCode = SSH_SOCKET_CONNECTION_ERROR_CODE;
      } else if (cause instanceof FileNotFoundException) {
        customMessage = INVALID_KEYPATH_ERROR_CODE;
        customCode = INVALID_KEYPATH_ERROR_MSG;
      } else {
        customMessage = UNKNOWN_ERROR_CODE;
        customCode = UNKNOWN_ERROR_MEG;
      }
    } else {
      if (message.startsWith("invalid privatekey")) {
        customMessage = INVALID_KEY_ERROR_MSG;
        customCode = INVALID_KEY_ERROR_CODE;
      } else if (message.contains("Auth fail") || message.contains("Auth cancel")
          || message.contains("USERAUTH fail")) {
        customMessage = INVALID_CREDENTIAL_ERROR_MSG;
        customCode = INVALID_CREDENTIAL_ERROR_CODE;
      } else if (message.startsWith("timeout: socket is not established")) {
        customMessage = SSH_SOCKET_CONNECTION_ERROR_MSG;
        customCode = SSH_SOCKET_CONNECTION_ERROR_CODE;
      }
    }
    return new SshException(customCode, customMessage);
  }

  public void postChannelConnect() {}

  protected class SshException {
    private String code;
    private String msg;

    private SshException(String code, String cause) {
      this.code = code;
      this.msg = cause;
    }

    public String getCode() {
      return code;
    }

    public String getMsg() {
      return msg;
    }
  }
}

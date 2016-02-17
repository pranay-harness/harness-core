package software.wings.core.ssh.executors;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorConstants;
import software.wings.exception.WingsException;

import java.io.*;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static software.wings.beans.ErrorConstants.*;
import static software.wings.utils.Misc.quietSleep;

/**
 * Created by anubhaw on 2/10/16.
 */

public abstract class AbstractSSHExecutor implements SSHExecutor {
  protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
  protected Session session = null;
  protected Channel channel = null;
  protected SSHSessionConfig config = null;
  protected OutputStream outputStream = null;
  protected OutputStream inputStream = null;

  public void init(SSHSessionConfig config) {
    this.config = config;
    try {
      session = getSession(config);
      channel = session.openChannel("exec");
      ((ChannelExec) channel).setPty(true);
      ((ChannelExec) channel).setErrStream(System.err, true);
      outputStream = new ByteArrayOutputStream();
      channel.setOutputStream(outputStream);
      inputStream = channel.getOutputStream();
    } catch (JSchException e) {
      LOGGER.error("Failed to initialize executor");
      SSHException shEx = extractSSHException(e);
      throw new WingsException(shEx.code, shEx.msg, e.getCause());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void execute(String command) {
    genericExecute(command);
  }

  private void genericExecute(String command) {
    try {
      ((ChannelExec) channel).setCommand(command);
      channel.connect();
      postChannelConnect();
      Thread thread = new Thread(() -> {
        while (!channel.isClosed()) {
          quietSleep(config.getRetryInterval());
        }
      });
      thread.start();
      thread.join(config.getSSHSessionTimeout());

      if (thread.isAlive()) {
        LOGGER.info("Command couldn't complete in time. Connection closed");
      } else {
        LOGGER.info("[" + new String(((ByteArrayOutputStream) outputStream).toByteArray(), "UTF-8") + "]"); // FIXME
        int ec = channel.getExitStatus();
        if (ec != 0) {
          LOGGER.info("Remote command failed with exit status " + ec);
        }
      }
    } catch (JSchException e) {
      SSHException shEx = extractSSHException(e);
      LOGGER.error("Command execution failed with error " + e.getStackTrace());
      throw new WingsException(shEx.code, shEx.msg, e.getCause());
    } catch (InterruptedException e) {
      LOGGER.error("Exception in channel observer thread");
      throw new WingsException(UNKNOWN_ERROR_MEG, UNKNOWN_ERROR_CODE, e.getCause());
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Exception in reading output stream");
      throw new WingsException(UNKNOWN_ERROR_MEG, UNKNOWN_ERROR_CODE, e.getCause());
    } finally {
      destroy();
    }
  }

  public void destroy() {
    if (null != channel) {
      channel.disconnect();
    }
    if (null != session) {
      session.disconnect();
    }
  }

  public void abort() {
    try {
      inputStream.write(3); // Send ^C command
      inputStream.flush();
    } catch (IOException e) {
      LOGGER.error("Abort command failed " + e.getStackTrace());
    }
  }

  public abstract Session getSession(SSHSessionConfig config) throws JSchException;
  public void postChannelConnect(){};

  public class SSHException {
    private String code;
    private String msg;

    private SSHException(String code, String cause) {
      this.code = code;
      this.msg = cause;
    }
  }

  private SSHException extractSSHException(JSchException jSchException) {
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
    return new SSHException(customCode, customMessage);
  }
}

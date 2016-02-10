package software.wings.core.executors;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static software.wings.utils.Misc.quietSleep;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SSHPwdAuthExecutor implements Executor {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SSHPwdAuthExecutor.class);

  private Session session = null;
  private Channel channel = null;
  private SSHSessionConfig config = null;
  private OutputStream outputStream = null;
  private OutputStream inputStream = null;

  public void init(SSHSessionConfig config) {
    this.config = config;
    session = SSHSessionFactory.getSSHSessionWithPwd(config);
    try {
      channel = session.openChannel("exec");
      ((ChannelExec) channel).setPty(true);
      ((ChannelExec) channel).setErrStream(System.err, true);
      outputStream = new ByteArrayOutputStream();
      channel.setOutputStream(outputStream);
      inputStream = channel.getOutputStream();
    } catch (JSchException | IOException ioe) {
      LOGGER.error("Failed to initialize executor");
    }
  }

  @Override
  synchronized public void execute(String command) {
    try {
      ((ChannelExec) channel).setCommand(command);
      channel.connect();
      Thread thread = new Thread(() -> {
        while (!channel.isClosed()) {
          try {
            quietSleep(config.getRetryInterval());
          } catch (Exception e) {
            // ignored
          }
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
    } catch (JSchException | IOException | InterruptedException e) {
      LOGGER.error("Command execution failed with error " + e.getStackTrace());
    } finally {
      destroy();
    }
  }

  @Override
  synchronized public void destroy() {
    if (null != channel) {
      channel.disconnect();
    }
    if (null != session) {
      session.disconnect();
    }
  }

  @Override
  synchronized public void abort() {
    try {
      inputStream.write(3); // Send ^C command
      inputStream.flush();
    } catch (IOException e) {
      LOGGER.error("Abort command failed " + e.getStackTrace());
    }
  }

  public Channel getChannel() {
    return channel;
  }
}

package software.wings.helpers.executors;

import com.jcraft.jsch.*;
import org.slf4j.LoggerFactory;
import software.wings.helpers.executors.callbacks.SSHCommandExecutionCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static software.wings.utils.Misc.quietSleep;

/**
 * Created by anubhaw on 2/8/16.
 */

public class SSHPubKeyCommandExecutor implements CommandExecutor {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SSHCommandExecutor.class);
  private String hostName;
  private String sshUser;
  private String sshPassword;
  private String command;
  private SSHCommandExecutionCallback callback;
  private Session session;
  private Channel channel;

  private int sshPort = 22;

  public SSHPubKeyCommandExecutor(String hostName, int sshPort, String sshUser, String sshPassword, String command,
      SSHCommandExecutionCallback callback) {
    this.hostName = hostName;
    this.sshPort = sshPort;
    this.sshUser = sshUser;
    this.sshPassword = sshPassword;
    this.command = command;
    this.callback = callback;
  }

  @Override
  public void execute() {
    try {
      JSch jsch = new JSch();
      jsch.addIdentity("/Users/anubhaw/.ssh/id_rsa");
      session = jsch.getSession(sshUser, hostName, sshPort);
      UserInfo ui = new MyUserInfo(sshPassword);
      session.setUserInfo(ui);

      callback.log("Trying to connect over ssh");
      session.connect(SSHConnectionTimeout);
      session.setTimeout(SSHSessionTimeout);
      callback.log("Connection established... going to execute command : " + command);
      channel = session.openChannel("exec");
      ((ChannelExec) channel).setCommand(command);
      ((ChannelExec) channel).setErrStream(System.err);

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      channel.setOutputStream(os);
      //            OutputStream out = channel.getOutputStream();

      ((ChannelExec) channel).setPty(true);
      channel.connect();
      //            out.write((ui.getPassword() + "\n").getBytes());
      //            out.flush();

      Thread thread = new Thread(() -> {
        while (!channel.isClosed()) {
          try {
            quietSleep(RetryInterval);
          } catch (Exception e) {
            // ignored
          }
        }
      });
      thread.start();
      thread.join(SSHSessionTimeout);

      if (thread.isAlive()) {
        callback.log("Command couldn't complete in time. Connection closed");
      } else {
        callback.log("[" + new String(os.toByteArray(), "UTF-8") + "]");
        int ec = channel.getExitStatus();
        if (ec != 0) {
          callback.log("Remote command failed with exit status " + ec);
        }
      }
    } catch (JSchException | IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (null != channel) {
        channel.disconnect();
      }
      if (null != session) {
        session.disconnect();
      }
    }
  }
}

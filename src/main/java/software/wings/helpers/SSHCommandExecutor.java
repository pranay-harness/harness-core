package software.wings.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import software.wings.utils.Misc;

public class SSHCommandExecutor {
  private String hostName;
  private String sshUser;
  private String sshPassword;
  private String command;
  private SSHCommandExecutionCallback callback;
  private int sshPort = 22;

  public SSHCommandExecutor(String hostName, int sshPort, String sshUser, String sshPassword, String command,
      SSHCommandExecutionCallback callback) {
    this.hostName = hostName;
    this.sshPort = sshPort;
    this.sshUser = sshUser;
    this.sshPassword = sshPassword;
    this.command = command;
    this.callback = callback;
  }

  public void execute() {
    try {
      JSch jsch = new JSch();
      Session session = jsch.getSession(sshUser, hostName, sshPort);
      UserInfo ui = new MyUserInfo(sshPassword);
      session.setUserInfo(ui);

      callback.log("Trying to connect over ssh");
      session.connect();
      callback.log("Connection established... going to execute command : " + command);
      Channel channel = session.openChannel("exec");
      ((ChannelExec) channel).setCommand(command);
      channel.setInputStream(null);
      ((ChannelExec) channel).setErrStream(System.err);

      InputStream in = channel.getInputStream();

      channel.connect();

      byte[] tmp = new byte[1024];
      while (true) {
        while (in.available() > 0) {
          int i = in.read(tmp, 0, 1024);
          if (i < 0)
            break;
          callback.log(new String(tmp, 0, i));
        }
        if (channel.isClosed()) {
          if (in.available() > 0)
            continue;
          callback.log("SSH Command execution completed - exit-status: " + channel.getExitStatus());
          break;
        }
        Misc.quietSleep(1000);
      }
      channel.disconnect();
      session.disconnect();
    } catch (JSchException | IOException e) {
      e.printStackTrace();
    }
  }

  private static class MyUserInfo implements UserInfo {
    private String sshPassword;
    public MyUserInfo(String sshPassword) {
      this.sshPassword = sshPassword;
    }
    public String getPassword() {
      return sshPassword;
    }
    public boolean promptYesNo(String str) {
      return true;
    }
    public String getPassphrase() {
      return null;
    }
    public boolean promptPassphrase(String message) {
      return true;
    }
    public boolean promptPassword(String message) {
      return true;
    }
    public void showMessage(String message) {
      System.out.println(message);
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(SSHCommandExecutor.class);
}

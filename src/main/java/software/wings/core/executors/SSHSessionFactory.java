package software.wings.core.executors;

import com.jcraft.jsch.*;

/**
 * Created by anubhaw on 2/8/16.
 */

public class SSHSessionFactory {
  public static Session getSSHSessionWithPwd(SSHSessionConfig config) {
    JSch jsch = new JSch();
    Session session = null;
    try {
      session = jsch.getSession(config.getUser(), config.getHost(), config.getPort());
      session.setPassword(config.getPassword());
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect(config.getSSHConnectionTimeout());
      session.setTimeout(config.getSSHSessionTimeout());
    } catch (JSchException e) {
      e.printStackTrace();
    }
    return session;
  }

  public static Session getSSHSessionWithKey(SSHSessionConfig config) {
    JSch jsch = new JSch();
    Session session = null;
    try {
      jsch.addIdentity(config.getKeyPath());
      session = jsch.getSession(config.getUser(), config.getHost(), config.getPort());
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect(config.getSSHConnectionTimeout());
      session.setTimeout(config.getSSHSessionTimeout());
    } catch (JSchException e) {
      e.printStackTrace();
    }
    return session;
  }
}

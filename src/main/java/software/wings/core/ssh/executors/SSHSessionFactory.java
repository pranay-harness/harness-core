package software.wings.core.ssh.executors;

import com.jcraft.jsch.*;
import org.slf4j.*;
import org.slf4j.Logger;

/**
 * Created by anubhaw on 2/8/16.
 */

public class SSHSessionFactory {
  private final static Logger LOGGER = LoggerFactory.getLogger(SSHSessionFactory.class);

  public static Session getSSHSession(SSHSessionConfig config, String credentialType) {
    JSch jsch = new JSch();
    Session session = null;
    try {
      if ("KEY".equals(credentialType)) {
        jsch.addIdentity(config.getKeyPath());
        session = jsch.getSession(config.getUser(), config.getHost(), config.getPort());
      } else {
        session = jsch.getSession(config.getUser(), config.getHost(), config.getPort());
        session.setPassword(config.getPassword());
      }
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect(config.getSSHConnectionTimeout());
      session.setTimeout(config.getSSHSessionTimeout());
    } catch (JSchException e) {
      e.printStackTrace();
    }
    return session;
  }

  public static Session getSSHSessionWithJumpbox(SSHSessionConfig config) {
    Session session = null;
    try {
      Session jumpboxSession = getSSHSession(config.getJumpboxConfig(), "PASSWORD");
      int forwardingPort = jumpboxSession.setPortForwardingL(0, config.getHost(), config.getPort());
      LOGGER.info("portforwarding port " + forwardingPort);
      session = (new JSch()).getSession(config.getUser(), "127.0.0.1", forwardingPort);
      session.setConfig("StrictHostKeyChecking", "no");
      session.setPassword(config.getPassword());
      session.connect(config.getSSHConnectionTimeout());
      session.setTimeout(config.getSSHSessionTimeout());
    } catch (JSchException e) {
      e.printStackTrace();
    }
    return session;
  }
}

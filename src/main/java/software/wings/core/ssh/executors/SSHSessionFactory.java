package software.wings.core.ssh.executors;

import com.jcraft.jsch.*;
import org.slf4j.*;
import org.slf4j.Logger;
import software.wings.core.ssh.executors.SSHSessionConfig.SSHSessionConfigBuilder;

/**
 * Created by anubhaw on 2/8/16.
 */

public class SSHSessionFactory {
  private final static Logger LOGGER = LoggerFactory.getLogger(SSHSessionFactory.class);

  public static Session getSSHSession(SSHSessionConfig config) {
    JSch jsch = new JSch();
    Session session = null;
    try {
      if ("KEY".equals(getSessionType(config))) {
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
      Session jumpboxSession = getSSHSession(config.getJumpboxConfig());
      int forwardingPort = jumpboxSession.setPortForwardingL(0, config.getHost(), config.getPort());
      LOGGER.info("portforwarding port " + forwardingPort);

      SSHSessionConfig newConfig = new SSHSessionConfigBuilder()
                                       .user(config.getUser())
                                       .password(config.getPassword())
                                       .keyPath(config.getKeyPath())
                                       .host("127.0.0.1")
                                       .port(forwardingPort)
                                       .build();
      session = getSSHSession(newConfig);
    } catch (JSchException e) {
      e.printStackTrace();
    }
    return session;
  }

  private static String getSessionType(SSHSessionConfig config) {
    return config.getKeyPath() != null && config.getKeyPath().length() > 0 ? "KEY" : "PASSWORD";
  }
}

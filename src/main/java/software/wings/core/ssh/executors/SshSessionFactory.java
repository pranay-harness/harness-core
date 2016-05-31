package software.wings.core.ssh.executors;

import static software.wings.core.ssh.executors.SshSessionConfig.SshSessionConfigBuilder.aSshSessionConfig;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SshSessionFactory {
  private static final Logger logger = LoggerFactory.getLogger(SshSessionFactory.class);

  public static Session getSSHSessionWithJumpbox(SshSessionConfig config) {
    Session session = null;
    try {
      Session jumpboxSession = getSSHSession(config.getJumpboxConfig());
      int forwardingPort = jumpboxSession.setPortForwardingL(0, config.getHost(), config.getPort());
      logger.info("portforwarding port " + forwardingPort);

      SshSessionConfig newConfig = aSshSessionConfig()
                                       .withUserName(config.getUserName())
                                       .withPassword(config.getPassword())
                                       .withKey(config.getKey())
                                       .withHost("127.0.0.1")
                                       .withPort(forwardingPort)
                                       .build();
      session = getSSHSession(newConfig);
    } catch (JSchException e) {
      e.printStackTrace();
    }
    return session;
  }

  public static Session getSSHSession(SshSessionConfig config) throws JSchException {
    JSch jsch = new JSch();
    JSch.setLogger(new MyLogger());

    Session session = null;
    if ("KEY".equals(getSessionType(config))) {
      if (null == config.getKeyPassphrase()) {
        jsch.addIdentity(config.getKey());
      } else {
        jsch.addIdentity(config.getKey(), config.getKeyPassphrase());
      }
      session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
    } else {
      session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
      session.setPassword(config.getPassword());
    }
    session.setConfig("StrictHostKeyChecking", "no");
    session.connect(config.getSshConnectionTimeout());
    session.setTimeout(config.getSshSessionTimeout());
    return session;
  }

  private static String getSessionType(SshSessionConfig config) {
    return config.getKey() != null && config.getKey().length() > 0 ? "KEY" : "PASSWORD";
  }

  public static class MyLogger implements com.jcraft.jsch.Logger {
    static java.util.Hashtable name = new java.util.Hashtable();

    static {
      name.put(DEBUG, "DEBUG: ");
      name.put(INFO, "INFO: ");
      name.put(WARN, "WARN: ");
      name.put(ERROR, "ERROR: ");
      name.put(FATAL, "FATAL: ");
    }

    public boolean isEnabled(int level) {
      return true;
    }

    public void log(int level, String message) {
      switch (level) {
        case DEBUG:
          logger.debug(message);
          break;
        case INFO:
          logger.info(message);
          break;
        case WARN:
          logger.warn(message);
          break;
        case FATAL:
        case ERROR:
          logger.error(message);
          break;
      }
    }
  }
}

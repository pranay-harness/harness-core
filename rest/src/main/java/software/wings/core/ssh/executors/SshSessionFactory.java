package software.wings.core.ssh.executors;

import static org.eclipse.jetty.util.StringUtil.isNotBlank;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SshSessionFactory {
  private static final Logger logger = LoggerFactory.getLogger(SshSessionFactory.class);

  /**
   * Gets the SSH session with jumpbox.
   *
   * @param config the config
   * @return the SSH session with jumpbox
   * @throws JSchException the j sch exception
   */
  public static Session getSSHSessionWithJumpbox(SshSessionConfig config) throws JSchException {
    Session session = null;
    Session jumpboxSession = getSSHSession(config.getBastionHostConfig());
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

    return session;
  }

  /**
   * Gets the SSH session.
   *
   * @param config the config
   * @return the SSH session
   * @throws JSchException the j sch exception
   */
  public static Session getSSHSession(SshSessionConfig config) throws JSchException {
    JSch jsch = new JSch();
    //    JSch.setLogger(new jschLogger());

    Session session = null;
    if (isNotBlank(config.getKey())) {
      if (null == config.getKeyPassphrase()) {
        jsch.addIdentity(config.getKeyName(), config.getKey().getBytes(StandardCharsets.UTF_8), null, null);
      } else {
        jsch.addIdentity(config.getKeyName(), config.getKey().getBytes(StandardCharsets.UTF_8), null,
            config.getKeyPassphrase().getBytes(StandardCharsets.UTF_8));
      }
      session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
    } else {
      session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
      session.setPassword(config.getPassword());
    }
    session.setConfig("StrictHostKeyChecking", "no");
    session.setUserInfo(new SshUserInfo(config.getPassword()));
    session.setTimeout(config.getSshSessionTimeout());
    session.setServerAliveInterval(10 * 1000); // Send noop packet every 10 sec
    Socket client = new Socket();
    try {
      client.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getSocketConnectTimeout());
      client.close();
    } catch (IOException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      throw new JSchException("timeout: socket is not established", e);
    } finally {
      IOUtils.closeQuietly(client);
    }
    session.connect(config.getSshConnectionTimeout());

    return session;
  }

  /**
   * The Class jschLogger.
   */
  public static class jschLogger implements com.jcraft.jsch.Logger {
    /**
     * The Name.
     */
    static java.util.Hashtable name = new java.util.Hashtable();

    static {
      name.put(DEBUG, "DEBUG: ");
      name.put(INFO, "INFO: ");
      name.put(WARN, "WARN: ");
      name.put(ERROR, "ERROR: ");
      name.put(FATAL, "FATAL: ");
    }

    /* (non-Javadoc)
     * @see com.jcraft.jsch.Logger#isEnabled(int)
     */
    public boolean isEnabled(int level) {
      return true;
    }

    /* (non-Javadoc)
     * @see com.jcraft.jsch.Logger#log(int, java.lang.String)
     */
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

package software.wings.core.ssh.executors;

import static io.harness.govern.Switch.unhandled;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import com.google.common.base.Charsets;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.security.encryption.EncryptionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

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
    if (config.isKeyLess()) {
      String keyPath = getKeyPath(config);
      if (!new File(keyPath).isFile()) {
        throw new JSchException("File at " + keyPath + " does not exist", new FileNotFoundException());
      }
      jsch.addIdentity(keyPath);
      session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
    } else if (config.getKey() != null && config.getKey().length > 0) {
      if (null == config.getKeyPassphrase()) {
        jsch.addIdentity(config.getKeyName(), EncryptionUtils.toBytes(config.getKey(), Charsets.UTF_8), null, null);
      } else {
        jsch.addIdentity(config.getKeyName(), EncryptionUtils.toBytes(config.getKey(), Charsets.UTF_8), null,
            EncryptionUtils.toBytes(config.getKeyPassphrase(), Charsets.UTF_8));
      }
      session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
    } else {
      session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
      session.setPassword(new String(config.getPassword()));
      session.setUserInfo(new SshUserInfo(new String(config.getPassword())));
    }
    session.setConfig("StrictHostKeyChecking", "no");

    session.setTimeout(config.getSshSessionTimeout());
    session.setServerAliveInterval(10 * 1000); // Send noop packet every 10 sec

    session.connect(config.getSshConnectionTimeout());

    return session;
  }

  protected static String getKeyPath(SshSessionConfig config) {
    String userhome = System.getProperty("user.home");
    String keyPath = userhome + File.separator + ".ssh" + File.separator + "id_rsa";
    if (config.getKeyPath() != null) {
      keyPath = config.getKeyPath();
      keyPath = keyPath.replace("$HOME", userhome);
    }
    return keyPath;
  }

  /**
   * The Class jschLogger.
   */
  @SuppressFBWarnings("NM_CLASS_NAMING_CONVENTION")
  public static class jschLogger implements com.jcraft.jsch.Logger {
    /**
     * The Name.
     */
    static Map name = new java.util.Hashtable();

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
        default:
          unhandled(level);
      }
    }
  }
}

package software.wings.core.ssh.executors;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.KERBEROS;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import com.google.common.base.Charsets;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.KerberosConfig;
import software.wings.security.encryption.EncryptionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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

    Session session;
    if (config.getAuthenticationScheme() != null && config.getAuthenticationScheme().equals(KERBEROS)) {
      logger.info("SSH using Kerberos Auth");
      logger.info("Do we need to generate Ticket Granting Ticket(TGT)? " + config.getKerberosConfig().isGenerateTGT());
      if (config.getKerberosConfig() != null && config.getKerberosConfig().isGenerateTGT()) {
        if (!isValidKeyTabFile(config.getKerberosConfig().getKeyTabFilePath())) {
          logger.error("Cannot proceed with Ticket Granting Ticket(TGT) generation");
          throw new JSchException(
              "Failure: Invalid keytab file path. Cannot proceed with Ticket Granting Ticket(TGT) generation");
        }
        logger.info("Generating Ticket Granting Ticket(TGT)...");
        boolean ticketGenerated = generateTGT(
            config.getKerberosConfig(), config.getPassword() != null ? new String(config.getPassword()) : null);
        if (ticketGenerated) {
          logger.info("Ticket Granting Ticket(TGT) generated successfully for "
              + config.getKerberosConfig().getPrincipalWithRealm());
        } else {
          logger.error("Failure: could not generate Ticket Granting Ticket(TGT)");
          throw new JSchException("Failure: could not generate Ticket Granting Ticket(TGT)");
        }
      }

      session = jsch.getSession(config.getKerberosConfig().getPrincipal(), config.getHost(), config.getPort());
      session.setConfig("PreferredAuthentications", "gssapi-with-mic");
    } else if (config.isKeyLess()) {
      String keyPath = getKeyPath(config);
      if (!new File(keyPath).isFile()) {
        throw new JSchException("File at " + keyPath + " does not exist", new FileNotFoundException());
      }
      if (null == config.getKeyPassphrase()) {
        jsch.addIdentity(keyPath);
      } else {
        jsch.addIdentity(keyPath, EncryptionUtils.toBytes(config.getKeyPassphrase(), Charsets.UTF_8));
      }
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

  private static boolean generateTGT(KerberosConfig kerberosConfig, String password) {
    logger.info("Generating Ticket Granting Ticket(TGT) for principal: " + kerberosConfig.getPrincipalWithRealm());
    String commandString = !StringUtils.isEmpty(password)
        ? format("echo %s | kinit %s", password, kerberosConfig.getPrincipalWithRealm())
        : format("kinit %s -k -t %s", kerberosConfig.getPrincipalWithRealm(), kerberosConfig.getKeyTabFilePath());
    logger.info("Executing command " + commandString);
    return executeLocalCommand(commandString);
  }

  private static boolean isValidKeyTabFile(String keyTabFilePath) {
    if (!StringUtils.isEmpty(keyTabFilePath)) {
      if (new File(keyTabFilePath).exists()) {
        logger.info(format("Found keytab file at path: [%s]", keyTabFilePath));
        return true;
      } else {
        logger.error(format("Invalid keytab file path: [%s].", keyTabFilePath));
        return false;
      }
    }
    return true;
  }

  private static boolean executeLocalCommand(String cmdString) {
    String[] commandList = new String[] {"sh", "-c", cmdString};
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .command(commandList)
                                          .directory(new File(System.getProperty("user.home")))
                                          .readOutput(true)
                                          .redirectOutput(new LogOutputStream() {
                                            @Override
                                            protected void processLine(String line) {
                                              logger.info(line);
                                            }
                                          })
                                          .redirectError(new LogOutputStream() {
                                            @Override
                                            protected void processLine(String line) {
                                              logger.error(line);
                                            }
                                          });

    ProcessResult processResult = null;
    try {
      processResult = processExecutor.execute();
    } catch (IOException | InterruptedException | TimeoutException e) {
      logger.error("Failed to execute command ", e);
    }
    return processResult != null && processResult.getExitValue() == 0;
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

package software.wings.core.ssh.executors;

import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.KERBEROS;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import io.harness.logging.LogCallback;
import io.harness.security.EncryptionUtils;
import io.harness.ssh.SshHelperUtils;

import software.wings.beans.command.NoopExecutionCallback;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by anubhaw on 2/8/16.
 */
@UtilityClass
@Slf4j
public class SshSessionFactory {
  /**
   * Gets the SSH session with jumpbox.
   *
   * @param config the config
   * @param logCallback the LogCallback
   * @return the SSH session with jumpbox
   * @throws JSchException the j sch exception
   */
  public static Session getSSHSessionWithJumpbox(SshSessionConfig config, LogCallback logCallback)
      throws JSchException {
    Session jumpboxSession = getSSHSession(config.getBastionHostConfig());
    int forwardingPort = jumpboxSession.setPortForwardingL(0, config.getHost(), config.getPort());
    log.info("portforwarding port " + forwardingPort);
    logCallback.saveExecutionLog("portforwarding port " + forwardingPort);

    SshSessionConfig newConfig = aSshSessionConfig()
                                     .withUserName(config.getUserName())
                                     .withPassword(config.getPassword())
                                     .withKey(config.getKey())
                                     .withHost("127.0.0.1")
                                     .withPort(forwardingPort)
                                     .build();
    return getSSHSession(newConfig);
  }

  /**
   * Gets the SSH session.
   *
   * @param config the config
   * @return the SSH session
   * @throws JSchException the j sch exception
   */
  public static Session getSSHSession(SshSessionConfig config) throws JSchException {
    return getSSHSession(config, new NoopExecutionCallback());
  }

  /**
   * Gets the SSH session.
   *
   * @param config the config
   * @param logCallback the LogCallback
   * @return the SSH session
   * @throws JSchException the j sch exception
   */
  public static Session getSSHSession(SshSessionConfig config, LogCallback logCallback) throws JSchException {
    JSch jsch = new JSch();

    Session session;
    if (config.getAuthenticationScheme() != null && config.getAuthenticationScheme() == KERBEROS) {
      logCallback.saveExecutionLog("SSH using Kerberos Auth");
      log.info("SSH using Kerberos Auth");
      generateTGTUsingSshConfig(config, logCallback);

      session = jsch.getSession(config.getKerberosConfig().getPrincipal(), config.getHost(), config.getPort());
      session.setConfig("PreferredAuthentications", "gssapi-with-mic");
    } else if (config.getAccessType() != null && config.getAccessType() == USER_PASSWORD) {
      log.info("SSH using Username Password");
      session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
      byte[] password = EncryptionUtils.toBytes(config.getSshPassword(), Charsets.UTF_8);
      session.setPassword(password);
      session.setUserInfo(new SshUserInfo(new String(password, Charsets.UTF_8)));
    } else if (config.isKeyLess()) {
      log.info("SSH using KeyPath");
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
    } else {
      if (config.getKey() != null && config.getKey().length > 0) {
        // Copy Key because EncryptionUtils has a side effect of modifying the original array
        final char[] copyOfKey = getCopyOfKey(config);
        log.info("SSH using Key");
        if (null == config.getKeyPassphrase()) {
          jsch.addIdentity(config.getKeyName(), EncryptionUtils.toBytes(copyOfKey, Charsets.UTF_8), null, null);
        } else {
          jsch.addIdentity(config.getKeyName(), EncryptionUtils.toBytes(copyOfKey, Charsets.UTF_8), null,
              EncryptionUtils.toBytes(config.getKeyPassphrase(), Charsets.UTF_8));
        }
        session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
      } else {
        log.warn("User password on commandline is not supported...");
        session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
        session.setPassword(new String(config.getPassword()));
        session.setUserInfo(new SshUserInfo(new String(config.getPassword())));
      }
    }
    session.setConfig("StrictHostKeyChecking", "no");
    session.setTimeout(config.getSshSessionTimeout());
    session.setServerAliveInterval(10 * 1000); // Send noop packet every 10 sec

    session.connect(config.getSshConnectionTimeout());

    return session;
  }

  @VisibleForTesting
  static char[] getCopyOfKey(SshSessionConfig config) {
    return Arrays.copyOf(config.getKey(), config.getKey().length);
  }

  public static void generateTGTUsingSshConfig(SshSessionConfig config, LogCallback logCallback) throws JSchException {
    if (config.getKerberosConfig() == null) {
      return;
    }
    log.info("Do we need to generate Ticket Granting Ticket(TGT)? " + config.getKerberosConfig().isGenerateTGT());
    if (config.getKerberosConfig().isGenerateTGT()) {
      SshHelperUtils.generateTGT(config.getKerberosConfig().getPrincipalWithRealm(),
          config.getPassword() != null ? new String(config.getPassword()) : null,
          config.getKerberosConfig().getKeyTabFilePath(), logCallback);
    }
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
}

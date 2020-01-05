package software.wings.core.ssh.executors;

import static java.lang.String.format;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.KERBEROS;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import com.google.common.base.Charsets;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.harness.security.EncryptionUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.KerberosConfig;
import software.wings.beans.command.LogCallback;
import software.wings.beans.command.NoopExecutionCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

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
    logger.info("portforwarding port " + forwardingPort);
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
    if (config.getAuthenticationScheme() != null && config.getAuthenticationScheme().equals(KERBEROS)) {
      logCallback.saveExecutionLog("SSH using Kerberos Auth");
      logger.info("SSH using Kerberos Auth");
      generateTGT(config, logCallback);

      session = jsch.getSession(config.getKerberosConfig().getPrincipal(), config.getHost(), config.getPort());
      session.setConfig("PreferredAuthentications", "gssapi-with-mic");
    } else if (config.getAccessType() != null && config.getAccessType().equals(USER_PASSWORD)) {
      logger.info("SSH using Username Password");
      session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
      byte[] password = EncryptionUtils.toBytes(config.getSshPassword(), Charsets.UTF_8);
      session.setPassword(password);
      session.setUserInfo(new SshUserInfo(new String(password, Charsets.UTF_8)));
    } else if (config.isKeyLess()) {
      logger.info("SSH using KeyPath");
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
      logger.info("SSH using Key");
      if (null == config.getKeyPassphrase()) {
        jsch.addIdentity(config.getKeyName(), EncryptionUtils.toBytes(config.getKey(), Charsets.UTF_8), null, null);
      } else {
        jsch.addIdentity(config.getKeyName(), EncryptionUtils.toBytes(config.getKey(), Charsets.UTF_8), null,
            EncryptionUtils.toBytes(config.getKeyPassphrase(), Charsets.UTF_8));
      }
      session = jsch.getSession(config.getUserName(), config.getHost(), config.getPort());
    } else {
      logger.warn("User password on commandline is not supported...");
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

  public static void generateTGT(SshSessionConfig config, LogCallback logCallback) throws JSchException {
    logger.info("Do we need to generate Ticket Granting Ticket(TGT)? " + config.getKerberosConfig().isGenerateTGT());
    if (config.getKerberosConfig() != null && config.getKerberosConfig().isGenerateTGT()) {
      if (!isValidKeyTabFile(config.getKerberosConfig().getKeyTabFilePath())) {
        logCallback.saveExecutionLog("Cannot proceed with Ticket Granting Ticket(TGT) generation.", ERROR);
        logger.error("Cannot proceed with Ticket Granting Ticket(TGT) generation");
        throw new JSchException(
            "Failure: Invalid keytab file path. Cannot proceed with Ticket Granting Ticket(TGT) generation");
      }
      logger.info("Generating Ticket Granting Ticket(TGT)...");
      boolean ticketGenerated = generateTGT(config.getKerberosConfig(),
          config.getPassword() != null ? new String(config.getPassword()) : null, logCallback);
      if (ticketGenerated) {
        logCallback.saveExecutionLog("Ticket Granting Ticket(TGT) generated successfully for "
            + config.getKerberosConfig().getPrincipalWithRealm());
        logger.info("Ticket Granting Ticket(TGT) generated successfully for "
            + config.getKerberosConfig().getPrincipalWithRealm());
      } else {
        logger.error("Failure: could not generate Ticket Granting Ticket(TGT)");
        throw new JSchException("Failure: could not generate Ticket Granting Ticket(TGT)");
      }
    }
  }

  private static boolean generateTGT(KerberosConfig kerberosConfig, String password, LogCallback logCallback) {
    logCallback.saveExecutionLog(
        "Generating Ticket Granting Ticket(TGT) for principal: " + kerberosConfig.getPrincipalWithRealm());
    logger.info("Generating Ticket Granting Ticket(TGT) for principal: " + kerberosConfig.getPrincipalWithRealm());
    String commandString = !StringUtils.isEmpty(password)
        ? format("echo %s | kinit %s", password, kerberosConfig.getPrincipalWithRealm())
        : format("kinit -k -t %s %s", kerberosConfig.getKeyTabFilePath(), kerberosConfig.getPrincipalWithRealm());
    return executeLocalCommand(commandString);
  }

  private static boolean isValidKeyTabFile(String keyTabFilePath) {
    if (!StringUtils.isEmpty(keyTabFilePath)) {
      if (new File(keyTabFilePath).exists()) {
        logger.info("Found keytab file at path: [{}]", keyTabFilePath);
        return true;
      } else {
        logger.error("Invalid keytab file path: [{}].", keyTabFilePath);
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
}

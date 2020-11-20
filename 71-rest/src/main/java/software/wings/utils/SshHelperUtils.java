package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.CONNECTION_TIMEOUT;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.INVALID_KEY;
import static io.harness.eraro.ErrorCode.INVALID_KEYPATH;
import static io.harness.eraro.ErrorCode.SOCKET_CONNECTION_ERROR;
import static io.harness.eraro.ErrorCode.SOCKET_CONNECTION_TIMEOUT;
import static io.harness.eraro.ErrorCode.SSH_CONNECTION_ERROR;
import static io.harness.eraro.ErrorCode.SSH_SESSION_TIMEOUT;
import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;
import static io.harness.eraro.ErrorCode.UNKNOWN_HOST;
import static io.harness.eraro.ErrorCode.UNREACHABLE_HOST;
import static io.harness.logging.LogLevel.ERROR;
import static java.lang.String.format;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SU_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.KERBEROS;
import static software.wings.core.ssh.executors.ScriptExecutor.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.ScriptExecutor.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.ScriptExecutor.ExecutorType.PASSWORD_AUTH;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import com.jcraft.jsch.JSchException;
import com.sun.mail.iap.ConnectionException;
import io.harness.eraro.ErrorCode;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.netty.channel.ConnectTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.AuthenticationScheme;
import software.wings.beans.KerberosConfig;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.core.ssh.executors.ScriptExecutor.ExecutorType;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.ssh.executors.SshSessionConfig.Builder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Created by anubhaw on 2/23/17.
 */
@Slf4j
public class SshHelperUtils {
  private static ExecutorType getExecutorType(
      SettingAttribute hostConnectionSetting, SettingAttribute bastionHostConnectionSetting) {
    ExecutorType executorType;
    if (bastionHostConnectionSetting != null) {
      executorType = BASTION_HOST;
    } else {
      HostConnectionAttributes hostConnectionAttributes = (HostConnectionAttributes) hostConnectionSetting.getValue();
      AccessType accessType = hostConnectionAttributes.getAccessType();
      if (accessType == AccessType.KEY || accessType == KEY_SU_APP_USER || accessType == KEY_SUDO_APP_USER) {
        executorType = KEY_AUTH;
      } else {
        executorType = PASSWORD_AUTH;
      }
    }
    return executorType;
  }

  /**
   * Normalize error.
   *
   * @param jschexception the jschexception
   * @return the string
   */
  public static ErrorCode normalizeError(JSchException jschexception) {
    String message = jschexception.getMessage();
    Throwable cause = jschexception.getCause();

    ErrorCode errorConst = UNKNOWN_ERROR;

    if (cause != null) { // TODO: Refactor use enums, maybe ?
      if (cause instanceof NoRouteToHostException) {
        errorConst = UNREACHABLE_HOST;
      } else if (cause instanceof UnknownHostException) {
        errorConst = UNKNOWN_HOST;
      } else if (cause instanceof SocketTimeoutException) {
        errorConst = SOCKET_CONNECTION_TIMEOUT;
      } else if (cause instanceof ConnectTimeoutException) {
        errorConst = CONNECTION_TIMEOUT;
      } else if (cause instanceof ConnectionException) {
        errorConst = SSH_CONNECTION_ERROR;
      } else if (cause instanceof SocketException) {
        errorConst = SOCKET_CONNECTION_ERROR;
      } else if (cause instanceof FileNotFoundException) {
        errorConst = INVALID_KEYPATH;
      }
    } else {
      if (message.startsWith("invalid privatekey")) {
        errorConst = INVALID_KEY;
      } else if (message.contains("Auth fail") || message.contains("Auth cancel") || message.contains("USERAUTH fail")
          || message.contains("authentication failure")) {
        errorConst = INVALID_CREDENTIAL;
      } else if (message.startsWith("timeout: socket is not established")
          || message.contains("SocketTimeoutException")) {
        errorConst = SOCKET_CONNECTION_TIMEOUT;
      } else if (message.equals("session is down")) {
        errorConst = SSH_SESSION_TIMEOUT;
      }
    }
    return errorConst;
  }

  public static SshSessionConfig createSshSessionConfig(SettingAttribute settingAttribute, String hostName) {
    Builder builder = aSshSessionConfig().withAccountId(settingAttribute.getAccountId()).withHost(hostName);
    populateBuilderWithCredentials(builder, settingAttribute, null);
    return builder.build();
  }
  public static SshSessionConfig createSshSessionConfig(String commandName, CommandExecutionContext context) {
    SSHExecutionCredential sshExecutionCredential = (SSHExecutionCredential) context.getExecutionCredential();

    String hostName = context.getHost().getPublicDns();
    Builder builder = aSshSessionConfig()
                          .withAccountId(context.getAccountId())
                          .withAppId(context.getAppId())
                          .withExecutionId(context.getActivityId())
                          .withHost(hostName)
                          .withCommandUnitName(commandName);

    // TODO: The following can be removed as we do not support username and password from context anymore
    if (sshExecutionCredential != null) {
      builder.withUserName(sshExecutionCredential.getSshUser())
          .withPassword(sshExecutionCredential.getSshPassword())
          .withSudoAppName(sshExecutionCredential.getAppAccount())
          .withSudoAppPassword(sshExecutionCredential.getAppAccountPassword());
    }

    populateBuilderWithCredentials(
        builder, context.getHostConnectionAttributes(), context.getBastionConnectionAttributes());
    return builder.build();
  }

  public static void populateBuilderWithCredentials(
      Builder builder, SettingAttribute hostConnectionSetting, SettingAttribute bastionHostConnectionSetting) {
    ExecutorType executorType = getExecutorType(hostConnectionSetting, bastionHostConnectionSetting);

    builder.withExecutorType(executorType);
    HostConnectionAttributes hostConnectionAttributes = (HostConnectionAttributes) hostConnectionSetting.getValue();

    if (executorType == KEY_AUTH) {
      if (isNotEmpty(hostConnectionAttributes.getKey())) {
        builder.withKey(new String(hostConnectionAttributes.getKey()).toCharArray());
      }

      if (isNotEmpty(hostConnectionAttributes.getPassphrase())) {
        builder.withKeyPassphrase(new String(hostConnectionAttributes.getPassphrase()).toCharArray());
      }

      builder.withUserName(hostConnectionAttributes.getUserName())
          .withPort(hostConnectionAttributes.getSshPort())
          .withKeyName(hostConnectionSetting.getUuid())
          .withPassword(null)
          .withKeyLess(hostConnectionAttributes.isKeyless())
          .withKeyPath(hostConnectionAttributes.getKeyPath());
    } else if (KERBEROS == hostConnectionAttributes.getAuthenticationScheme()) {
      KerberosConfig kerberosConfig = hostConnectionAttributes.getKerberosConfig();

      if (isNotEmpty(hostConnectionAttributes.getKerberosPassword())) {
        builder.withPassword(new String(hostConnectionAttributes.getKerberosPassword()).toCharArray());
      }

      builder.withAuthenticationScheme(KERBEROS)
          .withKerberosConfig(kerberosConfig)
          .withPort(hostConnectionAttributes.getSshPort());
    } else if (USER_PASSWORD == hostConnectionAttributes.getAccessType()) {
      if (isNotEmpty(hostConnectionAttributes.getSshPassword())) {
        builder.withSshPassword(new String(hostConnectionAttributes.getSshPassword()).toCharArray());
      }

      builder.withAuthenticationScheme(AuthenticationScheme.SSH_KEY)
          .withAccessType(hostConnectionAttributes.getAccessType())
          .withUserName(hostConnectionAttributes.getUserName())
          .withPort(hostConnectionAttributes.getSshPort());
    }

    if (bastionHostConnectionSetting != null) {
      BastionConnectionAttributes bastionAttrs = (BastionConnectionAttributes) bastionHostConnectionSetting.getValue();
      Builder sshSessionConfig = aSshSessionConfig()
                                     .withHost(bastionAttrs.getHostName())
                                     .withKeyName(bastionHostConnectionSetting.getUuid())
                                     .withUserName(bastionAttrs.getUserName())
                                     .withPort(bastionAttrs.getSshPort());

      if (isNotEmpty(bastionAttrs.getKey())) {
        sshSessionConfig.withKey(new String(bastionAttrs.getKey()).toCharArray());
      }

      if (isNotEmpty(bastionAttrs.getPassphrase())) {
        sshSessionConfig.withKeyPassphrase(new String(bastionAttrs.getPassphrase()).toCharArray());
      }

      builder.withBastionHostConfig(sshSessionConfig.build());
    }
  }

  public static void generateTGT(String userPrincipal, String password, String keyTabFilePath, LogCallback logCallback)
      throws JSchException {
    if (!isValidKeyTabFile(keyTabFilePath)) {
      logCallback.saveExecutionLog("Cannot proceed with Ticket Granting Ticket(TGT) generation.", ERROR);
      log.error("Cannot proceed with Ticket Granting Ticket(TGT) generation");
      throw new JSchException(
          "Failure: Invalid keytab file path. Cannot proceed with Ticket Granting Ticket(TGT) generation");
    }
    log.info("Generating Ticket Granting Ticket(TGT)...");
    logCallback.saveExecutionLog("Generating Ticket Granting Ticket(TGT) for principal: " + userPrincipal);
    String commandString = !StringUtils.isEmpty(password) ? format("echo \"%s\" | kinit %s", password, userPrincipal)
                                                          : format("kinit -k -t %s %s", keyTabFilePath, userPrincipal);
    boolean ticketGenerated = executeLocalCommand(commandString, logCallback, null, false);
    if (ticketGenerated) {
      logCallback.saveExecutionLog("Ticket Granting Ticket(TGT) generated successfully for " + userPrincipal);
      log.info("Ticket Granting Ticket(TGT) generated successfully for " + userPrincipal);
    } else {
      log.error("Failure: could not generate Ticket Granting Ticket(TGT)");
      throw new JSchException("Failure: could not generate Ticket Granting Ticket(TGT)");
    }
  }
  private static boolean isValidKeyTabFile(String keyTabFilePath) {
    if (!StringUtils.isEmpty(keyTabFilePath)) {
      if (new File(keyTabFilePath).exists()) {
        log.info("Found keytab file at path: [{}]", keyTabFilePath);
        return true;
      } else {
        log.error("Invalid keytab file path: [{}].", keyTabFilePath);
        return false;
      }
    }
    return true;
  }

  public static boolean executeLocalCommand(
      String cmdString, LogCallback logCallback, Writer output, boolean isOutputWriter) {
    String[] commandList = new String[] {"/bin/bash", "-c", cmdString};
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         ByteArrayOutputStream byteArrayErrorStream = new ByteArrayOutputStream()) {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .command(commandList)
                                            .directory(new File(System.getProperty("user.home")))
                                            .readOutput(true)
                                            .redirectOutput(byteArrayOutputStream)
                                            .redirectError(byteArrayErrorStream);

      ProcessResult processResult = null;
      try {
        processResult = processExecutor.execute();
      } catch (IOException | InterruptedException | TimeoutException e) {
        log.error("Failed to execute command ", e);
      }
      if (byteArrayOutputStream.toByteArray().length != 0) {
        if (isOutputWriter) {
          try {
            output.write(byteArrayOutputStream.toString());
          } catch (IOException e) {
            log.error("Failed to store the output to writer ", e);
          }
        } else {
          logCallback.saveExecutionLog(byteArrayOutputStream.toString(), LogLevel.INFO);
        }
      }
      if (byteArrayErrorStream.toByteArray().length != 0) {
        logCallback.saveExecutionLog(byteArrayErrorStream.toString(), ERROR);
      }
      return processResult != null && processResult.getExitValue() == 0;
    } catch (IOException e) {
      log.error("Failed to execute command ", e);
    }
    return false;
  }
}

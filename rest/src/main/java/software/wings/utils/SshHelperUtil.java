package software.wings.utils;

import static software.wings.beans.ErrorCodes.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCodes.INVALID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_KEYPATH;
import static software.wings.beans.ErrorCodes.INVALID_PORT;
import static software.wings.beans.ErrorCodes.SOCKET_CONNECTION_ERROR;
import static software.wings.beans.ErrorCodes.SOCKET_CONNECTION_TIMEOUT;
import static software.wings.beans.ErrorCodes.SSH_SESSION_TIMEOUT;
import static software.wings.beans.ErrorCodes.UNKNOWN_ERROR;
import static software.wings.beans.ErrorCodes.UNKNOWN_HOST;
import static software.wings.beans.ErrorCodes.UNREACHABLE_HOST;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SU_APP_USER;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD_AUTH;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import com.jcraft.jsch.JSchException;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.ErrorCodes;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.ssh.executors.SshSessionConfig.Builder;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by anubhaw on 2/23/17.
 */
public class SshHelperUtil {
  public static ExecutorType getExecutorType(CommandExecutionContext commandExecutionContext) {
    ExecutorType executorType;
    if (commandExecutionContext.getBastionConnectionAttributes() != null) {
      executorType = BASTION_HOST;
    } else {
      SettingAttribute settingAttribute = commandExecutionContext.getHostConnectionAttributes();
      HostConnectionAttributes hostConnectionAttributes = (HostConnectionAttributes) settingAttribute.getValue();
      AccessType accessType = hostConnectionAttributes.getAccessType();
      if (accessType.equals(AccessType.KEY) || accessType.equals(KEY_SU_APP_USER)
          || accessType.equals(KEY_SUDO_APP_USER)) {
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
  public static ErrorCodes normalizeError(JSchException jschexception) {
    String message = jschexception.getMessage();
    Throwable cause = jschexception.getCause();

    ErrorCodes errorConst = UNKNOWN_ERROR;

    if (cause != null) { // TODO: Refactor use enums, maybe ?
      if (cause instanceof NoRouteToHostException) {
        errorConst = UNREACHABLE_HOST;
      } else if (cause instanceof UnknownHostException) {
        errorConst = UNKNOWN_HOST;
      } else if (cause instanceof SocketTimeoutException) {
        errorConst = SOCKET_CONNECTION_TIMEOUT;
      } else if (cause instanceof ConnectException) {
        errorConst = INVALID_PORT;
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

  public static SshSessionConfig getSshSessionConfig(
      String hostName, String commandName, CommandExecutionContext context) {
    ExecutorType executorType = getExecutorType(context);

    SSHExecutionCredential sshExecutionCredential = (SSHExecutionCredential) context.getExecutionCredential();

    Builder builder = aSshSessionConfig()
                          .withAccountId(context.getAccountId())
                          .withAppId(context.getAppId())
                          .withExecutionId(context.getActivityId())
                          .withExecutorType(executorType)
                          .withHost(hostName)
                          .withCommandUnitName(commandName)
                          .withUserName(sshExecutionCredential.getSshUser())
                          .withPassword(sshExecutionCredential.getSshPassword())
                          .withSudoAppName(sshExecutionCredential.getAppAccount())
                          .withSudoAppPassword(sshExecutionCredential.getAppAccountPassword())
                          .withKeyPassphrase(sshExecutionCredential.getKeyPassphrase());

    if (executorType.equals(KEY_AUTH)) {
      SettingAttribute settingAttribute = context.getHostConnectionAttributes();
      HostConnectionAttributes hostConnectionAttributes = (HostConnectionAttributes) settingAttribute.getValue();
      builder.withKey(hostConnectionAttributes.getKey())
          .withUserName(hostConnectionAttributes.getUserName())
          .withKeyName(settingAttribute.getUuid())
          .withPassword(null);
    }

    if (context.getBastionConnectionAttributes() != null) {
      SettingAttribute settingAttribute = context.getBastionConnectionAttributes();
      BastionConnectionAttributes bastionAttrs = (BastionConnectionAttributes) settingAttribute.getValue();
      builder.withBastionHostConfig(aSshSessionConfig()
                                        .withHost(bastionAttrs.getHostName())
                                        .withKey(bastionAttrs.getKey())
                                        .withKeyName(settingAttribute.getUuid())
                                        .withUserName(bastionAttrs.getUserName())
                                        .build());
    }
    return builder.build();
  }
}

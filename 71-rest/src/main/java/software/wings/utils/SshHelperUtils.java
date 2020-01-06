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
import io.netty.channel.ConnectTimeoutException;
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

import java.io.FileNotFoundException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by anubhaw on 2/23/17.
 */
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

  private static void populateBuilderWithCredentials(
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
}

package software.wings.core.ssh.executors;

import static io.harness.eraro.ErrorCode.UNKNOWN_EXECUTOR_TYPE_ERROR;

import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY_SU_APP_USER;
import static software.wings.core.ssh.executors.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.ExecutorType.PASSWORD_AUTH;
import static software.wings.utils.SshHelperUtils.normalizeError;

import io.harness.exception.WingsException;

import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SshSessionManager {
  private static ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();

  /**
   * Gets cached session.
   *
   * @param config the config
   * @return the cached session
   */
  public static synchronized Session getCachedSession(SshSessionConfig config, DelegateLogService delegateLogService) {
    String key = config.getExecutionId() + "~" + config.getHost().trim();
    log.info("Fetch session for executionId : {}, hostName: {} ", config.getExecutionId(), config.getHost());

    Session cachedSession = sessions.computeIfAbsent(key, s -> {
      log.info("No session found. Create new session for executionId : {}, hostName: {}", config.getExecutionId(),
          config.getHost());
      return getSession(config, delegateLogService);
    });

    // Unnecessary but required test before session reuse.
    // test channel. http://stackoverflow.com/questions/16127200/jsch-how-to-keep-the-session-alive-and-up
    try {
      ChannelExec testChannel = (ChannelExec) cachedSession.openChannel("exec");
      testChannel.setCommand("true");
      testChannel.connect(config.getSocketConnectTimeout());
      testChannel.disconnect();
      log.info("Session connection test successful");
    } catch (Exception exception) {
      log.error("Session connection test failed. Reopen new session", exception);
      cachedSession =
          sessions.merge(key, cachedSession, (session1, session2) -> getSession(config, delegateLogService));
    }
    return cachedSession;
  }

  private static Session getSession(SshSessionConfig config, DelegateLogService delegateLogService) {
    return getSession(config,
        new ExecutionLogCallback(delegateLogService, config.getAccountId(), config.getAppId(), config.getExecutionId(),
            config.getCommandUnitName()));
  }

  private static Session getSession(SshSessionConfig config, ExecutionLogCallback executionLogCallback) {
    if (config.getExecutorType() == null) {
      if (config.getBastionHostConfig() != null) {
        config.setExecutorType(BASTION_HOST);
      } else {
        if (config.getAccessType() == HostConnectionAttributes.AccessType.KEY
            || config.getAccessType() == KEY_SU_APP_USER || config.getAccessType() == KEY_SUDO_APP_USER) {
          config.setExecutorType(KEY_AUTH);
        } else {
          config.setExecutorType(PASSWORD_AUTH);
        }
      }
    }

    try {
      switch (config.getExecutorType()) {
        case PASSWORD_AUTH:
        case KEY_AUTH:
          return SshSessionFactory.getSSHSession(config, executionLogCallback);
        case BASTION_HOST:
          return SshSessionFactory.getSSHSessionWithJumpbox(config, executionLogCallback);
        default:
          throw new WingsException(
              UNKNOWN_EXECUTOR_TYPE_ERROR, new Throwable("Unknown executor type: " + config.getExecutorType()));
      }
    } catch (JSchException jschEx) {
      throw new WingsException(normalizeError(jschEx), normalizeError(jschEx).name(), jschEx);
    }
  }

  /**
   * Evict and disconnect cached session.
   *
   * @param executionId the execution id
   * @param hostName    the host name
   */
  public static void evictAndDisconnectCachedSession(String executionId, String hostName) {
    log.info("Clean up session for executionId : {}, hostName: {} ", executionId, hostName);
    String key = executionId + "~" + hostName.trim();
    Session session = sessions.remove(key);
    if (session != null && session.isConnected()) {
      log.info("Found cached session. disconnecting the session");
      session.disconnect();
      log.info("Session disconnected successfully");
    } else {
      log.info("No cached session found for executionId : {}, hostName: {} ", executionId, hostName);
    }
  }
}

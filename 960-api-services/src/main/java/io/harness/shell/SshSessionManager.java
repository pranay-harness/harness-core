package io.harness.shell;

import static io.harness.eraro.ErrorCode.UNKNOWN_EXECUTOR_TYPE_ERROR;
import static io.harness.shell.AccessType.KEY_SUDO_APP_USER;
import static io.harness.shell.AccessType.KEY_SU_APP_USER;
import static io.harness.shell.ExecutorType.BASTION_HOST;
import static io.harness.shell.ExecutorType.KEY_AUTH;
import static io.harness.shell.ExecutorType.PASSWORD_AUTH;
import static io.harness.shell.SshHelperUtils.normalizeError;

import io.harness.exception.WingsException;
import io.harness.logging.LogCallback;
import io.harness.shell.AccessType;
import io.harness.shell.SshSessionConfig;
import io.harness.shell.SshSessionFactory;

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
   * @param logCallback
   * @return the cached session
   */
  public static synchronized Session getCachedSession(SshSessionConfig config, LogCallback logCallback) {
    String key = config.getExecutionId() + "~" + config.getHost().trim();
    log.info("Fetch session for executionId : {}, hostName: {} ", config.getExecutionId(), config.getHost());

    Session cachedSession = sessions.computeIfAbsent(key, s -> {
      log.info("No session found. Create new session for executionId : {}, hostName: {}", config.getExecutionId(),
          config.getHost());
      return getSession(config, logCallback);
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
      cachedSession = sessions.merge(key, cachedSession, (session1, session2) -> getSession(config, logCallback));
    }
    return cachedSession;
  }

  private static Session getSession(SshSessionConfig config, LogCallback executionLogCallback) {
    if (config.getExecutorType() == null) {
      if (config.getBastionHostConfig() != null) {
        config.setExecutorType(BASTION_HOST);
      } else {
        if (config.getAccessType() == AccessType.KEY || config.getAccessType() == KEY_SU_APP_USER
            || config.getAccessType() == KEY_SUDO_APP_USER) {
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

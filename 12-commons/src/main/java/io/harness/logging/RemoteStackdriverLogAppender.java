package io.harness.logging;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.unhandled;
import static io.harness.network.Http.connectableHttpUrl;
import static io.harness.network.Localhost.getLocalHostName;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Logging.WriteOption;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.LoggingOptions.Builder;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import com.google.cloud.logging.v2.LoggingSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.harness.version.VersionInfoManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public abstract class RemoteStackdriverLogAppender<E> extends AppenderBase<E> {
  private static final int MAX_BATCH_SIZE = 1000;
  private static final String SEVERITY = "severity";
  private static final String LOG_NAME = "delegate";
  private static final String LOG_PROXY_HOST = "logs.harness.io:443";

  private static Logging logging;

  private AtomicBoolean useLogProxy;
  private String localhostName = "localhost";
  private CustomJsonLayout layout;
  private BlockingQueue<LogEntry> logQueue;
  private Map<String, String> logLabels;
  private final LogLines logLines = new LogLines();
  private final ExecutorService appenderPool = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("remote-stackdriver-log-appender").build());
  private final VersionInfoManager versionInfoManager = new VersionInfoManager();
  private final String processId =
      Splitter.on("@").split(ManagementFactory.getRuntimeMXBean().getName()).iterator().next();
  private final AtomicInteger failedAttempts = new AtomicInteger(0);
  private final AtomicLong nextAttempt = new AtomicLong(0);

  static boolean loggingInitialized() {
    return logging != null;
  }

  @Override
  public void start() {
    synchronized (this) {
      if (started) {
        return;
      }

      layout = new CustomJsonLayout();
      super.start();
      localhostName = getLocalHostName();
      logQueue = Queues.newLinkedBlockingQueue(500000);
      Executors
          .newSingleThreadScheduledExecutor(
              new ThreadFactoryBuilder().setNameFormat("remote-stackdriver-log-submitter").build())
          .scheduleWithFixedDelay(this ::submitLogs, 1L, 1L, TimeUnit.SECONDS);
    }
  }

  @Override
  protected void append(E eventObject) {
    if (eventObject instanceof ILoggingEvent) {
      appenderPool.submit(() -> {
        try {
          synchronized (this) {
            Map<String, ?> jsonMap = layout.toJsonMap((ILoggingEvent) eventObject);
            Level logLevel = Level.valueOf((String) jsonMap.remove(SEVERITY));
            if (!logQueue.offer(
                    LogEntry.newBuilder(JsonPayload.of(jsonMap)).setSeverity(logLevelToSeverity(logLevel)).build())) {
              logQueue.clear();
              logger.error("No space left in log queue. Cleared.");
            }
          }
        } catch (Exception ex) {
          logger.error("Error appending log entry", ex);
        }
      });
    }
  }

  @VisibleForTesting
  public static Severity logLevelToSeverity(Level logLevel) {
    switch (logLevel.toInt()) {
      case Level.TRACE_INT:
      case Level.DEBUG_INT:
        return Severity.DEBUG;
      case Level.INFO_INT:
        return Severity.INFO;
      case Level.WARN_INT:
        return Severity.WARNING;
      case Level.ERROR_INT:
        return Severity.ERROR;
      default:
        unhandled(logLevel.toString());
        return Severity.INFO;
    }
  }

  private void submitLogs() {
    synchronized (this) {
      if (logQueue.isEmpty()) {
        return;
      }

      if (System.currentTimeMillis() < nextAttempt.get()) {
        return;
      }

      ensureLoggingInitialized();
      if (logging == null) {
        int attempts = failedAttempts.incrementAndGet();
        if (attempts >= 50 && attempts % 10 == 0) {
          logQueue.clear();
          logger.error("Failed to initialize logging after {} attempts. Cleared log queue.", attempts);
        }
        // Exponential backoff. Delay by 2 ^ (attempts / 2) seconds, max 60. (12 attempts to reach the max)
        long delayMillis = (long) (Math.min(Math.pow(2, (double) attempts / 2d), 60d) * 1000d);
        nextAttempt.set(System.currentTimeMillis() + delayMillis);
        return;
      }

      failedAttempts.set(0);
      nextAttempt.set(0);

      try {
        if (logQueue.size() > MAX_BATCH_SIZE) {
          logLines.add(LogEntry
                           .newBuilder(JsonPayload.of(ImmutableMap.of("message",
                               "Log queue exceeds max batch size (" + MAX_BATCH_SIZE
                                   + "). Current queue size: " + logQueue.size())))
                           .setSeverity(Severity.WARNING)
                           .build());
        }
        logQueue.drainTo(logLines.getLines(), MAX_BATCH_SIZE);
        logging.write(logLines.getLines(), WriteOption.logName(LOG_NAME),
            WriteOption.resource(MonitoredResource.newBuilder("global").build()), WriteOption.labels(getLogLabels()));
      } catch (Exception ex) {
        logger.error("Failed to submit logs.", ex);
      } finally {
        logLines.clear();
      }
    }
  }

  private void ensureLoggingInitialized() {
    if (logging != null) {
      Date nineMinutesFromNow = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(9));
      GoogleCredentials credentials = (GoogleCredentials) logging.getOptions().getCredentials();
      Date expirationTime = credentials.getAccessToken().getExpirationTime();
      if (expirationTime.before(nineMinutesFromNow)) {
        logger.info("Logging token expires {}. Refreshing.", expirationTime);
        try {
          logging.close();
        } catch (Exception e) {
          logger.error("Error closing logging", e);
        }
        logging = null;
      } else {
        return;
      }
    }

    AccessTokenBean accessTokenBean = getLoggingToken();
    if (accessTokenBean == null) {
      return;
    }

    if (useLogProxy == null) {
      boolean cannotConnectStackdriver = !connectableHttpUrl("https://" + LoggingSettings.getDefaultEndpoint());
      if (cannotConnectStackdriver) {
        return;
        // TODO (brett) - Enable log proxy check after verified working with grpc
        //        if (!connectableHttpUrl("https://" + LOG_PROXY_HOST)) {
        //          return;
        //        }
      }
      useLogProxy = new AtomicBoolean(cannotConnectStackdriver);
    }

    Builder loggingOptionsBuilder =
        LoggingOptions.newBuilder()
            .setProjectId(accessTokenBean.getProjectId())
            .setCredentials(GoogleCredentials.create(
                new AccessToken(accessTokenBean.getTokenValue(), new Date(accessTokenBean.getExpirationTimeMillis()))));

    if (useLogProxy.get()) {
      loggingOptionsBuilder.setHost(LOG_PROXY_HOST);
    }

    try {
      logging = loggingOptionsBuilder.build().getService();
    } catch (Exception e) {
      logger.error("Failed to build Logging client for StackdriverLogging", e);
    }
  }

  private Map<String, String> getLogLabels() {
    String delegateId = getDelegateId();
    if (isEmpty(logLabels) || !StringUtils.equals(delegateId, logLabels.get("delegateId"))) {
      ImmutableMap.Builder<String, String> labelsBuilder =
          ImmutableMap.<String, String>builder()
              .put("source", localhostName)
              .put("processId", processId)
              .put("version", versionInfoManager.getVersionInfo().getVersion())
              .put("app", getAppName())
              .put("accountId", getAccountId())
              .put("managerHost", getManagerHost());
      if (isNotBlank(delegateId)) {
        labelsBuilder.put("delegateId", delegateId);
      }
      logLabels = labelsBuilder.build();
    }
    return logLabels;
  }

  protected abstract String getAppName();
  protected abstract String getAccountId();
  protected abstract String getManagerHost();
  protected abstract String getDelegateId();
  protected abstract AccessTokenBean getLoggingToken();

  @VisibleForTesting
  public BlockingQueue<LogEntry> getLogQueue() {
    return logQueue;
  }

  public static class LogLines {
    private final List<LogEntry> lines = new ArrayList<>(MAX_BATCH_SIZE + 1);

    public List<LogEntry> getLines() {
      return lines;
    }

    public void add(LogEntry logLine) {
      lines.add(logLine);
    }

    public void clear() {
      lines.clear();
    }
  }
}

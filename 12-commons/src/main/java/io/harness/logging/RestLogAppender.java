package io.harness.logging;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.Localhost.getLocalHostName;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import com.github.fge.jsonschema.report.LogLevel;
import io.harness.flow.Flow;
import io.harness.network.Http;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RestLogAppender<E> extends AppenderBase<E> {
  private static final int MAX_BATCH_SIZE = 1000;
  private static final String LOGDNA_HOST_DIRECT = "https://logs.logdna.com";
  private static final String LOGDNA_HOST_PROXY = "https://app.harness.io/storage/applogs/";
  private static final String DUMMY_KEY = "9a3e6eac4dcdbdc41a93ca99100537df";

  private LogdnaRestClient logdnaRestClient;
  private LogLines logLines = new LogLines();
  private String baseUrl;
  private String programName;
  private String key;
  private String localhostName = "localhost";
  private Layout<E> layout;
  private BlockingQueue<LogLine> logQueue;
  private ExecutorService appenderPool =
      Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("rest-log-appender").build());

  /**
   * Instantiates a new Rest log appender.
   */
  public RestLogAppender() {}

  /**
   * Instantiates a new Rest log appender.
   *
   * @param programName the program name
   * @param key         the key
   */
  public RestLogAppender(String programName, String key) {
    this.programName = programName;
    this.key = key;
  }

  private void initializeRetrofit() {
    try {
      this.logdnaRestClient = createLogdnaRestClient(LOGDNA_HOST_DIRECT);
      baseUrl = LOGDNA_HOST_DIRECT;
    } catch (Exception e) {
      logger.info("Failed to connect directly to logdna", e);
      try {
        logdnaRestClient = createLogdnaRestClient(LOGDNA_HOST_PROXY);
        baseUrl = LOGDNA_HOST_PROXY;
      } catch (Exception ex) {
        logger.warn("Failed to connect via proxy to logdna", ex);
      }
    }
  }

  private LogdnaRestClient createLogdnaRestClient(String baseUrl) throws Exception {
    LogdnaRestClient restClient = new Builder()
                                      .baseUrl(baseUrl)
                                      .addConverterFactory(JacksonConverterFactory.create())
                                      .client(Http.getUnsafeOkHttpClient(baseUrl))
                                      .build()
                                      .create(LogdnaRestClient.class);
    logger.info("Initializing logdna");
    LogLines lines = new LogLines();
    lines.add(new LogLine("Init logdna using " + baseUrl, Level.INFO.toString(), programName));

    new SimpleTimeLimiter().callWithTimeout(() -> {
      Flow.retry(3, ofSeconds(1), () -> restClient.postLogs(getAuthHeader(), localhostName, lines).execute());
      return true;
    }, 5, TimeUnit.SECONDS, true);
    logger.info("Connection to logdna succeeded using connection: {}", baseUrl);
    return restClient;
  }

  private void submitLogs() {
    if (stackdriverSuccessful()) {
      return;
    }

    try {
      if (!logQueue.isEmpty()) {
        if (logQueue.size() > MAX_BATCH_SIZE) {
          logLines.add(new LogLine(
              "Log queue exceeds max batch size (" + MAX_BATCH_SIZE + "). Current queue size: " + logQueue.size(),
              LogLevel.WARNING.toString(), logQueue.peek().getApp()));
        }
        logQueue.drainTo(logLines.getLines(), MAX_BATCH_SIZE);
        // Do not retry here to post logs as if the request fails logs keep piling up.
        logdnaRestClient.postLogs(getAuthHeader(), localhostName, logLines).execute();
      }
    } catch (Exception ex) {
      logger.warn("Failed to submit logs to {}. Requeuing.", baseUrl, ex);
      logQueue.addAll(logLines.getLines());
    } finally {
      logLines.clear();
    }
  }

  private String getAuthHeader() {
    return "Basic " + encodeBase64String(format("%s:%s", key, "").getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void append(E eventObject) {
    if (stackdriverSuccessful()) {
      return;
    }

    appenderPool.submit(() -> {
      try {
        String logLevel = Level.INFO.toString();
        String message = layout.doLayout(eventObject);
        if (eventObject instanceof ILoggingEvent) {
          ILoggingEvent event = (ILoggingEvent) eventObject;
          logLevel = event.getLevel().toString();
        }
        LogLine logLine = new LogLine(message, logLevel, programName);
        logQueue.offer(logLine);
      } catch (Exception ex) {
        logger.error("", ex);
      }
    });
  }

  @Override
  public void start() {
    if (isEmpty(key) || key.equals(DUMMY_KEY)) {
      logger.info("Not starting RestLogAppender since RestLogAppender is disabled");
      return;
    }

    initializeRetrofit();

    if (isBlank(baseUrl)) {
      logger.warn("Not starting RestLogAppender since logdna could not be reached");
      return;
    }

    synchronized (this) {
      if (!started) {
        super.start();
        localhostName = getLocalHostName();
        logQueue = Queues.newLinkedBlockingQueue(500000);
        Executors
            .newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("rest-log-submitter").build())
            .scheduleAtFixedRate(this ::submitLogs, 1000, 1000, TimeUnit.MILLISECONDS);
      }
    }
  }

  private boolean stackdriverSuccessful() {
    return RemoteStackdriverLogAppender.loggingInitialized();
  }

  public String getProgramName() {
    return programName;
  }
  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }

  public Layout<E> getLayout() {
    return layout;
  }
  public void setLayout(Layout<E> layout) {
    this.layout = layout;
  }

  public static class LogLines {
    private final List<LogLine> lines = new ArrayList<>(MAX_BATCH_SIZE + 1);

    public List<LogLine> getLines() {
      return lines;
    }

    public void add(LogLine logLine) {
      lines.add(logLine);
    }

    public void clear() {
      lines.clear();
    }
  }

  @Value
  @AllArgsConstructor
  private static class LogLine {
    private String line;
    private String level;
    private String app;
  }
}

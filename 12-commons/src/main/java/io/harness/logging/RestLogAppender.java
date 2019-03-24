package io.harness.logging;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.Localhost.getLocalHostName;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.SimpleTimeLimiter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import io.harness.flow.Flow;
import io.harness.network.Http;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RestLogAppender<E> extends AppenderBase<E> {
  private static final Logger logger = LoggerFactory.getLogger(RestLogAppender.class);

  private static final int MAX_BATCH_SIZE = 1000;
  private static final String LOGDNA_HOST_DIRECT = "https://logs.logdna.com";
  private static final String LOGDNA_HOST_PROXY = "https://app.harness.io/storage/applogs/";
  private static final String DUMMY_KEY = "9a3e6eac4dcdbdc41a93ca99100537df";

  private Retrofit retrofit;
  private String programName;
  private String key;
  private String localhostName = "localhost";
  private Layout<E> layout;
  private Queue<LogLine> logQueue; // don't call size(), it runs in linear time
  private ExecutorService appenderPool = Executors.newSingleThreadScheduledExecutor();

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
    retrofit = new Retrofit.Builder()
                   .baseUrl(LOGDNA_HOST_DIRECT)
                   .addConverterFactory(JacksonConverterFactory.create())
                   .client(Http.getUnsafeOkHttpClient(LOGDNA_HOST_DIRECT))
                   .build();
    try {
      LogLines logLines = new LogLines();
      logLines.add(new LogLine("Init. Using " + LOGDNA_HOST_DIRECT, Level.INFO.toString(), programName));

      new SimpleTimeLimiter().callWithTimeout(() -> {
        Flow.retry(3, ofSeconds(1),
            () -> retrofit.create(LogdnaRestClient.class).postLogs(getAuthHeader(), localhostName, logLines).execute());
        return true;
      }, 5, TimeUnit.SECONDS, true);
    } catch (Exception e) {
      retrofit = new Retrofit.Builder()
                     .baseUrl(LOGDNA_HOST_PROXY)
                     .addConverterFactory(JacksonConverterFactory.create())
                     .client(Http.getUnsafeOkHttpClient(LOGDNA_HOST_PROXY))
                     .build();
    }
  }

  private void submitLogs() {
    try {
      int batchSize = 0;
      LogLines logLines = new LogLines();
      while (!logQueue.isEmpty() && batchSize < MAX_BATCH_SIZE) {
        LogLine logLine = logQueue.poll();
        if (logLine == null) { // no more element in the queue. break from loop
          break;
        }
        logLines.add(logLine);
        batchSize++; // increment unconditionally to break the loop
      }

      if (logLines.isEmpty()) {
        return;
      }

      Flow.retry(10, ofSeconds(3),
          () -> retrofit.create(LogdnaRestClient.class).postLogs(getAuthHeader(), localhostName, logLines).execute());
    } catch (Exception ex) {
      logger.error("Failed to submit logs after 10 tries to {}", retrofit.baseUrl(), ex);
    }
  }

  private String getAuthHeader() {
    return "Basic " + encodeBase64String(format("%s:%s", key, "").getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void append(E eventObject) {
    appenderPool.submit(() -> {
      try {
        String logLevel = Level.INFO.toString();
        String message = layout.doLayout(eventObject);
        if (eventObject instanceof ILoggingEvent) {
          ILoggingEvent event = (ILoggingEvent) eventObject;
          logLevel = event.getLevel().toString();
        }
        LogLine logLine = new LogLine(message, logLevel, programName);
        logQueue.add(logLine);
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

    super.start();

    synchronized (this) {
      localhostName = getLocalHostName();
      logQueue = Queues.newConcurrentLinkedQueue();
      Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
          this ::submitLogs, 1000, 1000, TimeUnit.MILLISECONDS);
    }
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
    List<LogLine> lines = new ArrayList<>();

    public List<LogLine> getLines() {
      return lines;
    }

    public void setLines(List<LogLine> lines) {
      this.lines = lines;
    }

    public void add(LogLine logLine) {
      if (logLine != null) {
        lines.add(logLine);
      }
    }

    public int size() {
      return lines.size();
    }

    public boolean isEmpty() {
      return lines.isEmpty();
    }
  }

  @Value
  @AllArgsConstructor
  public static class LogLine {
    private String line;
    private String level;
    private String app;
  }
}

package software.wings.logging;

import static io.harness.data.network.NetworkUtil.getLocalHostName;
import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Queues;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import com.fasterxml.jackson.databind.JsonNode;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The type Rest log appender.
 *
 * @param <E> the type parameter
 */
public class RestLogAppender<E> extends AppenderBase<E> {
  private static final int MAX_BATCH_SIZE = 1000;
  private static final String LOGDNA_INGEST_URL = "https://logs.logdna.com/logs/ingest?hostname=%s&now=:now";
  public static final String LOGDNA_HOST = "https://logs.logdna.com";
  private String programName;
  private String key;
  private String localhostName = "localhost";
  private Layout<E> layout;
  private ConcurrentLinkedQueue<LogLine> logQueue; // don't call size(), it runs in linear time
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

      Retrofit retrofit = new Retrofit.Builder()
                              .baseUrl(LOGDNA_HOST)
                              .addConverterFactory(JacksonConverterFactory.create())
                              .client(HttpUtil.getUnsafeOkHttpClient(LOGDNA_HOST))
                              .build();
      Response<JsonNode> execute =
          retrofit.create(LogdnaRestClient.class).postLogs(getAuthHeader(), localhostName, logLines).execute();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private String getAuthHeader() {
    return "Basic " + encodeBase64String(format("%s:%s", key, "").getBytes());
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
        ex.printStackTrace();
      }
    });
  }

  @Override
  public void start() {
    super.start();

    synchronized (this) {
      localhostName = getLocalHostName();
      logQueue = Queues.newConcurrentLinkedQueue();
      Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
          this ::submitLogs, 1000, 1000, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Gets program name.
   *
   * @return the program name
   */
  public String getProgramName() {
    return programName;
  }

  /**
   * Sets program name.
   *
   * @param programName the program name
   */
  public void setProgramName(String programName) {
    this.programName = programName;
  }

  /**
   * Gets key.
   *
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets key.
   *
   * @param key the key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Gets layout.
   *
   * @return the layout
   */
  public Layout<E> getLayout() {
    return layout;
  }

  /**
   * Sets layout.
   *
   * @param layout the layout
   */
  public void setLayout(Layout<E> layout) {
    this.layout = layout;
  }

  /**
   * The type Log lines.
   */
  public static class LogLines {
    /**
     * The Lines.
     */
    List<LogLine> lines = new ArrayList<>();

    /**
     * Gets lines.
     *
     * @return the lines
     */
    public List<LogLine> getLines() {
      return lines;
    }

    /**
     * Sets lines.
     *
     * @param lines the lines
     */
    public void setLines(List<LogLine> lines) {
      this.lines = lines;
    }

    /**
     * Add.
     *
     * @param logLine the log line
     */
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

  /**
   * The type Log line.
   */
  public static class LogLine {
    private String line;
    private String app;
    private String level;

    /**
     * Instantiates a new Log line.
     *
     * @param line  the line
     * @param level the level
     * @param app   the app
     */
    public LogLine(String line, String level, String app) {
      this.line = line;
      this.app = app;
      this.level = level;
    }

    /**
     * Gets line.
     *
     * @return the line
     */
    public String getLine() {
      return line;
    }

    /**
     * Sets line.
     *
     * @param line the line
     */
    public void setLine(String line) {
      this.line = line;
    }

    /**
     * Gets app.
     *
     * @return the app
     */
    public String getApp() {
      return app;
    }

    /**
     * Sets app.
     *
     * @param app the app
     */
    public void setApp(String app) {
      this.app = app;
    }

    /**
     * Gets level.
     *
     * @return the level
     */
    public String getLevel() {
      return level;
    }

    /**
     * Sets level.
     *
     * @param level the level
     */
    public void setLevel(String level) {
      this.level = level;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("line", line).add("app", app).add("level", level).toString();
    }
  }
}

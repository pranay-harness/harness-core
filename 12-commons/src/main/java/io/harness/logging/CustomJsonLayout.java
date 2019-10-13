package io.harness.logging;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter;
import ch.qos.logback.contrib.json.JsonLayoutBase;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * See {@link ch.qos.logback.contrib.json.classic.JsonLayout}
 */
@Slf4j
public class CustomJsonLayout extends JsonLayoutBase<ILoggingEvent> {
  private static final String TIMESTAMP = "timestamp";
  private static final String SEVERITY = "severity";
  private static final String VERSION_ENV_VAR = "VERSION";
  private static final String VERSION = "version";
  private static final String THREAD = "thread";
  private static final String LOGGER = "logger";
  private static final String MESSAGE = "message";
  private static final String HARNESS = "harness";
  private static final String EXCEPTION = "exception";

  private final ThrowableHandlingConverter throwableProxyConverter;

  CustomJsonLayout(LoggerContext context) {
    this.context = context;
    timestampFormat = "yyyy-MM-dd HH:mm:ss.SSS Z";
    appendLineSeparator = true;
    jsonFormatter = new JacksonJsonFormatter();
    throwableProxyConverter = new StackTraceProxyConverter();
  }

  @Override
  public void start() {
    this.throwableProxyConverter.start();
    super.start();
  }

  @Override
  public void stop() {
    super.stop();
    this.throwableProxyConverter.stop();
  }

  @Override
  protected Map toJsonMap(ILoggingEvent event) {
    Map<String, Object> map = new HashMap<>();

    final String formattedMessage = event.getFormattedMessage();

    addTimestamp(TIMESTAMP, true, event.getTimeStamp(), map);
    add(SEVERITY, true, String.valueOf(event.getLevel()), map);
    add(VERSION, true, System.getenv(VERSION_ENV_VAR), map);
    add(THREAD, true, event.getThreadName(), map);
    add(LOGGER, true, event.getLoggerName(), map);
    add(MESSAGE, true, formattedMessage, map);
    addMap(HARNESS, true, event.getMDCPropertyMap(), map);
    addThrowableInfo(event, map);

    if (logger.isDebugEnabled() && event.getLevel().toInt() >= Level.INFO.toInt()) {
      for (Entry<String, String> entry : event.getMDCPropertyMap().entrySet()) {
        if (formattedMessage.contains(entry.getValue())) {
          logger.debug("Logging message '{}' incorporates variable {} that is already in the MDC table with key {}",
              event.getMessage(), entry.getValue(), entry.getKey());
        }
      }
    }

    return map;
  }

  private void addThrowableInfo(ILoggingEvent value, Map<String, Object> map) {
    if (value != null && value.getThrowableProxy() != null) {
      String ex = throwableProxyConverter.convert(value);
      if (isNotBlank(ex)) {
        map.put(EXCEPTION, ex);
      }
    }
  }
}

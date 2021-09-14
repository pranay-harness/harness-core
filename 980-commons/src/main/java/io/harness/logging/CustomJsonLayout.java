/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter;
import ch.qos.logback.contrib.json.JsonLayoutBase;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;

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

  // Constructor needs to be public for initialization from outside of this package
  // from logback.xml configurations
  @SuppressWarnings("WeakerAccess")
  public CustomJsonLayout() {
    this(null);
  }

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
  protected Map<String, Object> toJsonMap(ILoggingEvent event) {
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

    if (log.isDebugEnabled() && event.getLevel().toInt() >= Level.INFO.toInt()) {
      for (Entry<String, String> entry : event.getMDCPropertyMap().entrySet()) {
        if (formattedMessage.contains(entry.getValue())) {
          try (AutoLogContext ignore = new MessagePatternLogContext(event.getMessage(), OVERRIDE_ERROR);
               AutoLogContext ignore2 = new MdcKeyLogContext(entry.getKey(), OVERRIDE_ERROR)) {
            log.debug("MDC table and the logging message have the same value {}", entry.getValue());
          }
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

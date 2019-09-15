package io.harness.logging;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.helpers.NOPAppender;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * {@link ConsoleAppenderFactory} extension which uses a Stackdriver compatible {@link JsonLayout}
 *
 * It outputs a json per line Stackdriver [LogEntry](https://cloud.google.com/logging/docs/view/logs_index)
 *
 */
@JsonTypeName("gke-console")
@Data
@EqualsAndHashCode(callSuper = false)
public class GkeConsoleAppenderFactory<E extends DeferredProcessingAware> extends ConsoleAppenderFactory<E> {
  private static final String SEVERITY = "severity";
  private static final String VERSION = "version";
  private static final String HARNESS = "harness";
  private boolean stackdriverLogEnabled;

  @Override
  public Appender<E> build(LoggerContext context, String applicationName, LayoutFactory<E> layoutFactory,
      LevelFilterFactory<E> levelFilterFactory, AsyncAppenderFactory<E> asyncAppenderFactory) {
    Appender<E> appender;
    if (!stackdriverLogEnabled) {
      appender = new NOPAppender<>();
    } else {
      appender = new ConsoleAppender<>();

      ConsoleAppender<E> consoleAppender = (ConsoleAppender<E>) appender;
      consoleAppender.setName("gke-console");
      consoleAppender.setContext(context);
      consoleAppender.setTarget(getTarget().get());

      final LayoutWrappingEncoder<E> layoutEncoder = new LayoutWrappingEncoder<>();
      layoutEncoder.setLayout(initJsonLayout(context));

      consoleAppender.setEncoder(layoutEncoder);
      consoleAppender.addFilter(levelFilterFactory.build(getThreshold()));

      getFilterFactories().forEach(f -> consoleAppender.addFilter(f.build()));
    }
    appender.start();

    return wrapAsync(appender, asyncAppenderFactory);
  }

  private Layout<E> initJsonLayout(LoggerContext context) {
    JsonLayout jsonLayout = new JsonLayout() {
      @Override
      protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
        map.put(SEVERITY, String.valueOf(event.getLevel()));
      }

      @Override
      protected Map toJsonMap(ILoggingEvent event) {
        Map<String, Object> jsonMap = super.toJsonMap(event);
        Map<String, String> mdc = (Map<String, String>) jsonMap.remove(MDC_ATTR_NAME);
        if (isNotEmpty(mdc)) {
          jsonMap.put(HARNESS, mdc);
        }
        jsonMap.put(VERSION, System.getenv("VERSION"));
        return jsonMap;
      }
    };

    jsonLayout.setContext(context);

    jsonLayout.setIncludeLevel(false);
    jsonLayout.setIncludeContextName(false);
    jsonLayout.setIncludeTimestamp(false);

    jsonLayout.setAppendLineSeparator(true);
    jsonLayout.setJsonFormatter(new JacksonJsonFormatter());
    jsonLayout.setThrowableProxyConverter(new StackTraceProxyConverter());

    return (Layout<E>) jsonLayout;
  }
}

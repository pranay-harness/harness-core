package io.harness.logging;

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
 *
 */
@JsonTypeName("gke-console")
@Data
@EqualsAndHashCode(callSuper = false)
public class GkeConsoleAppenderFactory<E extends DeferredProcessingAware> extends ConsoleAppenderFactory<E> {
  private boolean stackdriverLogEnabled;
  @Override
  public Appender<E> build(LoggerContext context, String applicationName, LayoutFactory<E> layoutFactory,
      LevelFilterFactory<E> levelFilterFactory, AsyncAppenderFactory<E> asyncAppenderFactory) {
    Appender<E> appender;
    if (!stackdriverLogEnabled) {
      appender = new NOPAppender();
    } else {
      appender = new ConsoleAppender<>();

      ConsoleAppender<E> consoleAppender = (ConsoleAppender<E>) appender;
      consoleAppender.setName("gke-console");
      consoleAppender.setContext(context);
      consoleAppender.setTarget(getTarget().get());

      final LayoutWrappingEncoder<E> layoutEncoder = new LayoutWrappingEncoder<>();
      JsonLayout jsonLayout = new StackdriverLoggingJsonLayout(context);
      layoutEncoder.setLayout((Layout<E>) jsonLayout);

      consoleAppender.setEncoder(layoutEncoder);
      consoleAppender.addFilter(levelFilterFactory.build(getThreshold()));

      getFilterFactories().forEach(f -> consoleAppender.addFilter(f.build()));
    }
    appender.start();

    return wrapAsync(appender, asyncAppenderFactory);
  }

  private static class StackdriverLoggingJsonLayout extends JsonLayout {
    StackdriverLoggingJsonLayout(LoggerContext context) {
      this.includeLevel = false;
      this.setAppendLineSeparator(true);
      this.setJsonFormatter(new JacksonJsonFormatter());
      this.setContext(context);
    }

    @Override
    protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
      map.put("severity", String.valueOf(event.getLevel()));
    }
  }
}

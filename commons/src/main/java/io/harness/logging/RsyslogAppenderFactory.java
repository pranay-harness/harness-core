package io.harness.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;

import javax.validation.constraints.NotNull;

@JsonTypeName("rest-log")
public class RsyslogAppenderFactory<E extends DeferredProcessingAware> extends AbstractAppenderFactory<E> {
  @NotNull private String name = "rest-log-appender";

  @NotNull private String programName;

  @NotNull private String key;

  @NotNull private int maxMessageLength = 128000;

  @JsonProperty
  public String getName() {
    return name;
  }

  @JsonProperty
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty
  public String getProgramName() {
    return programName;
  }

  @JsonProperty
  public void setProgramName(String programName) {
    this.programName = programName;
  }

  @JsonProperty
  public int getMaxMessageLength() {
    return maxMessageLength;
  }

  @JsonProperty
  public void setMaxMessageLength(int maxMessageLength) {
    this.maxMessageLength = maxMessageLength;
  }

  @JsonProperty
  public String getKey() {
    return key;
  }

  @JsonProperty
  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public Appender<E> build(LoggerContext context, String applicationName, LayoutFactory<E> layoutFactory,
      LevelFilterFactory<E> levelFilterFactory, AsyncAppenderFactory<E> asyncAppenderFactory) {
    RestLogAppender<E> appender = new RestLogAppender<>(programName, key);
    appender.setName(name);
    appender.setContext(context);
    appender.setLayout(buildLayout(context, layoutFactory));
    appender.addFilter(levelFilterFactory.build(threshold));
    getFilterFactories().stream().forEach(f -> appender.addFilter(f.build()));

    appender.start();

    return wrapAsync(appender, asyncAppenderFactory);
  }
}

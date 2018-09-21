package io.harness.logging;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.helpers.NOPAppender;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.validation.constraints.NotNull;

@JsonTypeName("rest-log")
public class RsyslogAppenderFactory<E extends DeferredProcessingAware> extends AbstractAppenderFactory<E> {
  private static final Log log = LogFactory.getLog(RsyslogAppenderFactory.class);

  @NotNull private String name = "rest-log-appender";

  @NotNull private String programName;

  private String key;

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
    AppenderBase<E> appender;
    if (isEmpty(key)) {
      appender = new NOPAppender();
      log.info("Using NOPAppender as LogAppender, log streaming is DISABLED");
    } else {
      appender = new RestLogAppender<>(programName, key);
      RestLogAppender restLogAppender = (RestLogAppender) appender;
      restLogAppender.setName(name);
      restLogAppender.setContext(context);
      restLogAppender.setLayout(buildLayout(context, layoutFactory));
      restLogAppender.addFilter(levelFilterFactory.build(threshold));
      getFilterFactories().forEach(f -> restLogAppender.addFilter(f.build()));
      log.info("Using RestLogAppender as LogAppender, log streaming is ENABLED");
    }

    appender.start();

    return wrapAsync(appender, asyncAppenderFactory);
  }
}

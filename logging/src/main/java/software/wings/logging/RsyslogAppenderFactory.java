package software.wings.logging;

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

/**
 * Created by peeyushaggarwal on 12/6/16.
 */
@JsonTypeName("rest-log")
public class RsyslogAppenderFactory<E extends DeferredProcessingAware> extends AbstractAppenderFactory<E> {
  @NotNull private String name = "rest-log-appender";

  @NotNull private String programName;

  @NotNull private String key;

  @NotNull private int maxMessageLength = 128000;

  /**
   * Getter for property 'name'.
   *
   * @return Value for property 'name'.
   */
  @JsonProperty
  public String getName() {
    return name;
  }

  /**
   * Setter for property 'name'.
   *
   * @param name Value to set for property 'name'.
   */
  @JsonProperty
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Getter for property 'programName'.
   *
   * @return Value for property 'programName'.
   */
  @JsonProperty
  public String getProgramName() {
    return programName;
  }

  /**
   * Setter for property 'programName'.
   *
   * @param programName Value to set for property 'programName'.
   */
  @JsonProperty
  public void setProgramName(String programName) {
    this.programName = programName;
  }

  /**
   * Getter for property 'maxMessageLength'.
   *
   * @return Value for property 'maxMessageLength'.
   */
  @JsonProperty
  public int getMaxMessageLength() {
    return maxMessageLength;
  }

  /**
   * Setter for property 'maxMessageLength'.
   *
   * @param maxMessageLength Value to set for property 'maxMessageLength'.
   */
  @JsonProperty
  public void setMaxMessageLength(int maxMessageLength) {
    this.maxMessageLength = maxMessageLength;
  }

  /**
   * Getter for property 'key'.
   *
   * @return Value for property 'key'.
   */
  @JsonProperty
  public String getKey() {
    return key;
  }

  /**
   * Setter for property 'key'.
   *
   * @param key Value to set for property 'key'.
   */
  @JsonProperty
  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public Appender<E> build(LoggerContext context, String applicationName, LayoutFactory<E> layoutFactory,
      LevelFilterFactory<E> levelFilterFactory, AsyncAppenderFactory<E> asyncAppenderFactory) {
    //   CloudBeesSyslogAppender<E> appender = new CloudBeesSyslogAppender<E>(programName, key, host, port);
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

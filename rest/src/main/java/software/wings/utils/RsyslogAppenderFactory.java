package software.wings.utils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.papertrailapp.logback.Syslog4jAppender;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;
import org.productivity.java.syslog4j.impl.net.tcp.ssl.SSLTCPNetSyslogConfig;

import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 12/6/16.
 */
@JsonTypeName("rsyslog-tcp-ssl")
public class RsyslogAppenderFactory<E extends DeferredProcessingAware> extends AbstractAppenderFactory<E> {
  @NotNull private String name = "rsyslog-tcp-ssl-appender";

  @NotNull private String host;

  @NotNull private int port;

  @NotNull private String programName;

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
   * Getter for property 'host'.
   *
   * @return Value for property 'host'.
   */
  @JsonProperty
  public String getHost() {
    return host;
  }

  /**
   * Setter for property 'host'.
   *
   * @param host Value to set for property 'host'.
   */
  @JsonProperty
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Getter for property 'port'.
   *
   * @return Value for property 'port'.
   */
  @JsonProperty
  public int getPort() {
    return port;
  }

  /**
   * Setter for property 'port'.
   *
   * @param port Value to set for property 'port'.
   */
  @JsonProperty
  public void setPort(int port) {
    this.port = port;
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

  @Override
  public Appender<E> build(LoggerContext context, String applicationName, LayoutFactory<E> layoutFactory,
      LevelFilterFactory<E> levelFilterFactory, AsyncAppenderFactory<E> asyncAppenderFactory) {
    Syslog4jAppender appender = new Syslog4jAppender();
    appender.setName(name);

    appender.setContext(context);
    appender.setLayout(buildLayout(context, layoutFactory));

    appender.addFilter(levelFilterFactory.build(threshold));
    getFilterFactories().stream().forEach(f -> appender.addFilter(f.build()));

    SSLTCPNetSyslogConfig config = new SSLTCPNetSyslogConfig(host, port);
    config.setIdent(programName);
    config.setMaxMessageLength(maxMessageLength);

    appender.setSyslogConfig(config);

    appender.start();

    return wrapAsync(appender, asyncAppenderFactory);
  }
}

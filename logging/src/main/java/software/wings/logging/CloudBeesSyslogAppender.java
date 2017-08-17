package software.wings.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.SyslogMessage;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;
import org.productivity.java.syslog4j.SyslogIF;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;

/**
 * Created by peeyushaggarwal on 3/7/17.
 */
public class CloudBeesSyslogAppender<E> extends AppenderBase<E> {
  private TcpSyslogMessageSender messageSender;
  private Layout<E> layout;
  private String programName;
  private String key;
  private String host;
  private int port;
  private String localhostName;

  public CloudBeesSyslogAppender() {}

  public CloudBeesSyslogAppender(String programName, String key, String host, int port) {
    this.programName = programName;
    this.key = key;
    this.host = host;
    this.port = port;
  }

  @Override
  protected void append(E loggingEvent) {
    try {
      SyslogMessage syslogMessage =
          new SyslogMessage() {
            @Override
            public void toRfc5424SyslogMessage(Writer out) throws IOException {
              StringWriter stringWriter = new StringWriter();
              stringWriter.write("<key:" + key + "> ");
              super.toRfc5424SyslogMessage(stringWriter);
              out.write(Integer.toString(stringWriter.toString().length()));
              out.write(' ');
              out.write("<key:" + key + "> ");
              super.toRfc5424SyslogMessage(out);
            }
          }
              .withSeverity(Severity.fromNumericalCode(getSeverityForEvent(loggingEvent)))
              .withMsg(layout.doLayout(loggingEvent))
              .withFacility(Facility.USER)
              .withAppName(programName)
              .withHostname(localhostName);
      messageSender.sendMessage(syslogMessage);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void start() {
    super.start();

    synchronized (this) {
      if (messageSender == null) {
        messageSender = new TcpSyslogMessageSender();
        messageSender.setDefaultAppName(programName);
        messageSender.setDefaultFacility(Facility.USER);
        messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
        messageSender.setSyslogServerHostname(host);
        messageSender.setSyslogServerPort(port);
        messageSender.setMessageFormat(MessageFormat.RFC_5424); // optional, default is RFC 3164
        messageSender.setSsl(true);
        messageSender.setSocketConnectTimeoutInMillis(5000);
      }
    }
    try {
      this.localhostName = InetAddress.getLocalHost().getHostName();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Convert a level to equivalent syslog severity. Only levels for printing
   * methods i.e DEBUG, WARN, INFO and ERROR are converted.
   *
   * @see ch.qos.logback.core.net.SyslogAppenderBase#getSeverityForEvent(Object)
   */
  public int getSeverityForEvent(Object eventObject) {
    if (eventObject instanceof ILoggingEvent) {
      ILoggingEvent event = (ILoggingEvent) eventObject;
      return LevelToSyslogSeverity.convert(event);
    } else {
      return SyslogIF.LEVEL_INFO;
    }
  }

  public Layout<E> getLayout() {
    return layout;
  }

  public void setLayout(Layout<E> layout) {
    this.layout = layout;
  }

  /**
   * Getter for property 'programName'.
   *
   * @return Value for property 'programName'.
   */
  public String getProgramName() {
    return programName;
  }

  /**
   * Setter for property 'programName'.
   *
   * @param programName Value to set for property 'programName'.
   */
  public void setProgramName(String programName) {
    this.programName = programName;
  }

  /**
   * Getter for property 'key'.
   *
   * @return Value for property 'key'.
   */
  public String getKey() {
    return key;
  }

  /**
   * Setter for property 'key'.
   *
   * @param key Value to set for property 'key'.
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Getter for property 'host'.
   *
   * @return Value for property 'host'.
   */
  public String getHost() {
    return host;
  }

  /**
   * Setter for property 'host'.
   *
   * @param host Value to set for property 'host'.
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Getter for property 'port'.
   *
   * @return Value for property 'port'.
   */
  public int getPort() {
    return port;
  }

  /**
   * Setter for property 'port'.
   *
   * @param port Value to set for property 'port'.
   */
  public void setPort(int port) {
    this.port = port;
  }

  public String getLocalhostName() {
    return localhostName;
  }

  public void setLocalhostName(String localhostName) {
    this.localhostName = localhostName;
  }
}

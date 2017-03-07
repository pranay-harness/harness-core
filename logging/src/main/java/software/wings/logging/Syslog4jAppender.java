package software.wings.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import org.productivity.java.syslog4j.SyslogConfigIF;
import org.productivity.java.syslog4j.SyslogIF;
import org.productivity.java.syslog4j.SyslogRuntimeException;
import org.productivity.java.syslog4j.impl.message.processor.structured.StructuredSyslogMessageProcessor;

/**
 * Created by peeyushaggarwal on 3/7/17.
 */
public class Syslog4jAppender<E> extends AppenderBase<E> {
  private SyslogIF syslog;
  private SyslogConfigIF syslogConfig;
  private Layout<E> layout;
  private String programName;
  private String key;

  public Syslog4jAppender() {}

  public Syslog4jAppender(String programName, String key) {
    this.programName = programName;
    this.key = key;
  }

  @Override
  protected void append(E loggingEvent) {
    syslog.log(getSeverityForEvent(loggingEvent), layout.doLayout(loggingEvent));
  }

  @Override
  public void start() {
    super.start();

    synchronized (this) {
      try {
        Class syslogClass = syslogConfig.getSyslogClass();
        syslog = (SyslogIF) syslogClass.newInstance();

        syslog.initialize(syslogClass.getSimpleName(), syslogConfig);
        syslog.setMessageProcessor(new StructuredSyslogMessageProcessor(programName) {
          @Override
          public String createSyslogHeader(int facility, int level, boolean sendLocalTimestamp, boolean sendLocalName) {
            return "<key:" + key + "> " + super.createSyslogHeader(facility, level, sendLocalTimestamp, sendLocalName);
          }
        });
      } catch (ClassCastException cse) {
        throw new SyslogRuntimeException(cse);
      } catch (IllegalAccessException iae) {
        throw new SyslogRuntimeException(iae);
      } catch (InstantiationException ie) {
        throw new SyslogRuntimeException(ie);
      }
    }
  }

  @Override
  public void stop() {
    super.stop();

    synchronized (this) {
      if (syslog != null) {
        syslog.shutdown();
        syslog = null;
      }
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

  public SyslogConfigIF getSyslogConfig() {
    return syslogConfig;
  }

  public void setSyslogConfig(SyslogConfigIF syslogConfig) {
    this.syslogConfig = syslogConfig;
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
}

package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Entity(value = "commandLogs", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("activityId")
                           , @Field("hostName") }))
public class Log extends Base {
  public enum LogLevel { DEBUG, INFO, WARN, ERROR, FATAL }

  private String activityId;
  private String hostName;
  private String logLine;
  private LogLevel logLevel;

  /**
   * Gets activity id.
   *
   * @return the activity id
   */
  public String getActivityId() {
    return activityId;
  }

  /**
   * Sets activity id.
   *
   * @param activityId the activity id
   */
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  /**
   * Gets host name.
   *
   * @return the host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Sets host name.
   *
   * @param hostName the host name
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets log line.
   *
   * @return the log line
   */
  public String getLogLine() {
    return logLine;
  }

  /**
   * Sets log line.
   *
   * @param logLine the log line
   */
  public void setLogLine(String logLine) {
    this.logLine = logLine;
  }

  public LogLevel getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(LogLevel logLevel) {
    this.logLevel = logLevel;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("activityId", activityId)
        .add("hostName", hostName)
        .add("logLine", logLine)
        .add("logLevel", logLevel)
        .toString();
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(activityId, hostName, logLine, logLevel);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final Log other = (Log) obj;
    return Objects.equals(this.activityId, other.activityId) && Objects.equals(this.hostName, other.hostName)
        && Objects.equals(this.logLine, other.logLine) && Objects.equals(this.logLevel, other.logLevel);
  }

  public static final class Builder {
    private String activityId;
    private String hostName;
    private String logLine;
    private LogLevel logLevel;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aLog() {
      return new Builder();
    }

    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withLogLine(String logLine) {
      this.logLine = logLine;
      return this;
    }

    public Builder withLogLevel(LogLevel logLevel) {
      this.logLevel = logLevel;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder but() {
      return aLog()
          .withActivityId(activityId)
          .withHostName(hostName)
          .withLogLine(logLine)
          .withLogLevel(logLevel)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Log build() {
      Log log = new Log();
      log.setActivityId(activityId);
      log.setHostName(hostName);
      log.setLogLine(logLine);
      log.setLogLevel(logLevel);
      log.setUuid(uuid);
      log.setAppId(appId);
      log.setCreatedBy(createdBy);
      log.setCreatedAt(createdAt);
      log.setLastUpdatedBy(lastUpdatedBy);
      log.setLastUpdatedAt(lastUpdatedAt);
      log.setActive(active);
      return log;
    }
  }
}

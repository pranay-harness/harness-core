package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Entity(value = "commandLogs", noClassnameStored = true)
public class Log extends Base {
  @NotEmpty @Indexed private String activityId;
  private String hostName;
  @NotEmpty @Indexed private String commandUnitName;
  private String logLine;
  private Integer linesCount;
  @NotNull private LogLevel logLevel;
  @NotNull private CommandExecutionStatus commandExecutionStatus;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

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

  /**
   * Gets log level.
   *
   * @return the log level
   */
  public LogLevel getLogLevel() {
    return logLevel;
  }

  /**
   * Sets log level.
   *
   * @param logLevel the log level
   */
  public void setLogLevel(LogLevel logLevel) {
    this.logLevel = logLevel;
  }

  /**
   * Gets command unit name.
   *
   * @return the command unit name
   */
  public String getCommandUnitName() {
    return commandUnitName;
  }

  /**
   * Sets command unit name.
   *
   * @param commandUnitName the command unit name
   */
  public void setCommandUnitName(String commandUnitName) {
    this.commandUnitName = commandUnitName;
  }

  /**
   * Gets execution result.
   *
   * @return the execution result
   */
  public CommandExecutionStatus getCommandExecutionStatus() {
    return commandExecutionStatus;
  }

  /**
   * Sets execution result.
   *
   * @param commandExecutionStatus the execution result
   */
  public void setCommandExecutionStatus(CommandExecutionStatus commandExecutionStatus) {
    this.commandExecutionStatus = commandExecutionStatus;
  }

  @Override
  @JsonIgnore
  public Map<String, Object> getShardKeys() {
    Map<String, Object> shardKeys = super.getShardKeys();
    shardKeys.put("activityId", activityId);
    return shardKeys;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(activityId, hostName, commandUnitName, logLine, logLevel, commandExecutionStatus);
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
        && Objects.equals(this.commandUnitName, other.commandUnitName) && Objects.equals(this.logLine, other.logLine)
        && Objects.equals(this.logLevel, other.logLevel)
        && Objects.equals(this.commandExecutionStatus, other.commandExecutionStatus);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("activityId", activityId)
        .add("hostName", hostName)
        .add("commandUnitName", commandUnitName)
        .add("logLine", logLine)
        .add("logLevel", logLevel)
        .add("commandExecutionStatus", commandExecutionStatus)
        .toString();
  }

  public Integer getLinesCount() {
    return linesCount;
  }

  public void setLinesCount(Integer linesCount) {
    this.linesCount = linesCount;
  }

  /**
   * The enum Log level.
   */
  public enum LogLevel {
    /**
     * Debug log level.
     */
    DEBUG,
    /**
     * Info log level.
     */
    INFO,
    /**
     * Warn log level.
     */
    WARN,
    /**
     * Error log level.
     */
    ERROR,
    /**
     * Fatal log level.
     */
    FATAL
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String activityId;
    private String hostName;
    private String commandUnitName;
    private String logLine;
    private LogLevel logLevel;
    private CommandExecutionStatus commandExecutionStatus;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A log builder.
     *
     * @return the builder
     */
    public static Builder aLog() {
      return new Builder();
    }

    /**
     * With activity id builder.
     *
     * @param activityId the activity id
     * @return the builder
     */
    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    /**
     * With host name builder.
     *
     * @param hostName the host name
     * @return the builder
     */
    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With command unit name builder.
     *
     * @param commandUnitName the command unit name
     * @return the builder
     */
    public Builder withCommandUnitName(String commandUnitName) {
      this.commandUnitName = commandUnitName;
      return this;
    }

    /**
     * With log line builder.
     *
     * @param logLine the log line
     * @return the builder
     */
    public Builder withLogLine(String logLine) {
      this.logLine = logLine;
      return this;
    }

    /**
     * With log level builder.
     *
     * @param logLevel the log level
     * @return the builder
     */
    public Builder withLogLevel(LogLevel logLevel) {
      this.logLevel = logLevel;
      return this;
    }

    /**
     * With execution result builder.
     *
     * @param commandExecutionStatus the execution result
     * @return the builder
     */
    public Builder withExecutionResult(CommandExecutionStatus commandExecutionStatus) {
      this.commandExecutionStatus = commandExecutionStatus;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aLog()
          .withActivityId(activityId)
          .withHostName(hostName)
          .withCommandUnitName(commandUnitName)
          .withLogLine(logLine)
          .withLogLevel(logLevel)
          .withExecutionResult(commandExecutionStatus)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build log.
     *
     * @return the log
     */
    public Log build() {
      Log log = new Log();
      log.setActivityId(activityId);
      log.setHostName(hostName);
      log.setCommandUnitName(commandUnitName);
      log.setLogLine(logLine);
      log.setLogLevel(logLevel);
      log.setCommandExecutionStatus(commandExecutionStatus);
      log.setUuid(uuid);
      log.setAppId(appId);
      log.setCreatedBy(createdBy);
      log.setCreatedAt(createdAt);
      log.setLastUpdatedBy(lastUpdatedBy);
      log.setLastUpdatedAt(lastUpdatedAt);
      return log;
    }
  }
}

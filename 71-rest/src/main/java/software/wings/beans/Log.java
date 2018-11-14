package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Entity(value = "commandLogs", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
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
  @Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  @Override
  @JsonIgnore
  public Map<String, Object> getShardKeys() {
    Map<String, Object> shardKeys = super.getShardKeys();
    shardKeys.put("activityId", activityId);
    return shardKeys;
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

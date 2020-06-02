package software.wings.beans;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.GoogleDataStoreAware.readBlob;
import static io.harness.persistence.GoogleDataStoreAware.readLong;
import static io.harness.persistence.GoogleDataStoreAware.readString;
import static java.lang.System.currentTimeMillis;
import static software.wings.beans.Log.Builder.aLog;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.StringValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.WingsException;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.GoogleDataStoreAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Log.LogKeys;
import software.wings.beans.entityinterface.ApplicationAccess;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@FieldNameConstants(innerTypeName = "LogKeys")
@Entity(value = "commandLogs", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Indexes({
  @Index(options = @IndexOptions(name = "appId_activityId"), fields = {
    @Field(value = LogKeys.appId), @Field(value = LogKeys.activityId)
  })
})
public class Log implements GoogleDataStoreAware, PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware,
                            UpdatedAtAware, UpdatedByAware, ApplicationAccess {
  public static final String ID_KEY = "_id";

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;
  @NotEmpty private String activityId;
  private String hostName;
  @NotEmpty private String commandUnitName;
  private String logLine;
  private int linesCount;
  @NotNull private LogLevel logLevel;
  @NotNull private CommandExecutionStatus commandExecutionStatus;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  @Override
  public com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore) {
    Key taskKey = datastore.newKeyFactory()
                      .setKind(Log.class.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                      .newKey(generateUuid());
    try {
      com.google.cloud.datastore.Entity.Builder logEntityBuilder =
          com.google.cloud.datastore.Entity.newBuilder(taskKey)
              .set("activityId", getActivityId())

              .set("linesCount", LongValue.newBuilder(getLinesCount()).setExcludeFromIndexes(true).build())
              .set("logLevel",
                  com.google.cloud.datastore.StringValue.newBuilder(getLogLevel().toString())
                      .setExcludeFromIndexes(true)
                      .build())
              .set("commandExecutionStatus",
                  com.google.cloud.datastore.StringValue.newBuilder(getCommandExecutionStatus().name())
                      .setExcludeFromIndexes(true)
                      .build())
              .set(CREATED_AT_KEY, currentTimeMillis());

      if (getLogLine().length() <= 256) {
        logEntityBuilder.set("logLine", StringValue.newBuilder(getLogLine()).setExcludeFromIndexes(true).build());
      } else {
        logEntityBuilder.set("compressedLogLine",
            BlobValue.newBuilder(Blob.copyFrom(compressString(getLogLine()))).setExcludeFromIndexes(true).build());
      }

      if (isNotEmpty(getHostName())) {
        logEntityBuilder.set("hostName", getHostName());
      }

      if (isNotEmpty(getAppId())) {
        logEntityBuilder.set("appId", getAppId());
      }

      if (isNotEmpty(getCommandUnitName())) {
        logEntityBuilder.set("commandUnitName", getCommandUnitName());
      }

      if (validUntil != null) {
        logEntityBuilder.set("validUntil", validUntil.getTime());
      }

      return logEntityBuilder.build();
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }

  @Override
  public GoogleDataStoreAware readFromCloudStorageEntity(com.google.cloud.datastore.Entity entity) {
    final Log log = aLog()
                        .withUuid(entity.getKey().getName())
                        .withActivityId(readString(entity, "activityId"))
                        .withLogLevel(LogLevel.valueOf(readString(entity, "logLevel")))
                        .withCreatedAt(readLong(entity, CREATED_AT_KEY))
                        .withHostName(readString(entity, "hostName"))
                        .withAppId(readString(entity, "appId"))
                        .withCommandUnitName(readString(entity, "commandUnitName"))
                        .build();
    try {
      byte[] compressedLogLine = readBlob(entity, "compressedLogLine");
      if (isNotEmpty(compressedLogLine)) {
        log.setLogLine(deCompressString(compressedLogLine));
      } else {
        log.setLogLine(readString(entity, "logLine"));
      }
    } catch (IOException e) {
      throw new WingsException(e);
    }

    return log;
  }

  public enum LogLevel { DEBUG, INFO, WARN, ERROR, FATAL }

  static final String END_MARK = "#==#";
  static final String NO_FORMATTING = "\033[0m";

  public static String color(String value, LogColor color) {
    return color(value, color, LogWeight.Normal, LogColor.Black);
  }

  public static String color(String value, LogColor color, LogWeight weight) {
    return color(value, color, weight, LogColor.Black);
  }

  public static String color(String value, LogColor color, LogWeight weight, LogColor background) {
    String format = "\033[" + weight.value + ";" + color.value + "m\033[" + getBackgroundColorValue(background) + "m";
    return format + value.replaceAll(END_MARK, format).replaceAll("\\n", "\n" + format) + END_MARK;
  }

  private static int getBackgroundColorValue(LogColor background) {
    return background.value + 10;
  }

  public static String doneColoring(String value) {
    return value.replaceAll(END_MARK, NO_FORMATTING);
  }

  public enum LogColor {
    Red(91),
    Orange(33),
    Yellow(93),
    Green(92),
    Blue(94),
    Purple(95),
    Cyan(96),
    Gray(37),
    Black(30),
    White(97),

    GrayDark(90),
    RedDark(31),
    GreenDark(32),
    BlueDark(34),
    PurpleDark(35),
    CyanDark(36);

    final int value;

    LogColor(int value) {
      this.value = value;
    }
  }

  public enum LogWeight {
    Normal(0),
    Bold(1);

    final int value;

    LogWeight(int value) {
      this.value = value;
    }
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

  public static final class LogKeys {
    private LogKeys() {}
    // Temporary
    public static final String appId = "appId";
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
  }
}

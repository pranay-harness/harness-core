package software.wings.verification;

import static software.wings.common.VerificationConstants.ACTIVITY_LOG_TTL_WEEKS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@FieldNameConstants(innerTypeName = "CVActivityLogKeys")
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "cvActivityLogs", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class CVActivityLog implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  @Indexed private String cvConfigId;
  @Indexed private String stateExecutionId;
  @JsonProperty(value = "timestamp") private long createdAt;
  private long lastUpdatedAt;
  @Indexed private long dataCollectionMinute;
  @NonNull private String log;
  @NonNull private LogLevel logLevel;
  private List<Long> timestampParams;

  @Default
  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusWeeks(ACTIVITY_LOG_TTL_WEEKS).toInstant());

  @Override
  @JsonIgnore
  public long getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public List<Long> getTimestampParams() {
    if (timestampParams == null) {
      return Collections.emptyList();
    }
    return timestampParams;
  }

  public String getAnsiLog() {
    return logLevel.toAnsi(log);
  }
  public enum LogLevel {
    INFO(),
    WARN(Ansi.Yellow),
    ERROR(Ansi.Red);

    private Ansi ansi;

    LogLevel(Ansi ansi) {
      this.ansi = ansi;
    }

    LogLevel() {}

    public String toAnsi(String log) {
      return ansi == null ? log : ansi.colorize(log);
    }
  }
}

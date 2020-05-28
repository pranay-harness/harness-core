package software.wings.verification;

import static software.wings.beans.Log.LogColor.Red;
import static software.wings.beans.Log.LogColor.Yellow;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.Log.doneColoring;
import static software.wings.common.VerificationConstants.ACTIVITY_LOG_TTL_WEEKS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
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
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

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
@Indexes({
  @Index(fields = {
    @Field("cvConfigId")
    , @Field(value = "dataCollectionMinute", type = IndexType.DESC), @Field(value = "createdAt", type = IndexType.ASC)
  }, options = @IndexOptions(name = "service_guard_idx"))
})
public class CVActivityLog implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;
  @Indexed private String cvConfigId;
  @Indexed private String stateExecutionId;
  @JsonProperty(value = "timestamp") private long createdAt;
  private long lastUpdatedAt;
  @Indexed private long dataCollectionMinute;
  @NonNull private String log;
  @NonNull private LogLevel logLevel;
  private List<Long> timestampParams;
  @Indexed private String accountId;

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
    String ansiLog;
    if (logLevel == LogLevel.ERROR) {
      ansiLog = color(log, Red, Bold);
    } else if (logLevel == LogLevel.WARN) {
      ansiLog = color(log, Yellow, Bold);
    } else {
      ansiLog = log;
    }

    return doneColoring(ansiLog);
  }

  public enum LogLevel { INFO, WARN, ERROR }
}

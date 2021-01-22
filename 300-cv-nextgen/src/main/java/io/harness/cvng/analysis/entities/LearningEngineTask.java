package io.harness.cvng.analysis.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * @author praveensugavanam
 */
@Data
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "LearningEngineTaskKeys")
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "learningEngineTasks")
@HarnessEntity(exportable = true)
public abstract class LearningEngineTask implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("query_idx")
                 .unique(false)
                 .field(LearningEngineTaskKeys.taskStatus)
                 .field(LearningEngineTaskKeys.taskPriority)
                 .build())
        .add(SortCompoundMongoIndex.builder()
                 .name("trend_idx")
                 .unique(false)
                 .field(LearningEngineTaskKeys.verificationTaskId)
                 .field(LearningEngineTaskKeys.analysisEndTime)
                 .field(LearningEngineTaskKeys.analysisType)
                 .build())
        .build();
  }

  @Id private String uuid;
  private String verificationTaskId;
  @FdIndex private long createdAt;
  @FdIndex private long lastUpdatedAt;
  private String accountId;
  private LearningEngineTaskType analysisType;
  private int taskPriority = 1;
  private String analysisSaveUrl;
  private String failureUrl;
  private Instant analysisStartTime;
  private Instant analysisEndTime;
  private long analysisEndEpochMinute; // This is temporary. LE needs it for now.
  private ExecutionStatus taskStatus;
  public abstract LearningEngineTaskType getType();

  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(31).toInstant());

  public enum LearningEngineTaskType {
    SERVICE_GUARD_TIME_SERIES,
    LOG_CLUSTER,
    SERVICE_GUARD_LOG_ANALYSIS,
    CANARY_LOG_ANALYSIS,
    TEST_LOG_ANALYSIS,
    TIME_SERIES_CANARY,
    SERVICE_GUARD_FEEDBACK_ANALYSIS,
    TIME_SERIES_LOAD_TEST
  }

  public enum ExecutionStatus { QUEUED, RUNNING, FAILED, SUCCESS, TIMEOUT }
}

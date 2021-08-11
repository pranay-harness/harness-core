package io.harness.cvng.core.entities;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.cvng.beans.DataCollectionExecutionStatus;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;

@FieldNameConstants(innerTypeName = "DataCollectionTaskKeys")
@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "dataCollectionTasks")
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.CVNG)
public abstract class DataCollectionTask
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("verification_task_start_time_unique_idx")
                 .unique(false)
                 .field(DataCollectionTaskKeys.accountId)
                 .field(DataCollectionTaskKeys.verificationTaskId)
                 .field(DataCollectionTaskKeys.startTime)
                 .build())
        .add(SortCompoundMongoIndex.builder()
                 .name("verificationTaskIdQueryIdx")
                 .unique(false)
                 .field(DataCollectionTaskKeys.verificationTaskId)
                 .descSortField(DataCollectionTaskKeys.startTime)
                 .build())
        .build();
  }
  @Id private String uuid;
  @NonNull private String accountId;
  @FdIndex private String verificationTaskId;
  @FdIndex private String dataCollectionWorkerId;
  private Type type;
  @Getter(AccessLevel.NONE) @Builder.Default private boolean queueAnalysis = true;
  private String nextTaskId;
  @FdIndex @NonNull private DataCollectionExecutionStatus status;

  @FdIndex private long createdAt;
  @FdIndex private long lastUpdatedAt;
  private Instant lastPickedAt;
  private int retryCount;

  private String exception;
  private String stacktrace;
  private Instant validAfter;
  private DataCollectionInfo dataCollectionInfo;
  private Instant startTime;
  private Instant endTime;

  public boolean shouldQueueAnalysis() {
    return queueAnalysis;
  }
  @Builder.Default
  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(30).toInstant());
  @PrePersist
  private void prePersist() {
    if (validAfter == null) {
      validAfter = endTime.plus(DATA_COLLECTION_DELAY);
    }
  }

  public abstract boolean shouldCreateNextTask();

  public abstract boolean eligibleForRetry(Instant currentTime);

  public abstract Instant getNextValidAfter(Instant currentTime);

  public enum Type { SERVICE_GUARD, DEPLOYMENT }
  public Duration totalTime(Instant currentTime) {
    return Duration.between(validAfter, currentTime);
  }
  public Duration runningTime(Instant currentTime) {
    return Duration.between(lastPickedAt, currentTime);
  }

  public Duration waitTime() {
    return Duration.between(validAfter, lastPickedAt);
  }
}

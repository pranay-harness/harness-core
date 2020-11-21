package io.harness.cvng.core.entities;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.DataCollectionExecutionStatus;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
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
public abstract class DataCollectionTask
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;
  @NonNull @FdIndex private String accountId;
  @FdIndex private String verificationTaskId;
  @FdIndex private String dataCollectionWorkerId;
  private Type type;
  @Getter(AccessLevel.NONE) @Builder.Default private boolean queueAnalysis = true;
  private String nextTaskId;
  @FdIndex @NonNull private DataCollectionExecutionStatus status;

  private long createdAt;
  @FdIndex private long lastUpdatedAt;

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
}

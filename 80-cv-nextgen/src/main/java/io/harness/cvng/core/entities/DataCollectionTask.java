package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.beans.ExecutionStatus;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;

@FieldNameConstants(innerTypeName = "DataCollectionTaskKeys")
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "dataCollectionTasks", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class DataCollectionTask implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;

  @NonNull @FdIndex private String accountId;
  @FdIndex private String cvConfigId;

  private String nextTaskId;
  @FdIndex @NonNull private ExecutionStatus status;

  private long createdAt;
  @FdIndex private long lastUpdatedAt;

  private int retryCount;

  private String exception;
  private String stacktrace;
  private long validAfter;
  private DataCollectionInfo dataCollectionInfo;
  private Instant startTime;
  private Instant endTime;
  @Builder.Default
  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(30).toInstant());
}

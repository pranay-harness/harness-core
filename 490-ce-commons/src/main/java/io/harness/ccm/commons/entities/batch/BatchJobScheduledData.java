package io.harness.ccm.commons.entities.batch;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.persistence.ValidUntilAccess;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Entity(value = "batchJobScheduledData", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "BatchJobScheduledDataKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn(DbAliases.CENG)
@OwnedBy(CE)
public final class BatchJobScheduledData
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, ValidUntilAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("accountId_batchJobType_validRun_endAt")
                 .field(BatchJobScheduledDataKeys.accountId)
                 .field(BatchJobScheduledDataKeys.batchJobType)
                 .field(BatchJobScheduledDataKeys.validRun)
                 .descSortField(BatchJobScheduledDataKeys.endAt)
                 .build())
        .build();
  }

  @Id String uuid;
  String accountId;
  String batchJobType;
  String comments;
  long jobRunTimeMillis;
  boolean validRun;
  Instant startAt;
  Instant endAt;
  long createdAt;
  long lastUpdatedAt;

  @JsonIgnore
  @FdTtlIndex
  @SchemaIgnore
  @Builder.Default
  @EqualsAndHashCode.Exclude
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(3).toInstant());

  public BatchJobScheduledData(
      String accountId, String batchJobType, long jobRunTimeMillis, Instant startAt, Instant endAt) {
    this.accountId = accountId;
    this.batchJobType = batchJobType;
    this.jobRunTimeMillis = jobRunTimeMillis;
    this.startAt = startAt;
    this.endAt = endAt;
    this.validRun = true;
  }
}

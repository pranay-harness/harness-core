package io.harness.cvng.cdng.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CVNGStepTaskKeys")
@EqualsAndHashCode
@Entity(value = "cvngStepTasks", noClassnameStored = true)
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.CVNG)
public class CVNGStepTask
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, PersistentRegularIterable {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("iterator")
                 .unique(false)
                 .field(CVNGStepTaskKeys.status)
                 .field(CVNGStepTaskKeys.asyncTaskIteration)
                 .build())
        .build();
  }
  @Id private String uuid;
  private String accountId;
  private long createdAt;
  private long lastUpdatedAt;
  private String activityId;
  @FdIndex private String callbackId;
  private boolean skip;
  private Status status;
  @EqualsAndHashCode.Exclude
  @FdTtlIndex
  @Builder.Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
  private long asyncTaskIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (CVNGStepTaskKeys.asyncTaskIteration.equals(fieldName)) {
      this.asyncTaskIteration = nextIteration;
      return;
    }

    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (CVNGStepTaskKeys.asyncTaskIteration.equals(fieldName)) {
      return this.asyncTaskIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public static List<Status> getNonFinalStatues() {
    return Collections.singletonList(Status.IN_PROGRESS);
  }
  public void validate() {
    Preconditions.checkNotNull(accountId);
    if (!skip) {
      Preconditions.checkNotNull(activityId);
    }
    Preconditions.checkNotNull(callbackId);
    Preconditions.checkNotNull(status);
  }
  public enum Status { IN_PROGRESS, DONE }
}

package io.harness.cvng.state;

import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.OffsetDateTime;
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
@FieldNameConstants(innerTypeName = "CVNGVerificationTaskKeys")
@EqualsAndHashCode(callSuper = false)
@Entity(value = "cvngVerificationTasks", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class CVNGVerificationTask
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess, PersistentRegularIterable {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("iterator")
                 .unique(false)
                 .field(CVNGVerificationTaskKeys.status)
                 .field(CVNGVerificationTaskKeys.cvngVerificationTaskIteration)
                 .build())
        .build();
  }
  @Id private String uuid;
  private String accountId;
  private long createdAt;
  private long lastUpdatedAt;
  private String activityId;
  private String correlationId;
  private Status status;
  private Instant startTime;
  @FdTtlIndex @Builder.Default private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
  private long cvngVerificationTaskIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (CVNGVerificationTaskKeys.cvngVerificationTaskIteration.equals(fieldName)) {
      this.cvngVerificationTaskIteration = nextIteration;
      return;
    }

    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (CVNGVerificationTaskKeys.cvngVerificationTaskIteration.equals(fieldName)) {
      return this.cvngVerificationTaskIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public enum Status { IN_PROGRESS, DONE, TIMED_OUT }
}

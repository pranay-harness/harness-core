package io.harness.batch.processing.entities;

import static io.harness.event.app.EventServiceApplication.EVENTS_DB;

import io.harness.annotation.StoreIn;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.entities.BatchJobScheduledData.BatchJobScheduledDataKeys;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.time.Instant;

@Data
@Entity(value = "batchJobScheduledData", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "batchJobType_endAt"), fields = {
    @Field(BatchJobScheduledDataKeys.batchJobType), @Field(BatchJobScheduledDataKeys.endAt)
  })
})
@FieldNameConstants(innerTypeName = "BatchJobScheduledDataKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn(EVENTS_DB)
public class BatchJobScheduledData implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  BatchJobType batchJobType;
  Instant startAt;
  Instant endAt;
  long createdAt;
  long lastUpdatedAt;

  public BatchJobScheduledData(BatchJobType batchJobType, Instant startAt, Instant endAt) {
    this.batchJobType = batchJobType;
    this.startAt = startAt;
    this.endAt = endAt;
  }
}

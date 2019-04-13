package io.harness.mongo;

import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

@Data
@FieldNameConstants(innerTypeName = "SampleEntityKeys")
public class SampleEntity
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  @Id private String uuid;
  private EmbeddedUser createdBy;
  @Indexed private long createdAt;

  private EmbeddedUser lastUpdatedBy;
  private long lastUpdatedAt;
}

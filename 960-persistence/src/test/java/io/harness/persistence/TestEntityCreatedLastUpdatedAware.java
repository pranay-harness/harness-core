package io.harness.persistence;

import io.harness.beans.EmbeddedUser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TestEntityCreatedLastUpdatedAwareKeys")
class TestEntityCreatedLastUpdatedAware
    implements PersistentEntity, UuidAccess, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  @Id private String uuid;
  private long createdAt;
  private EmbeddedUser createdBy;
  private long lastUpdatedAt;
  private EmbeddedUser lastUpdatedBy;

  private String test;
}

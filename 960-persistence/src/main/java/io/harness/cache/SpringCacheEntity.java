package io.harness.cache;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;

import java.util.Date;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "SpringCacheEntityKeys")
@Entity(value = "cacheEntities")
@Document("cacheEntities")
public class SpringCacheEntity implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id String canonicalKey;
  long contextValue;

  byte[] entity;
  Date validUntil;

  // audit fields
  @Wither @FdIndex @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastUpdatedAt;
  @Version Long version;
}

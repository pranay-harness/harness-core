package io.harness.cache;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Date;

@Indexes({
  @Index(fields = {
    @Field("_id"), @Field("contextValue")
  }, options = @IndexOptions(unique = true, name = "commutativeIdx"))
})
@Value
@Builder
@FieldNameConstants(innerTypeName = "CacheEntityKeys")
@Entity(value = "cache")
@HarnessEntity(exportable = false)
public class CacheEntity implements PersistentEntity {
  long contextValue;
  @Id String canonicalKey;

  byte[] entity;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) Date validUntil;
}

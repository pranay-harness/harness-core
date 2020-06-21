package io.harness.cache;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.Indexed;
import io.harness.mongo.index.UniqueIndex;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Date;

@UniqueIndex(name = "commutativeIdx", fields = { @Field("_id")
                                                 , @Field("contextValue") })
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

package io.harness.cache;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.TtlIndex;
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

  @TtlIndex Date validUntil;
}

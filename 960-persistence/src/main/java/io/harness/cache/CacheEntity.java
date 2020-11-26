package io.harness.cache;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;

import java.util.Date;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@OwnedBy(PL)
@NgUniqueIndex(name = "commutativeIdx", fields = { @Field("_id")
                                                   , @Field("contextValue") })
@Value
@Builder
@FieldNameConstants(innerTypeName = "CacheEntityKeys")
@Entity(value = "cache")
@HarnessEntity(exportable = false)
public class CacheEntity implements PersistentEntity, AccountAccess {
  long contextValue;
  @Id String canonicalKey;

  byte[] entity;

  @FdTtlIndex Date validUntil;
  @FdIndex String accountId;
}

package io.harness.aggregator.models;

import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@Document("mongoReconciliationOffset")
@Entity(value = "mongoReconciliationOffset", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "keys")
@StoreIn(ACCESS_CONTROL)
@TypeAlias("mongoReconciliationOffset")
public class MongoReconciliationOffset implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id private String id;
  private byte[] key;
  private byte[] value;
  @FdIndex @CreatedDate private long createdAt;
}

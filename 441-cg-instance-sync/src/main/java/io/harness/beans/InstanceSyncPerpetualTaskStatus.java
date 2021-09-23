package io.harness.beans;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "InstanceSyncPerpetualTaskStatusKeys")
@Entity(value = "instanceSyncPerpetualTaskStatus", noClassnameStored = true)
@Document("instanceSyncPerpetualTaskStatus")
@StoreIn(DbAliases.CG_MANAGER)
@OwnedBy(HarnessTeam.PL)
public class InstanceSyncPerpetualTaskStatus implements PersistentEntity {
  @Id String uuid;
  @FdUniqueIndex String perpetualTaskId;
  String lastFailureReason;
  long initialFailureAt;
}

package io.harness.entity;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "InstanceSyncPerpetualTaskStatusKeys")
@Entity(value = "instanceSyncPerpetualTaskStatus", noClassnameStored = true)
@OwnedBy(HarnessTeam.PL)
public class InstanceSyncPerpetualTaskStatus implements PersistentEntity {
  @Id String uuid;
  @FdUniqueIndex String perpetualTaskId;
  String lastFailureReason;
  long initialFailureAt;
}

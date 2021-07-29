package io.harness.entities.instancesyncperpetualtaskinfo;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.ng.DbAliases;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "InstanceSyncPerpetualTaskInfoKeys")
@Entity(value = "instanceSyncPerpetualTasksInfoNG", noClassnameStored = true)
@Document("instanceSyncPerpetualTasksInfoNG")
@StoreIn(DbAliases.NG_MANAGER)
@Persistent
@OwnedBy(HarnessTeam.DX)
public class InstanceSyncPerpetualTaskInfo {
  @Id @org.mongodb.morphia.annotations.Id String id;
  String accountIdentifier;
  @FdUniqueIndex String infrastructureMappingId;
  List<DeploymentInfoDetails> deploymentInfoDetailsList;
  @FdUniqueIndex String perpetualTaskId;
  @CreatedDate long createdAt;
  @LastModifiedDate long lastUpdatedAt;
}

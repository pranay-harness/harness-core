package io.harness.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "InstanceSyncPerpetualTaskInfoKeys")
@Entity(value = "instanceSyncPerpetualTasksInfo", noClassnameStored = true)
@OwnedBy(HarnessTeam.DX)
public class InstanceSyncPerpetualTaskInfo implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_account_org_project_id")
                 .unique(true)
                 .field(InstanceSyncPerpetualTaskInfoKeys.accountIdentifier)
                 .field(InstanceSyncPerpetualTaskInfoKeys.orgIdentifier)
                 .field(InstanceSyncPerpetualTaskInfoKeys.projectIdentifier)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @FdIndex String infrastructureMappingId;
  @Singular List<String> perpetualTaskIds;
  @CreatedDate long createdAt;
  @LastModifiedDate long lastUpdatedAt;
}

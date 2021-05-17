package io.harness.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;

/**
 * Keeps track of the last sync status and time of the infra mapping.
 */
@Data
@Entity(value = "syncStatus", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "SyncStatusKeys")
@OwnedBy(HarnessTeam.DX)
public class SyncStatus implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("compositeIdx1")
                 .unique(true)
                 .field(SyncStatusKeys.orgIdentifier)
                 .field(SyncStatusKeys.projectIdentifier)
                 .field(SyncStatusKeys.serviceId)
                 .field(SyncStatusKeys.envId)
                 .field(SyncStatusKeys.infraMappingId)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("compositeIdx2")
                 .field(SyncStatusKeys.orgIdentifier)
                 .field(SyncStatusKeys.projectIdentifier)
                 .field(SyncStatusKeys.infraMappingId)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id private String id;
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String envId;
  private String serviceId;
  private String infraMappingId;
  private String infraMappingName;

  private long lastSyncedAt;
  private long lastSuccessfullySyncedAt;
  private String syncFailureReason;
}

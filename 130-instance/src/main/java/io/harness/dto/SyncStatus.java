package io.harness.dto;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import software.wings.beans.Base;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;

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
                 .field(Base.APP_ID_KEY2)
                 .field(SyncStatusKeys.serviceId)
                 .field(SyncStatusKeys.envId)
                 .field(SyncStatusKeys.infraMappingId)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("compositeIdx2")
                 .field(Base.APP_ID_KEY2)
                 .field(SyncStatusKeys.infraMappingId)
                 .build())
        .build();
  }

  public static final String SERVICE_ID_KEY = "serviceId";
  public static final String ENV_ID_KEY = "envId";
  public static final String INFRA_MAPPING_ID_KEY = "infraMappingId";
  public static final String ORG_ID_KEY = "orgId";
  public static final String PROJECT_ID_KEY = "projectId";
  public static final String ID_KEY = "id";

  private String id;
  private String envId;
  private String serviceId;
  private String infraMappingId;
  private String infraMappingName;

  private long lastSyncedAt;
  private long lastSuccessfullySyncedAt;
  private String syncFailureReason;
}

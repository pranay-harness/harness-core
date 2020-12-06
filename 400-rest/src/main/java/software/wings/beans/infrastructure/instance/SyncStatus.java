package software.wings.beans.infrastructure.instance;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;

import software.wings.beans.Base;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

/**
 * Keeps track of the last sync status and time of the infra mapping.
 *
 * @author rktummala on 05/19/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NgUniqueIndex(name = "compositeIdx1",
    fields = { @Field("appId")
               , @Field("serviceId"), @Field("envId"), @Field("infraMappingId") })
@CdIndex(name = "compositeIdx2", fields = { @Field("appId")
                                            , @Field("infraMappingId") })
@Entity(value = "syncStatus", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class SyncStatus extends Base {
  public static final String SERVICE_ID_KEY = "serviceId";
  public static final String ENV_ID_KEY = "envId";
  public static final String INFRA_MAPPING_ID_KEY = "infraMappingId";

  private String envId;
  private String serviceId;
  private String infraMappingId;
  private String infraMappingName;

  private long lastSyncedAt;
  private long lastSuccessfullySyncedAt;
  private String syncFailureReason;

  @Builder
  public SyncStatus(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, String envId, String serviceId, String infraMappingId,
      String infraMappingName, long lastSyncedAt, long lastSuccessfullySyncedAt, String syncFailureReason) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.envId = envId;
    this.serviceId = serviceId;
    this.infraMappingId = infraMappingId;
    this.infraMappingName = infraMappingName;
    this.lastSyncedAt = lastSyncedAt;
    this.lastSuccessfullySyncedAt = lastSuccessfullySyncedAt;
    this.syncFailureReason = syncFailureReason;
  }
}

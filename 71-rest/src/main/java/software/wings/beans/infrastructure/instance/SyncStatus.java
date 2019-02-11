package software.wings.beans.infrastructure.instance;

import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

/**
 * Keeps track of the last sync status and time of the infra mapping.
 *
 * @author rktummala on 05/19/18
 */
@Entity(value = "syncStatus", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Indexes({
  @Index(fields = { @Field("appId")
                    , @Field("serviceId"), @Field("envId"), @Field("infraMappingId") },
      options = @IndexOptions(name = "compositeIdx1", unique = true))
  ,
      @Index(fields = { @Field("appId")
                        , @Field("infraMappingId") }, options = @IndexOptions(name = "compositeIdx2"))
})
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

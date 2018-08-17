package software.wings.beans.infrastructure.instance;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

import java.util.List;

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
      options = @IndexOptions(unique = true))
  ,
      @Index(fields = { @Field("appId")
                        , @Field("infraMappingId") })
})
public class SyncStatus extends Base {
  private String envId;
  private String serviceId;
  private String infraMappingId;
  private String infraMappingName;

  private long lastSyncedAt;
  private long lastSuccessfullySyncedAt;
  private String syncFailureReason;

  @Builder
  public SyncStatus(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String envId, String serviceId,
      String infraMappingId, String infraMappingName, long lastSyncedAt, long lastSuccessfullySyncedAt,
      String syncFailureReason) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.envId = envId;
    this.serviceId = serviceId;
    this.infraMappingId = infraMappingId;
    this.infraMappingName = infraMappingName;
    this.lastSyncedAt = lastSyncedAt;
    this.lastSuccessfullySyncedAt = lastSuccessfullySyncedAt;
    this.syncFailureReason = syncFailureReason;
  }
}

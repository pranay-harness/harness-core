package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.Indexed;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

import java.util.Date;

/**
 * Created by anubhaw on 7/17/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity(value = "permits", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class Permit extends Base {
  public static final String PERMIT_KEY_ID = "key";
  @Indexed(options = @IndexOptions(unique = true)) private String key;
  private String group;
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date expireAt;
  private long leaseDuration;

  @Builder
  public Permit(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, String key, String group, Date expireAt, long leaseDuration) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.key = key;
    this.group = group;
    this.expireAt = new Date(expireAt.getTime());
    this.leaseDuration = leaseDuration;
  }
}

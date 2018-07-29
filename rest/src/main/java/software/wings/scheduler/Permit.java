package software.wings.scheduler;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

import java.util.Date;
import java.util.List;

/**
 * Created by anubhaw on 7/17/18.
 */
@Entity(value = "permits", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = false)
public class Permit extends Base {
  @Indexed(options = @IndexOptions(unique = true, background = true)) private String key;
  private String group;
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date expireAt;
  private long leaseDuration;

  @Builder
  public Permit(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String key, String group, Date expireAt,
      long leaseDuration) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.key = key;
    this.group = group;
    this.expireAt = new Date(expireAt.getTime());
    this.leaseDuration = leaseDuration;
  }
}

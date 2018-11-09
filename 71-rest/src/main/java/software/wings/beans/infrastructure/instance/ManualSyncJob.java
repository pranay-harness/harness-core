package software.wings.beans.infrastructure.instance;

import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;

import java.util.List;

/**
 * Keeps track of the manual sync job. These are short-lived.
 * We just need to persist them so that all managers can access them.
 *
 * @author rktummala on 06/04/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity(value = "manualSyncJobStatus", noClassnameStored = true)
public class ManualSyncJob extends Base {
  @Builder
  public ManualSyncJob(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
  }
}

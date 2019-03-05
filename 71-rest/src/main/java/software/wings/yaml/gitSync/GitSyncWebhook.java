package software.wings.yaml.gitSync;

import io.harness.annotation.HarnessExportableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;

/**
 * Created by bsollish on 10/03/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity(value = "gitSyncWebhook", noClassnameStored = true)
@HarnessExportableEntity
public class GitSyncWebhook extends Base {
  private String accountId;
  private String webhookToken;
  private String entityId;
}
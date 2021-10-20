package software.wings.yaml.gitSync;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by bsollish on 10/03/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity(value = "gitSyncWebhook", noClassnameStored = true)
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "GitSyncWebhookKeys")
public final class GitSyncWebhook extends Base implements AccountAccess {
  private String accountId;
  private String webhookToken;
  private String entityId;
}

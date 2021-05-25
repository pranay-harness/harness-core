package software.wings.beans.alert;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;

import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Entity(value = "notificationRulesStatuses", noClassnameStored = true)
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.CG_MANAGER)
public class NotificationRulesStatus implements PersistentEntity, AccountAccess {
  @Id private String accountId;
  private boolean enabled;
}

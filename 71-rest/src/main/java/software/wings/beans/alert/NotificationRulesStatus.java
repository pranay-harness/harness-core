package software.wings.beans.alert;

import io.harness.persistence.PersistentEntity;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Entity(value = "notificationRulesStatuses", noClassnameStored = true)
public class NotificationRulesStatus implements PersistentEntity {
  @Id private String accountId;
  private boolean enabled;
}

package software.wings.beans.alert;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.alerts.AlertCategory;
import software.wings.beans.Base;

import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
@Entity(value = "alertNotificationRules")
public class AlertNotificationRule extends Base {
  @NonNull @Indexed private final String accountId;
  private AlertCategory alertCategory;
  private AlertFilter alertFilter;
  @NonNull private Set<String> userGroupsToNotify;
  private boolean isDefault;

  public boolean hasAlertFilter() {
    return alertFilter != null;
  }
}
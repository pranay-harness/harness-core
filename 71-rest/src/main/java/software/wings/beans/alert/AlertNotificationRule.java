package software.wings.beans.alert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.alerts.AlertCategory;
import software.wings.beans.Base;

import java.util.Collections;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "alertNotificationRules", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class AlertNotificationRule extends Base implements AccountAccess {
  public static final String ALERT_CATEGORY = "alertCategory";

  @Indexed @Setter String accountId;
  @Indexed AlertCategory alertCategory;
  AlertFilter alertFilter;
  @NonNull Set<String> userGroupsToNotify = Collections.emptySet();

  public boolean isDefault() {
    return alertCategory == AlertCategory.All;
  }

  public boolean hasAlertFilter() {
    return alertFilter != null;
  }
}
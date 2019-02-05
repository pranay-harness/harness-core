package software.wings.resources.alert;

import lombok.Value;
import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertSeverity;

/**
 * Just a wrapper model over {@link software.wings.beans.alert.AlertType} enum so that Jackson serializes getters to
 * fields.
 */
@Value
class AlertType {
  private software.wings.beans.alert.AlertType alertType;

  public AlertCategory getCategory() {
    return alertType.getCategory();
  }

  public AlertSeverity getSeverity() {
    return alertType.getSeverity();
  }
}

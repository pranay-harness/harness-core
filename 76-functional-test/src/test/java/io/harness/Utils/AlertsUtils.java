package io.harness.Utils;

import io.harness.notifications.beans.Conditions;
import io.harness.notifications.beans.Conditions.Operator;
import software.wings.alerts.AlertCategory;
import software.wings.beans.alert.AlertFilter;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.beans.alert.AlertType;

import java.util.List;
import java.util.Set;

public class AlertsUtils {
  public static AlertNotificationRule createAlertNotificationRule(
      String accountId, Set<String> userGroups, AlertCategory alertCategory, AlertType alertType) {
    Conditions conditions = new Conditions(Operator.MATCHING, null, null);
    AlertFilter alertFilter = new AlertFilter(alertType, conditions);
    AlertNotificationRule alertRule = new AlertNotificationRule(accountId, alertCategory, alertFilter, userGroups);
    return alertRule;
  }

  public static boolean isAlertAvailable(List<AlertNotificationRule> alertsList, AlertNotificationRule alertToCheck) {
    for (AlertNotificationRule alertNotificationRule : alertsList) {
      if (alertToCheck.getUuid().equals(alertNotificationRule.getUuid())) {
        return true;
      }
    }
    return false;
  }
}

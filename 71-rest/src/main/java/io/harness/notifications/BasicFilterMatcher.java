package io.harness.notifications;

import io.harness.notifications.Condition.Operator;
import io.harness.notifications.beans.BaseCondition;
import lombok.Value;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertFilter;

/**
 * This matcher should be used there are no filters other than on alert type
 */
@Value
public class BasicFilterMatcher implements FilterMatcher {
  private AlertFilter alertFilter;
  private Alert alert;

  @Override
  public boolean matchesCondition() {
    boolean matches = alert.getType() == alertFilter.getAlertType();

    BaseCondition condition = (BaseCondition) alertFilter.getConditions();
    Operator op = condition.getOperator();

    switch (op) {
      case MATCHING:
        return matches;
      case NOT_MATCHING:
        return matches;
      default:
        throw new IllegalArgumentException("Unexpected value of alert filter operator: " + op);
    }
  }
}

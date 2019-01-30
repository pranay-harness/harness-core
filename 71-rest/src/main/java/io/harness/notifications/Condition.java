package io.harness.notifications;

/**
 * Any class implementing an alert filter should implement this interface.
 * These filters are used to determine who should be notified for a particular alert.
 *
 * See {@link software.wings.beans.alert.AlertNotificationRule} for more context.
 */
public interface Condition {
  enum Operator { MATCHING, NOT_MATCHING }

  Operator getOperator();
}

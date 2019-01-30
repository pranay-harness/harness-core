package io.harness.notifications.beans;

import io.harness.notifications.Condition;
import lombok.Value;

import javax.annotation.Nonnull;

/**
 * Basic condition which only checks for alert type in an Alert with alert type in AlertNotificationRule.
 */
@Value
public class BaseCondition implements Condition {
  @Nonnull public Operator operator;
}

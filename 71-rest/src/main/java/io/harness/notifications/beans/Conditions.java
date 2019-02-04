package io.harness.notifications.beans;

import lombok.Value;

import javax.annotation.Nonnull;

/**
 * Basic condition which only checks for alert type in an Alert with alert type in AlertNotificationRule.
 */
@Value
public class Conditions {
  @Nonnull private Operator operator;

  public enum Operator { MATCHING, NOT_MATCHING }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notifications;

import io.harness.notifications.beans.Conditions;
import io.harness.notifications.beans.Conditions.Operator;

import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertFilter;

import lombok.Value;

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

    Conditions conditions = alertFilter.getConditions();
    Operator op = conditions.getOperator();

    switch (op) {
      case MATCHING:
        return matches;
      case NOT_MATCHING:
        return !matches;
      default:
        throw new IllegalArgumentException("Unexpected value of alert filter operator: " + op);
    }
  }
}

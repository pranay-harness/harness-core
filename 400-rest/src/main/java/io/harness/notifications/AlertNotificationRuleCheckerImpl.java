/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notifications;

import io.harness.notifications.conditions.CVFilterMatcher;
import io.harness.notifications.conditions.ManualInterventionFilterMatcher;

import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertFilter;
import software.wings.beans.alert.AlertNotificationRule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlertNotificationRuleCheckerImpl implements AlertNotificationRuleChecker {
  @Override
  public boolean doesAlertSatisfyRule(Alert alert, AlertNotificationRule rule) {
    if (rule.getAlertCategory() != alert.getCategory()) {
      log.debug("Alert category and rule category don't match. Alert does not satisfy rule. Alert: {}, Rule: {}", alert,
          rule);
      return false;
    }

    if (null == rule.getAlertFilter()) {
      log.debug(
          "[Missing Alert Filter] Alert matched the rule because rule category and alert category are same and no additional filters are present. Rule: {}, Alert: {}",
          alert, rule);
      return rule.getAlertCategory() == alert.getCategory();
    }

    AlertFilter filter = rule.getAlertFilter();
    return alertSatisfiesFilter(alert, filter);
  }

  private boolean alertSatisfiesFilter(Alert alert, AlertFilter filter) {
    FilterMatcher matcher = null;

    // Matcher should be based on type on alert. Basic Filter Matcher will just compare alert type in alert with
    // alertType in rule. But for certain alerts like AlertType.SSOSyncFailedAlert , we might need custom checker which
    // will check that `ssoId` in rule is same as `ssoId` in alert.
    switch (alert.getType()) {
      case ManualInterventionNeeded:
        matcher = new ManualInterventionFilterMatcher(filter, alert);
        break;
      case CONTINUOUS_VERIFICATION_ALERT:
        matcher = new CVFilterMatcher(filter, alert);
        break;
      default:
        matcher = new BasicFilterMatcher(filter, alert);
    }

    return matcher.matchesCondition();
  }
}

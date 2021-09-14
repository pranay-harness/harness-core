/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notifications;

import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertNotificationRule;

/**
 * This aims to answers the question
 * "Does this alert satisfy this notification rule?"
 */

public interface AlertNotificationRuleChecker {
  /**
   * Does give alert satisfy given notification rule?
   * @return true, if yes.
   */
  boolean doesAlertSatisfyRule(Alert alert, AlertNotificationRule rule);
}

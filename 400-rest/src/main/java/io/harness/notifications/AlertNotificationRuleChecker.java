/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

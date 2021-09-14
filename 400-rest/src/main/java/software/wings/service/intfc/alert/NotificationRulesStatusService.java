/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.alert;

import software.wings.beans.alert.NotificationRulesStatus;

import javax.annotation.Nonnull;

/**
 * This is used to enable/disable notification rules for an account.
 * Originated from <a href="https://harness.atlassian.net/browse/PL-1384">PL-1384</a> as a way to disable some alerts.
 */
public interface NotificationRulesStatusService {
  /**
   * Get the status for an account.
   */
  @Nonnull NotificationRulesStatus get(String accountId);

  /**
   * Update the status in database.
   */
  NotificationRulesStatus update(String accountId, boolean enabled);
}

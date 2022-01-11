/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

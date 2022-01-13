/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.alert.AlertNotificationRule;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;
import javax.annotation.Nonnull;

@OwnedBy(PL)
public interface AlertNotificationRuleService extends OwnedByAccount {
  AlertNotificationRule create(AlertNotificationRule rule);

  AlertNotificationRule update(AlertNotificationRule rule);

  AlertNotificationRule createDefaultRule(String accountId);

  @Nonnull List<AlertNotificationRule> getAll(String accountId);

  void deleteById(String ruleId, String accountId);
}

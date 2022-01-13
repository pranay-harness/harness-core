/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notifications;

import static org.apache.commons.collections4.CollectionUtils.intersection;

import software.wings.beans.User;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.AlertNotificationRuleService;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class AlertVisibilityCheckerImpl implements AlertVisibilityChecker {
  @Inject private AlertNotificationRuleService ruleService;
  @Inject private AlertNotificationRuleChecker ruleChecker;
  @Inject private UserGroupService userGroupService;

  @Override
  public boolean shouldAlertBeShownToUser(String accountId, @Nonnull Alert alert, @Nonnull User user) {
    List<AlertNotificationRule> allRules = ruleService.getAll(accountId);

    boolean showAlertToUser = false;
    for (AlertNotificationRule rule : allRules) {
      if (showAlertToUser) {
        break;
      }

      showAlertToUser = ruleChecker.doesAlertSatisfyRule(alert, rule)
          && isUserPresentInUserGroups(accountId, rule.getUserGroupsToNotify(), user);
    }

    return showAlertToUser;
  }

  private boolean isUserPresentInUserGroups(String accountId, Set<String> userGroupIds, User user) {
    List<String> userGroupsWithCurrentUser = userGroupService.listByAccountId(accountId, user, true)
                                                 .stream()
                                                 .map(UserGroup::getUuid)
                                                 .collect(Collectors.toList());

    // if the intersection of userGroupIds matching the alertNotificationRule and userGroupIds with current user
    // is not empty, it means that user should be shown given alert.
    return !intersection(userGroupIds, userGroupsWithCurrentUser).isEmpty();
  }
}

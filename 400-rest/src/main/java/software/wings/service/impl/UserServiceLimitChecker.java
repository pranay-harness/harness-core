/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.lib.StaticLimit;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

@OwnedBy(PL)
@Slf4j
@Singleton
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class UserServiceLimitChecker {
  @Inject private AccountService accountService;
  @Inject private LimitConfigurationService limits;

  public void limitCheck(String accountId, List<User> existingUsersAndInvites, Set<String> emailsToInvite) {
    Account account = accountService.get(accountId);
    if (null == account) {
      log.error("No account found for accountId={}", accountId);
      return;
    }

    ConfiguredLimit limit = limits.getOrDefault(accountId, ActionType.CREATE_USER);
    if (null == limit) {
      log.error("No user limit configured. accountId={}", accountId);
      return;
    }

    StaticLimit userLimit = (StaticLimit) limit.getLimit();
    if (existingUsersAndInvites.size() + emailsToInvite.size() > userLimit.getCount()) {
      String err = MessageFormatter.format("You can only add upto {} users.", userLimit.getCount()).getMessage();
      throw new WingsException(ErrorCode.USAGE_LIMITS_EXCEEDED, err, USER);
    }
  }
}

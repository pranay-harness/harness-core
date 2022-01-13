/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.Account;
import software.wings.beans.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Vaibhav Tulsyan
 * 21/May/2019
 */
@OwnedBy(PL)
@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class DomainWhitelistCheckerService {
  @Inject private AuthenticationUtils authenticationUtils;

  public boolean isDomainWhitelisted(User user) {
    Account primaryAccount = authenticationUtils.getDefaultAccount(user);
    if (null == primaryAccount) {
      log.warn("User with UUID {} has null primary account", user.getUuid());
      return false;
    }

    return isDomainWhitelistedInternal(user, primaryAccount);
  }

  private boolean isDomainWhitelistedInternal(User user, Account account) {
    Set<String> filter = account.getWhitelistedDomains();

    if (isEmpty(filter)) {
      return true;
    }

    String email = user.getEmail();

    if (isEmpty(email)) {
      log.warn("Empty email received from account {}", account.getUuid());
      return false;
    }

    String domain = email.substring(email.indexOf('@') + 1);

    if (!filter.contains(domain)) {
      log.info("Domain filter was: [{}] while the email was: [{}]", filter, email);
      return false;
    }
    return true;
  }

  boolean isDomainWhitelisted(User user, Account account) {
    if (null == account) {
      log.warn("Account received was null for user with UUID {}", user.getUuid());
      return false;
    }
    return isDomainWhitelistedInternal(user, account);
  }

  public void throwDomainWhitelistFilterException() {
    throw new WingsException(ErrorCode.DOMAIN_WHITELIST_FILTER_CHECK_FAILED,
        "Domain name filter failed. Please contact your Account Administrator", USER);
  }
}

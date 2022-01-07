/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features.api;

import software.wings.beans.AccountType;
import software.wings.service.intfc.AccountService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRestrictedCeFeature extends AbstractRestrictedFeature {
  public AbstractRestrictedCeFeature(AccountService accountService, FeatureRestrictions featureRestrictions) {
    super(accountService, featureRestrictions);
  }

  @Override
  protected String getAccountType(String accountId) {
    String accountType = accountService.getCeAccountType(accountId).orElse(AccountType.PAID);
    log.info("Account type: {}", accountType);
    return accountService.getCeAccountType(accountId).orElse(AccountType.PAID);
  }

  @Override
  public Restrictions getRestrictionsForAccount(String accountId) {
    return getRestrictions(getAccountType(accountId));
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

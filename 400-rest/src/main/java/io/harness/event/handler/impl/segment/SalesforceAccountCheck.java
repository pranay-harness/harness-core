/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.handler.impl.segment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.Account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesforceAccountCheck {
  @Inject SalesforceApiCheck salesforceApiCheck;

  public boolean isAccountPresentInSalesforce(Account account) {
    if (account == null || account.getAccountName() == null) {
      log.error("Account to be checked in Salesforce is null");
      return false;
    }
    return getSalesforceAccountInternal(account);
  }

  private boolean getSalesforceAccountInternal(Account account) {
    return salesforceApiCheck.isPresentInSalesforce(account);
  }
}

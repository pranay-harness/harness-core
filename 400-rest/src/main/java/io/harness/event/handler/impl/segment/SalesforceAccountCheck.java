/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

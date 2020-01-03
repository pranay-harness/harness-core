package io.harness.event.handler.impl.segment;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;

@Slf4j
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesforceAccountCheck {
  @Inject SalesforceApiCheck salesforceApiCheck;

  public boolean isAccountPresentInSalesforce(Account account) {
    if (account == null || account.getAccountName() == null) {
      logger.error("Account to be checked in Salesforce is null");
      return false;
    }
    return getSalesforceAccountInternal(account);
  }

  private boolean getSalesforceAccountInternal(Account account) {
    return salesforceApiCheck.isPresentInSalesforce(account);
  }
}

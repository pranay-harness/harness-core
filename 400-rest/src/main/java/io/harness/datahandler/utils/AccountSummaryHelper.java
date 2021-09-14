/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.datahandler.utils;

import io.harness.datahandler.models.AccountSummary;

import software.wings.beans.Account;

import java.util.List;

public interface AccountSummaryHelper {
  List<AccountSummary> getAccountSummariesFromAccounts(List<Account> accounts);
  AccountSummary getAccountSummaryFromAccount(Account account);
}

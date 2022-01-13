/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.datahandler.utils;

import io.harness.datahandler.models.AccountSummary;

import software.wings.beans.Account;

import java.util.List;

public interface AccountSummaryHelper {
  List<AccountSummary> getAccountSummariesFromAccounts(List<Account> accounts);
  AccountSummary getAccountSummaryFromAccount(Account account);
}

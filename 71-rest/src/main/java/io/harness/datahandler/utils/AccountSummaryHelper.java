package io.harness.datahandler.utils;

import io.harness.datahandler.models.AccountSummary;
import software.wings.beans.Account;

import java.util.List;

public interface AccountSummaryHelper {
  List<AccountSummary> getAccountSummariesFromAccounts(List<Account> accounts);
  AccountSummary getAccountSummaryFromAccount(Account account);
}

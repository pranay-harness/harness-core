package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.migrations.Migration;

import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.List;

public class FetchAndSaveAccounts implements Migration {
  @Inject private AccountService accountService;

  /**
   * licenseInfo was previously marked with @Transient in {@link Account} model.
   * {@code @Transient} annotation prevents field from being saved in database.
   *
   * That annotation was removed.
   *
   * Just fetching and saving all accounts so that the removal of transient annotation is affected in database.
   */
  @Override
  public void migrate() {
    List<Account> allAccounts = accountService.listAllAccounts();
    for (Account account : allAccounts) {
      accountService.update(account);
    }
  }
}

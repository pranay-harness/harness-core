package io.harness.migrations.all;

import io.harness.migrations.Migration;

import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FetchAndSaveAccounts2 implements Migration {
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
      try {
        log.info("Updating account. accountId={}", account.getUuid());
        if (null == account.getLicenseInfo()) {
          log.info("license info is null. accountId={}", account.getUuid());
        } else {
          accountService.update(account);
        }
      } catch (Exception e) {
        log.error("Error updating account", e);
      }
    }
  }
}

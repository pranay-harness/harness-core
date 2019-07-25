package migrations.all;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import java.util.List;

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
        logger.info("Updating account. accountId={}", account.getUuid());
        if (null == account.getLicenseInfo()) {
          logger.info("license info is null. accountId={}", account.getUuid());
        } else {
          accountService.update(account);
        }
      } catch (Exception e) {
        logger.error("Error updating account", e);
      }
    }
  }
}

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
    try {
      List<Account> allAccounts = accountService.listAllAccounts();
      for (Account account : allAccounts) {
        logger.info("Fetching and saving account. accountId={}", account.getUuid());
        accountService.save(account);
      }
    } catch (Exception e) {
      logger.error("Error saving accounts.", e);
    }
  }
}

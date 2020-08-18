package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.beans.Account.AccountKeys;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;

@Slf4j
public class AddHarnessGroupAccessFlagToAccount implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Starting migration for adding isHarnessSupportAccessAllowed flag to accounts");

    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority)
                                                           .project(AccountKeys.isHarnessSupportAccessAllowed, true)
                                                           .fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        wingsPersistence.updateField(
            Account.class, account.getUuid(), AccountKeys.isHarnessSupportAccessAllowed, Boolean.TRUE);
      }
    } catch (Exception e) {
      logger.error("Error happened while adding isHarnessSupportAccessAllowed flag to accounts", e);
    }
  }
}

package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.UsageRestrictionsService;

/**
 * @author marklu on 11/5/18
 */
@Slf4j
public class DanglingAppEnvReferenceRemovalMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UsageRestrictionsService usageRestrictionsService;

  @Override
  public void migrate() {
    try (HIterator<Account> accountHIterator = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (accountHIterator.hasNext()) {
        Account account = accountHIterator.next();
        try {
          int purgeCount = usageRestrictionsService.purgeDanglingAppEnvReferences(account.getUuid());
          logger.info(
              "{} usage restrictions referring to non-existent application/environment have been fixed in account {}",
              purgeCount, account.getUuid());
        } catch (Exception e) {
          logger.error(
              "Failed to purge dangling references in usage restrictions to application/environments in account "
                  + account.getUuid(),
              e);
        }
      }
    }
  }
}

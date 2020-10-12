package migrations.all;

import static software.wings.beans.Account.AccountKeys;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;

@Slf4j
public class LicenseExpiryReminderTimeMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Starting migration of lastLicenseExpiryReminderSentAt field.");
    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createAuthorizedQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        long lastLicenseExpiryReminderSentAt = account.getLastLicenseExpiryReminderSentAt();
        if (lastLicenseExpiryReminderSentAt != 0L) {
          UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);
          updateOperations.push(AccountKeys.licenseExpiryRemindersSentAt, lastLicenseExpiryReminderSentAt);
          wingsPersistence.update(account, updateOperations);
        }
      }
      logger.info("Migration of lastLicenseExpiryReminderSentAt field is finished.");
    }
  }
}

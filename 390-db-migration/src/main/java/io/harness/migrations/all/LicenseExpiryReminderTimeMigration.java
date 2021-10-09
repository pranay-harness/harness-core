package io.harness.migrations.all;

import static software.wings.beans.Account.AccountKeys;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class LicenseExpiryReminderTimeMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Starting migration of lastLicenseExpiryReminderSentAt field.");
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
      log.info("Migration of lastLicenseExpiryReminderSentAt field is finished.");
    }
  }
}

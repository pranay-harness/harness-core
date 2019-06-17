package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.dl.WingsPersistence;

/**
 * Running this migration creates default entry in the login settings table for each account.
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddLoginSettingsToAccountMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;
  @Inject LoginSettingsService loginSettingsService;

  @Override
  public void migrate() {
    logger.info("Starting AddLoginSettingsToAccountMigration migration for all accounts.");
    try {
      HIterator<Account> accountIterator =
          new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority).fetch());

      while (accountIterator.hasNext()) {
        Account account = accountIterator.next();
        try {
          HIterator<LoginSettings> loginSettingsHIterator =
              new HIterator<>(wingsPersistence.createQuery(LoginSettings.class, excludeAuthority)
                                  .field("accountId")
                                  .equal(account.getUuid())
                                  .fetch());

          // There can be only one settings for an account.
          if (loginSettingsHIterator.hasNext()) {
            logger.info("Login settings already exist for account: {}. Skipping it.", account.getUuid());
            continue;
          }
          loginSettingsService.createDefaultLoginSettings(account);
        } catch (Exception exceptionInWhileLoop) {
          logger.error("Login settings migration failed for account id: {}", account.getUuid(), exceptionInWhileLoop);
        }
      }
    } catch (Exception ex) {
      logger.error("AddLoginSettingsToAccountMigration migration failed.", ex);
    }
  }
}

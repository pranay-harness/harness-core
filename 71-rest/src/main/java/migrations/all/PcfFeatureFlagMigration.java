package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

@Slf4j
public class PcfFeatureFlagMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public void migrate() {
    logger.info("Retrieving Accounts");

    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        String accountId = account.getUuid();

        if (!featureFlagService.isEnabled(FeatureName.USE_PCF_CLI, accountId)) {
          logger.info("Enabling USE_PCF_CLI for accountId: " + accountId);
          featureFlagService.enableAccount(FeatureName.USE_PCF_CLI, accountId);
        }
      }
    } catch (Exception e) {
      logger.warn("Something failed in PcfFeatureFlagMigration Migration", e);
    }
  }
}

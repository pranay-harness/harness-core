package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.OnPrimaryManagerMigration;
import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.sync.YamlService;

@Slf4j
public class TemplateLibraryYamlOnPrimaryManagerMigration implements OnPrimaryManagerMigration {
  private static final String DEBUG_LINE = "TEMPLATE_YAML_SUPPORT: ";

  @Inject YamlService yamlService;
  @Inject WingsPersistence wingsPersistence;
  @Inject FeatureFlagService featureFlagService;

  @Override
  public void migrate() {
    logger.info(String.join(DEBUG_LINE, " Starting Migration For Template Library Yaml"));
    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        if (!featureFlagService.isEnabled(FeatureName.TEMPLATE_YAML_SUPPORT, account.getUuid())) {
          logger.info(String.join(
              DEBUG_LINE, " Starting Migration For Template Library Yaml for account", account.getAccountName()));
          featureFlagService.enableAccount(FeatureName.TEMPLATE_YAML_SUPPORT, account.getUuid());
          yamlService.syncYamlTemplate(account.getUuid());
        } else {
          logger.info(String.join(DEBUG_LINE,
              " Migration For Template Library Yaml for account {} already completed in last migration.",
              account.getAccountName()));
        }
      }
    }

    logger.info(String.join(DEBUG_LINE, " Completed triggering migration for Template Library Yaml"));
  }
}

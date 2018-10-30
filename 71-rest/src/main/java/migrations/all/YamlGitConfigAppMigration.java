package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlGitConfig;

public class YamlGitConfigAppMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(YamlGitConfigAppMigration.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private YamlGitService yamlGitService;
  @Inject private EncryptionService encryptionService;
  @Inject private SecretManager secretManager;

  @Override
  public void migrate() {
    logger.info("Running YamlGitConfigAppMigration");

    try (HIterator<Account> accountHIterator =
             new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority).fetch())) {
      while (accountHIterator.hasNext()) {
        String accountId = accountHIterator.next().getUuid();

        YamlGitConfig yamlGitConfig = yamlGitService.get(accountId, accountId, EntityType.ACCOUNT);

        if (yamlGitConfig != null) {
          logger.info("Retrieving applications for accountId " + accountId);
          try (HIterator<Application> apps = new HIterator<>(
                   wingsPersistence.createQuery(Application.class).filter(ACCOUNT_ID_KEY, accountId).fetch())) {
            while (apps.hasNext()) {
              Application application = apps.next();
              saveYamlGitConfigForApp(application, yamlGitConfig);
            }
          }
          logger.info("Done updating applications for accountId " + accountId);
        }
      }
    }

    logger.info("Completed running YamlGitConfigAppMigration");
  }

  private void saveYamlGitConfigForApp(Application app, YamlGitConfig yamlGitConfig) {
    YamlGitConfig savedYamlGitConfig = yamlGitService.get(app.getAccountId(), app.getUuid(), EntityType.APPLICATION);
    if (savedYamlGitConfig != null) {
      return;
    }

    encryptionService.decrypt(
        yamlGitConfig, secretManager.getEncryptionDetails(yamlGitConfig, "_GLOBAL_APP_ID_", null));

    yamlGitConfig.setUuid(null);
    yamlGitConfig.setAppId(app.getUuid());
    yamlGitConfig.setEntityType(EntityType.APPLICATION);
    yamlGitConfig.setEntityId(app.getUuid());

    wingsPersistence.save(yamlGitConfig);
  }
}

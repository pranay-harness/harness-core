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
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

public class YamlGitConfigAppMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(YamlGitConfigAppMigration.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private YamlGitService yamlGitService;

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

    YamlGitConfig newYamlGitConfig = YamlGitConfig.builder()
                                         .enabled(true)
                                         .syncMode(SyncMode.BOTH)
                                         .entityId(app.getUuid())
                                         .entityType(EntityType.APPLICATION)
                                         .gitConnectorId(yamlGitConfig.getGitConnectorId())
                                         .branchName(yamlGitConfig.getBranchName())
                                         .accountId(yamlGitConfig.getAccountId())
                                         .build();
    newYamlGitConfig.setAppId(app.getUuid());

    wingsPersistence.save(newYamlGitConfig);
  }
}

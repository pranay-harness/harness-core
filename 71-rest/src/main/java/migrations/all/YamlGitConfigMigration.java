package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

public class YamlGitConfigMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(YamlGitConfigMigration.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private SecretManager secretManager;
  @Inject private EncryptionService encryptionService;

  @Override
  public void migrate() {
    logger.info("Running YamlGitConfigMigration");

    try (HIterator<YamlGitConfig> yamlGitConfigHIterator =
             new HIterator<>(wingsPersistence.createQuery(YamlGitConfig.class).fetch())) {
      while (yamlGitConfigHIterator.hasNext()) {
        YamlGitConfig yamlGitConfig = yamlGitConfigHIterator.next();
        encryptionService.decrypt(
            yamlGitConfig, secretManager.getEncryptionDetails(yamlGitConfig, "_GLOBAL_APP_ID_", null));
        updateYamlGitConfig(yamlGitConfig);
      }
    }

    logger.info("Completed running YamlGitConfigMigration");
  }

  private void updateYamlGitConfig(YamlGitConfig yamlGitConfig) {
    yamlGitConfig.setSyncMode(SyncMode.BOTH);
    yamlGitConfig.setEntityType(EntityType.ACCOUNT);
    yamlGitConfig.setEntityId(yamlGitConfig.getAccountId());
    wingsPersistence.saveAndGet(YamlGitConfig.class, yamlGitConfig);
  }
}

package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.yaml.gitSync.YamlGitConfig.ENTITY_ID_KEY;
import static software.wings.yaml.gitSync.YamlGitConfig.ENTITY_TYPE_KEY;
import static software.wings.yaml.gitSync.YamlGitConfig.SYNC_MODE_KEY;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

import java.util.HashMap;
import java.util.Map;

public class YamlGitConfigMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(YamlGitConfigMigration.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Running YamlGitConfigMigration");

    try (HIterator<YamlGitConfig> yamlGitConfigHIterator =
             new HIterator<>(wingsPersistence.createQuery(YamlGitConfig.class).fetch())) {
      while (yamlGitConfigHIterator.hasNext()) {
        YamlGitConfig yamlGitConfig = yamlGitConfigHIterator.next();
        updateYamlGitConfig(yamlGitConfig);
      }
    }

    logger.info("Completed running YamlGitConfigMigration");
  }

  private void updateYamlGitConfig(YamlGitConfig yamlGitConfig) {
    if (isEmpty(yamlGitConfig.getEntityId()) && yamlGitConfig.getEntityType() == null) {
      Map<String, Object> keyValuePairs = new HashMap<>();
      keyValuePairs.put(SYNC_MODE_KEY, SyncMode.BOTH);
      keyValuePairs.put(ENTITY_ID_KEY, yamlGitConfig.getAccountId());
      keyValuePairs.put(ENTITY_TYPE_KEY, EntityType.ACCOUNT);

      wingsPersistence.updateFields(YamlGitConfig.class, yamlGitConfig.getUuid(), keyValuePairs);
    }
  }
}

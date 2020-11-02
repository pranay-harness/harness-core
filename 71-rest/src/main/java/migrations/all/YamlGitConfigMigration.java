package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.yaml.gitSync.YamlGitConfig.ENTITY_ID_KEY;
import static software.wings.yaml.gitSync.YamlGitConfig.ENTITY_TYPE_KEY;
import static software.wings.yaml.gitSync.YamlGitConfig.SYNC_MODE_KEY;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.EntityType;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class YamlGitConfigMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Running YamlGitConfigMigration");

    try (HIterator<YamlGitConfig> yamlGitConfigHIterator =
             new HIterator<>(wingsPersistence.createQuery(YamlGitConfig.class).fetch())) {
      for (YamlGitConfig yamlGitConfig : yamlGitConfigHIterator) {
        updateYamlGitConfig(yamlGitConfig);
      }
    }

    log.info("Completed running YamlGitConfigMigration");
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

package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.EntityType;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.YamlGitConfigKeys;

@Slf4j
public class GitSyncErrorGitDetailsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  private Table<String, String, YamlGitConfig> yamlGitConfigTable = HashBasedTable.create();

  @Override
  public void migrate() {
    logger.info("Running migration GitSyncErrorGitDetailsMigration");

    Query<GitSyncError> query = wingsPersistence.createQuery(GitSyncError.class);

    try (HIterator<GitSyncError> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        GitSyncError syncError = records.next();
        try {
          // already populated
          if (isNotEmpty(syncError.getGitConnectorId())) {
            continue;
          }

          final String accountId = syncError.getAccountId();
          final String appId = syncError.getAppId();
          if (isEmpty(appId)) {
            logger.info("SKipping GitSyncError id=[{}], as app id is absent", syncError.getUuid());
            continue;
          }
          final YamlGitConfig yamlGitConfig = getYamlGitConfig(accountId, appId);
          if (yamlGitConfig != null) {
            syncError.setGitConnectorId(yamlGitConfig.getGitConnectorId());
            syncError.setBranchName(yamlGitConfig.getBranchName());
            syncError.setYamlGitConfigId(yamlGitConfig.getUuid());
            wingsPersistence.save(syncError);
            logger.info(
                "Updated GitSyncError id= [{}], with GitConnectorId= [{}], BranchName =[{}], YamlGitConfigId=[{}]",
                syncError.getUuid(), syncError.getGitConnectorId(), syncError.getBranchName(),
                syncError.getYamlGitConfigId());
          }
        } catch (Exception e) {
          logger.error("Error while processing gitsyncerror id =" + syncError.getUuid(), e);
        }
      }
    }
    logger.info("Completed migration:  GitSyncErrorGitDetailsMigration");
  }

  private YamlGitConfig getYamlGitConfig(String accountId, String appId) {
    YamlGitConfig yamlGitConfig = yamlGitConfigTable.get(accountId, appId);
    if (yamlGitConfig == null) {
      final Query<YamlGitConfig> query = wingsPersistence.createQuery(YamlGitConfig.class)
                                             .filter(YamlGitConfigKeys.accountId, accountId)
                                             .filter(YamlGitConfigKeys.enabled, Boolean.TRUE);
      if (appId.equals(GLOBAL_APP_ID)) {
        query.filter(YamlGitConfigKeys.entityType, EntityType.ACCOUNT).filter(YamlGitConfigKeys.entityId, accountId);
      } else {
        query.filter(YamlGitConfigKeys.entityType, EntityType.APPLICATION).filter(YamlGitConfigKeys.entityId, appId);
      }
      yamlGitConfig = query.get();
      if (yamlGitConfig != null) {
        yamlGitConfigTable.put(accountId, appId, yamlGitConfig);
      }
    }
    return yamlGitConfig;
  }
}
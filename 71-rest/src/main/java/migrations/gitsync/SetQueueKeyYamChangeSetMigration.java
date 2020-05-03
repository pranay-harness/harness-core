package migrations.gitsync;

import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.OnPrimaryManagerMigration;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.yaml.gitSync.GitChangeSetRunnableHelper;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlChangeSet.YamlChangeSetKeys;

@Slf4j
public class SetQueueKeyYamChangeSetMigration implements OnPrimaryManagerMigration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private GitChangeSetRunnableHelper gitChangeSetRunnableHelper;

  @Override
  public void migrate() {
    logger.info("Running migration SetQueueKeyYamChangeSetMigration");

    gitChangeSetRunnableHelper.handleOldQueuedChangeSets(wingsPersistence);

    populateQueueKeyAndMetadata();

    logger.info("Completed migration SetQueueKeyYamChangeSetMigration");
  }

  private void populateQueueKeyAndMetadata() {
    Query<YamlChangeSet> query = wingsPersistence.createQuery(YamlChangeSet.class, excludeAuthority)
                                     .field(YamlChangeSetKeys.status)
                                     .in(ImmutableList.of(Status.QUEUED, Status.RUNNING));

    try (HIterator<YamlChangeSet> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        YamlChangeSet yamlChangeSet = records.next();
        try {
          // Is the record already being processed ? If yes then skip
          if (StringUtils.isBlank(yamlChangeSet.getQueueKey()) || yamlChangeSet.getGitSyncMetadata() == null) {
            logger.info("Updating yamlchangeset id = [{}]", yamlChangeSet.getUuid());
            yamlChangeSetService.populateGitSyncMetadata(yamlChangeSet);
            updateRequiredFieldsOnly(yamlChangeSet);
            logger.info("Successfully updated yamlchangeset id = [{}]", yamlChangeSet.getUuid());
          }
        } catch (Exception e) {
          logger.error("Error while updating yamlchangeset id =" + yamlChangeSet.getUuid(), e);
        }
      }
    }
  }

  void updateRequiredFieldsOnly(YamlChangeSet yamlChangeSetWithMetadata) {
    final UpdateOperations<YamlChangeSet> updateOperations =
        wingsPersistence.createUpdateOperations(YamlChangeSet.class);
    setUnset(updateOperations, YamlChangeSetKeys.gitSyncMetadata, yamlChangeSetWithMetadata.getGitSyncMetadata());
    setUnset(updateOperations, YamlChangeSetKeys.queueKey, yamlChangeSetWithMetadata.getQueueKey());
    wingsPersistence.update(yamlChangeSetWithMetadata, updateOperations);
  }
}

package software.wings.service.impl.yaml;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.time.Duration;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 10/31/17.
 */
@Singleton
@ValidateOnExecution
public class YamlChangeSetServiceImpl implements YamlChangeSetService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;

  private static final Logger logger = LoggerFactory.getLogger(YamlChangeSetServiceImpl.class);

  @Override
  public YamlChangeSet save(YamlChangeSet yamlChangeSet) {
    return wingsPersistence.saveAndGet(YamlChangeSet.class, yamlChangeSet);
  }

  @Override
  public YamlChangeSet get(String accountId, String changeSetId) {
    return wingsPersistence.createQuery(YamlChangeSet.class)
        .field("accountId")
        .equal(accountId)
        .field(Mapper.ID_KEY)
        .equal(changeSetId)
        .get();
  }

  @Override
  public void update(YamlChangeSet yamlChangeSet) {
    UpdateOperations<YamlChangeSet> updateOperations =
        wingsPersistence.createUpdateOperations(YamlChangeSet.class).set("status", yamlChangeSet.getStatus());
    wingsPersistence.update(yamlChangeSet, updateOperations);
  }

  @Override
  public synchronized YamlChangeSet getQueuedChangeSet(String accountId) {
    try (AcquiredLock lock = persistentLocker.acquireLock(YamlChangeSet.class, accountId, Duration.ofMinutes(1))) {
      Query<YamlChangeSet> findQuery = wingsPersistence.createQuery(YamlChangeSet.class)
                                           .field("accountId")
                                           .equal(accountId)
                                           .field("status")
                                           .equal(Status.QUEUED)
                                           .order("createdAt");
      UpdateOperations<YamlChangeSet> updateOperations =
          wingsPersistence.createUpdateOperations(YamlChangeSet.class).set("status", Status.RUNNING);
      YamlChangeSet modifiedChangeSet = wingsPersistence.getDatastore().findAndModify(findQuery, updateOperations);

      if (modifiedChangeSet == null) {
        logger.info("No change set found in queued state");
      }
      return modifiedChangeSet;
    } catch (WingsException exception) {
      exception.logProcessedMessages();
    } catch (Exception exception) {
      logger.error("Error seen in fetching changeSet", exception);
    }
    return null;
  }

  @Override
  public boolean updateStatus(String accountId, String changeSetId, Status newStatus) {
    YamlChangeSet yamlChangeSet = get(accountId, changeSetId);
    if (yamlChangeSet != null) {
      UpdateResults status = wingsPersistence.update(
          yamlChangeSet, wingsPersistence.createUpdateOperations(YamlChangeSet.class).set("status", newStatus));
      return status.getUpdatedCount() != 0;
    } else {
      logger.warn("No YamlChangeSet found");
    }
    return false;
  }

  @Override
  public boolean deleteChangeSet(String accountId, String changeSetId) {
    return wingsPersistence.delete(wingsPersistence.createQuery(YamlChangeSet.class)
                                       .field("accountId")
                                       .equal(accountId)
                                       .field(Mapper.ID_KEY)
                                       .equal(changeSetId));
  }

  @Override
  public void saveChangeSet(YamlGitConfig yamlGitConfig, List<GitFileChange> gitFileChanges) {
    YamlChangeSet yamlChangeSet = YamlChangeSet.builder()
                                      .accountId(yamlGitConfig.getAccountId())
                                      .gitFileChanges(gitFileChanges)
                                      .status(Status.QUEUED)
                                      .queuedOn(System.currentTimeMillis())
                                      .build();
    yamlChangeSet.setAppId(Base.GLOBAL_APP_ID);
    save(yamlChangeSet);
  }
}

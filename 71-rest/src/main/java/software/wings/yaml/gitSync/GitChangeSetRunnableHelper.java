package software.wings.yaml.gitSync;

import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.yaml.gitSync.YamlChangeSet.MAX_QUEUE_DURATION_EXCEEDED_CODE;

import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlChangeSet.YamlChangeSetKeys;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class GitChangeSetRunnableHelper {
  private static final long TIMEOUT_FOR_RUNNING_CHANGESET = 90;

  public List<YamlChangeSet> getStuckYamlChangeSets(
      WingsPersistence wingsPersistence, List<String> runningAccountIdList) {
    return wingsPersistence.createQuery(YamlChangeSet.class, excludeAuthority)
        .field("accountId")
        .in(runningAccountIdList)
        .filter(YamlChangeSetKeys.status, Status.RUNNING.name())
        .field(YamlChangeSet.LAST_UPDATED_AT_KEY)
        .lessThan(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(TIMEOUT_FOR_RUNNING_CHANGESET))
        .project("_id", true)
        .project("accountId", true)
        .asList();
  }

  private static final BasicDBObject notQueuedStatusDBObject =
      new BasicDBObject("status", new BasicDBObject("$in", new String[] {Status.QUEUED.name()}));

  public List<String> getQueuedAccountIdList(WingsPersistence wingsPersistence) {
    return HPersistence.retry(
        () -> wingsPersistence.getCollection(YamlChangeSet.class).distinct("accountId", notQueuedStatusDBObject));
  }

  private static final BasicDBObject runningStatusDBObject = new BasicDBObject("status", Status.RUNNING.name());

  public List<String> getRunningAccountIdList(WingsPersistence wingsPersistence) {
    return HPersistence.retry(
        () -> wingsPersistence.getCollection(YamlChangeSet.class).distinct("accountId", runningStatusDBObject));
  }

  public void handleOldQueuedChangeSets(WingsPersistence wingsPersistence) {
    logger.info("Marking obsolete queued changesets as skipped");
    try {
      final UpdateResults update =
          wingsPersistence.update(wingsPersistence.createAuthorizedQuery(YamlChangeSet.class)
                                      .filter(YamlChangeSetKeys.status, Status.QUEUED)
                                      .field(CreatedAtAware.CREATED_AT_KEY)
                                      .lessThan(System.currentTimeMillis() - Duration.ofDays(3).toMillis()),
              wingsPersistence.createUpdateOperations(YamlChangeSet.class)
                  .set(YamlChangeSetKeys.status, Status.SKIPPED)
                  .set(YamlChangeSetKeys.messageCode, MAX_QUEUE_DURATION_EXCEEDED_CODE));
      logger.info("Successfully marked obsolete queued changesets with update results = [{}]", update);
    } catch (Exception e) {
      logger.error("Error while marking obsolete queued changesets as skipped", e);
    }
  }
}

package software.wings.yaml.gitSync;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class GitChangeSetRunnableHelper {
  private static final long TIMEOUT_FOR_RUNNING_CHANGESET = 90;

  public List<YamlChangeSet> getStuckYamlChangeSets(
      WingsPersistence wingsPersistence, List<String> runningAccountIdList) {
    return wingsPersistence.createQuery(YamlChangeSet.class, excludeAuthority)
        .field("accountId")
        .in(runningAccountIdList)
        .filter("status", Status.RUNNING.name())
        .field("lastUpdatedAt")
        .lessThan(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(TIMEOUT_FOR_RUNNING_CHANGESET))
        .project("_id", true)
        .project("accountId", true)
        .asList();
  }

  public List<String> getQueuedAccountIdList(WingsPersistence wingsPersistence) {
    return wingsPersistence.getDatastore()
        .getCollection(YamlChangeSet.class)
        .distinct("accountId",
            new BasicDBObject(
                "status", new BasicDBObject("$in", new String[] {Status.FAILED.name(), Status.QUEUED.name()})));
  }

  public List<String> getRunningAccountIdList(WingsPersistence wingsPersistence) {
    return wingsPersistence.getDatastore()
        .getCollection(YamlChangeSet.class)
        .distinct("accountId", new BasicDBObject("status", Status.RUNNING.name()));
  }
}

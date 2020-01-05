package software.wings.search.framework;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.PerpetualSearchLocker.LockTimeoutCallback;
import software.wings.search.framework.SearchDistributedLock.SearchDistributedLockKeys;

import java.time.Instant;
import java.util.Date;

/**
 * Update search lock heartbeat task
 *
 * @author utkarsh
 */
@AllArgsConstructor
@Slf4j
public class SearchHeartbeatMonitor implements Runnable {
  private WingsPersistence wingsPersistence;
  private LockTimeoutCallback lockTimeoutCallback;
  private String lockName;
  private String uuid;
  private ConfigurationController configurationController;

  @Override
  public void run() {
    Query<SearchDistributedLock> query = wingsPersistence.createQuery(SearchDistributedLock.class)
                                             .field(SearchDistributedLockKeys.name)
                                             .equal(lockName)
                                             .field(SearchDistributedLockKeys.uuid)
                                             .equal(uuid);

    SearchDistributedLock searchDistributedLock = query.get();
    Instant instant = Instant.now();

    if (searchDistributedLock != null && configurationController.isPrimary()) {
      wingsPersistence.updateField(
          SearchDistributedLock.class, lockName, SearchDistributedLockKeys.heartbeat, Date.from(instant));
    } else {
      logger.info("Search lock is deleted");
      lockTimeoutCallback.stop();
    }
  }
}

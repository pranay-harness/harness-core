package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.SearchDistributedLock.SearchDistributedLockKeys;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PerpetualSearchLocker {
  @Inject WingsPersistence wingsPersistence;
  @Inject private ScheduledExecutorService scheduledExecutorService;

  private boolean canAcquireLock(String lockName) {
    return wingsPersistence.get(SearchDistributedLock.class, lockName) != null;
  }

  private boolean isLockAcquired(String lockName, String uuid) {
    Query<SearchDistributedLock> query = wingsPersistence.createQuery(SearchDistributedLock.class)
                                             .field(SearchDistributedLockKeys.name)
                                             .equal(lockName)
                                             .field(SearchDistributedLockKeys.uuid)
                                             .equal(uuid);
    return query.get() != null;
  }

  private boolean acquireLock(String lockName, String uuid) {
    Instant instant = Instant.now();
    SearchDistributedLock searchDistributedLock =
        new SearchDistributedLock(lockName, uuid, Date.from(instant), instant.toEpochMilli());
    wingsPersistence.save(searchDistributedLock);
    return isLockAcquired(lockName, uuid);
  }

  public ScheduledFuture tryToAcquireLock(String lockName, String uuid, LockTimeoutCallback lockTimeoutCallback)
      throws InterruptedException {
    if (!isLockAcquired(lockName, uuid)) {
      logger.info("Attempting to acquire lock");
      boolean shouldAcquireLock = false;
      while (!shouldAcquireLock) {
        Thread.sleep(1000);
        shouldAcquireLock = !canAcquireLock(lockName);
      }
      if (!acquireLock(lockName, uuid)) {
        tryToAcquireLock(lockName, uuid, lockTimeoutCallback);
      }
    }
    logger.info("Search lock acquired");
    SearchHeartbeatMonitor searchHeartbeatMonitor =
        new SearchHeartbeatMonitor(wingsPersistence, lockTimeoutCallback, lockName, uuid);
    return scheduledExecutorService.scheduleAtFixedRate(searchHeartbeatMonitor, 0, 10, TimeUnit.SECONDS);
  }

  @FunctionalInterface
  public interface LockTimeoutCallback {
    void stop();
  }
}

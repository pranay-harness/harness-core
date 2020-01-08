package software.wings.scheduler.persistance;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.lock.mongo.MongoPersistentLocker.LOCKS_STORE;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsPersistence;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.ExecutorService;

@Slf4j
public class PersistentLockCleanup implements Runnable {
  public static final String NAME = "MAINTENANCE";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private ExecutorService executorService;

  public void deleteOld(OffsetDateTime date) {
    final BasicDBObject filter = new BasicDBObject()
                                     .append("lockState", "unlocked")
                                     .append("lastUpdated", new BasicDBObject("$lt", Date.from(date.toInstant())));
    wingsPersistence.getCollection(LOCKS_STORE, "locks").remove(filter);
  }

  public DBCursor queryOldLocks(OffsetDateTime date) {
    final BasicDBObject filter =
        new BasicDBObject().append("lastUpdated", new BasicDBObject("$lt", Date.from(date.toInstant())));
    return wingsPersistence.getCollection(LOCKS_STORE, "locks").find(filter).limit(1000);
  }

  @Override
  public void run() {
    logger.info("Running PersistentLockCleanup Job asynchronously and returning");

    executorService.submit(this ::executeInternal);
  }

  private void executeInternal() {
    int total = 0;
    int destroyed = 0;
    try (AcquiredLock lock = persistentLocker.acquireLock(PersistentLocker.class, NAME, Duration.ofMinutes(10))) {
      OffsetDateTime startTime = OffsetDateTime.now().minusWeeks(1);
      deleteOld(startTime);

      final DBCursor locks = queryOldLocks(startTime);

      while (locks.hasNext()) {
        total++;
        if (delete(locks.next().get("_id"))) {
          destroyed++;
        }
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException exception) {
      logger.error("Error seen in the PersistentLockCleanupJob execute call", exception);
    }
    logger.info("Destroyed {} locks out of {} outdated", destroyed, total);
  }

  private boolean delete(Object lock) {
    // Do not delete the lock willy-nilly. We are in race between the query for unlocked state, the deleting
    // and some other process attempting to lock the same locks.
    //
    // The lock needs to be deleted only if successfully acquired

    boolean destroyed = false;
    try (AcquiredLock lk = persistentLocker.acquireLock(lock.toString(), Duration.ofSeconds(10))) {
      destroyed = true;
      persistentLocker.destroy(lk);
    } catch (WingsException exception) {
      // Nothing to do. If we did not get the lock or we succeeded to destroy it - either way move to the
      // next one.
    }
    return destroyed;
  }
}

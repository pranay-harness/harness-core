package software.wings.lock;

import static io.harness.threading.Morpheus.sleep;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;

import java.time.Duration;

/**
 * The Class PersistentLockerTest.
 */
public class PersistentLockerDBTest extends WingsBaseTest {
  @Inject private DistributedLockSvc distributedLockSvc;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;

  @Test
  public void testAcquireLockDoLock() {
    try (AcquiredLock lock = persistentLocker.acquireLock("foo", Duration.ofSeconds(1))) {
    }

    final BasicDBObject filter = new BasicDBObject().append("_id", "foo");

    DBObject dbLock = wingsPersistence.getCollection("locks").findOne(filter);
    assertNotNull(dbLock);

    boolean damage = false;
    try (AcquiredLock lock = persistentLocker.acquireLock("foo", Duration.ofSeconds(1))) {
      persistentLocker.destroy(lock);
      damage = true;
    } catch (WingsException exception) {
      // Do nothing. This is just to suppress the exception
    }

    assertFalse(damage);

    dbLock = wingsPersistence.getCollection("locks").findOne(filter);
    assertNull(dbLock);
  }

  @Test
  @Ignore // The underlining code does not respect lock after timeout. Enable this test when this issue is fixed.
  public void testAcquireAfterTimeout() throws InterruptedException {
    Duration timeout = Duration.ofMillis(1);

    boolean great = false;
    try (AcquiredLock lock1 = persistentLocker.acquireLock(AcquiredLock.class, "cba", timeout)) {
      Thread.sleep(10);
      for (int i = 0; i < 10; ++i) {
        try (AcquiredLock lock2 = persistentLocker.tryToAcquireLock(AcquiredLock.class, "cba", timeout)) {
          if (lock2 != null) {
            great = true;
            break;
          }
        }
        sleep(Duration.ofMillis(100));
      }
    } catch (WingsException exception) {
    }

    assertTrue(great);
  }
}

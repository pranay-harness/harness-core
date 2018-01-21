package software.wings.lock;

import static io.harness.threading.Morpheus.sleep;
import static org.assertj.core.api.Assertions.assertThat;
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
  public void testAcquireLockAfterDestroy() {
    try (AcquiredLock lock = persistentLocker.acquireLock("foo", Duration.ofSeconds(1))) {
      persistentLocker.destroy(lock);
    } catch (WingsException exception) {
      // Do nothing. This is just to suppress the exception
    }

    try (AcquiredLock lock = persistentLocker.acquireLock("foo", Duration.ofSeconds(1))) {
    }
  }

  @Test
  public void testTryToAcquireLock() {
    try (AcquiredLock outer = persistentLocker.tryToAcquireLock(AcquiredLock.class, "foo", Duration.ofSeconds(1))) {
      assertThat(outer).isNotNull();
      try (AcquiredLock inner = persistentLocker.tryToAcquireLock(AcquiredLock.class, "foo", Duration.ofSeconds(1))) {
        assertThat(inner).isNull();
      }
    }
  }

  @Test
  @Ignore // The underlining code does not respect lock after timeout. Enable this test when this issue is fixed.
  public void testAcquireAfterTimeout() throws InterruptedException {
    class AnotherLock implements Runnable {
      public boolean locked;
      public boolean tested;

      @Override
      public void run() {
        try (AcquiredLock lock = persistentLocker.acquireLock(AcquiredLock.class, "cba", Duration.ofMillis(1))) {
          sleep(Duration.ofMillis(5));
          synchronized (this) {
            locked = true;
            this.notify();
          }

          synchronized (this) {
            while (!tested) {
              try {
                this.wait();
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }
        }
      }
    }

    AnotherLock run = new AnotherLock();
    Thread thread = new Thread(run);
    thread.start();

    synchronized (run) {
      while (!run.locked) {
        run.wait();
      }
    }

    boolean great = false;
    try (AcquiredLock lock = persistentLocker.acquireLock(AcquiredLock.class, "cba", Duration.ofMillis(100))) {
      great = true;
    }
    sleep(Duration.ofMillis(5));

    synchronized (run) {
      run.tested = true;
      run.notify();
    }

    thread.join();
    assertTrue(great);
  }
}

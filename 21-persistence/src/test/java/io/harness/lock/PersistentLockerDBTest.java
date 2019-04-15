package io.harness.lock;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.lock.PersistentLocker.LOCKS_STORE;
import static io.harness.threading.Morpheus.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import io.harness.rule.RealMongo;
import io.harness.threading.Concurrent;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;

/**
 * The Class PersistentLockerTest.
 */
@Slf4j
public class PersistentLockerDBTest extends PersistenceTest {
  @Inject private DistributedLockSvc distributedLockSvc;
  @Inject private HPersistence persistence;
  @Inject private PersistentLocker persistentLocker;

  private DBObject getDbLock(String uuid) {
    final DBCollection locks = persistence.getCollection(LOCKS_STORE, ReadPref.NORMAL, "locks");
    return locks.findOne(new BasicDBObject().append("_id", uuid));
  }

  @Test
  @Category(UnitTests.class)
  public void testAcquireLockDoLock() {
    String uuid = generateUuid();
    try (AcquiredLock lock = persistentLocker.acquireLock(uuid, Duration.ofSeconds(1))) {
    }

    DBObject dbLock = getDbLock(uuid);
    assertNotNull(dbLock);

    boolean damage = false;
    try (AcquiredLock lock = persistentLocker.acquireLock(uuid, Duration.ofSeconds(1))) {
      persistentLocker.destroy(lock);
      damage = true;
    } catch (WingsException exception) {
      // Do nothing. This is just to suppress the exception
    }

    assertFalse(damage);

    dbLock = getDbLock(uuid);
    assertNull(dbLock);
  }

  @Test
  @Category(UnitTests.class)
  public void testAcquireEphemeralLock() {
    String uuid = generateUuid();
    try (AcquiredLock lock = persistentLocker.acquireEphemeralLock(uuid, Duration.ofSeconds(1))) {
    } catch (WingsException exception) {
      // Do nothing. This is just to suppress the exception
    }

    DBObject dbLock = getDbLock(uuid);
    assertNull(dbLock);
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void testConcurrentAcquireEphemeralLock() {
    String uuid = generateUuid();

    Concurrent.test(10, i -> {
      try (AcquiredLock lock = persistentLocker.acquireEphemeralLock(uuid, Duration.ofSeconds(1))) {
      } catch (WingsException exception) {
        // Do nothing. This is just to suppress the exception
      }
    });
  }

  @Test
  @Category(UnitTests.class)
  public void testAcquireLockAfterDestroy() {
    String uuid = generateUuid();
    try (AcquiredLock lock = persistentLocker.acquireLock(uuid, Duration.ofSeconds(1))) {
      persistentLocker.destroy(lock);
    } catch (WingsException exception) {
      // Do nothing. This is just to suppress the exception
    }

    try (AcquiredLock lock = persistentLocker.acquireLock(uuid, Duration.ofSeconds(1))) {
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testTryToAcquireEphemeralLock() {
    String uuid = generateUuid();
    try (AcquiredLock outer =
             persistentLocker.tryToAcquireEphemeralLock(AcquiredLock.class, "foo", Duration.ofSeconds(1))) {
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testTryToAcquireLock() {
    String uuid = generateUuid();
    try (AcquiredLock outer = persistentLocker.tryToAcquireLock(AcquiredLock.class, uuid, Duration.ofSeconds(1))) {
      assertThat(outer).isNotNull();
      try (AcquiredLock inner = persistentLocker.tryToAcquireLock(AcquiredLock.class, uuid, Duration.ofSeconds(1))) {
        assertThat(inner).isNull();
      }
    }
  }

  @Test
  @Category(UnitTests.class)
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
                logger.error("", e);
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

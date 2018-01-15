package software.wings.lock;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.inject.Inject;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;

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

    try (AcquiredLock lock = persistentLocker.acquireLock("foo", Duration.ofSeconds(1))) {
      persistentLocker.destroy(lock);
    }

    dbLock = wingsPersistence.getCollection("locks").findOne(filter);
    assertNull(dbLock);
  }
}

package software.wings.scheduler;

import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;

import java.time.Duration;
import java.time.OffsetDateTime;

public class PersistentLockCleanupJobTest extends WingsBaseTest {
  public static final Logger logger = LoggerFactory.getLogger(PersistentLockCleanupJobTest.class);

  @Inject private PersistentLocker persistentLocker;

  @Inject @InjectMocks PersistentLockCleanupJob job;

  @Test
  public void selfPruneTheJobWhenSucceed() throws Exception {
    OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);
    try (AcquiredLock lock =
             persistentLocker.acquireLock(PersistentLockCleanupJob.class, "bar1", Duration.ofSeconds(1))) {
    }

    try (AcquiredLock lock =
             persistentLocker.acquireLock(PersistentLockCleanupJob.class, "bar2", Duration.ofSeconds(1))) {
    }

    OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);

    // Query with before date - we should not have the locks in the result
    DBCursor dbObjects = job.queryOldLocks(before);

    for (int i = 0; i < 2; ++i) {
      boolean fooBar1 = false;
      boolean fooBar2 = false;

      while (dbObjects.hasNext()) {
        final DBObject object = dbObjects.next();
        final Object id = object.get("_id");
        if (id.equals(PersistentLockCleanupJob.class.getName() + "-bar1")) {
          fooBar1 = true;
        }
        if (id.equals(PersistentLockCleanupJob.class.getName() + "-bar2")) {
          fooBar2 = true;
        }
      }

      assertEquals(i != 0, fooBar1);
      assertEquals(i != 0, fooBar2);

      // Query with after date - we should have the locks in the result
      dbObjects = job.queryOldLocks(after);
    }
  }
}

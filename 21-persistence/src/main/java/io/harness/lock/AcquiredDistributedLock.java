package io.harness.lock;

import static io.harness.govern.Switch.unhandled;
import static io.harness.lock.PersistentLocker.LOCKS_STORE;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.mongodb.BasicDBObject;
import io.harness.persistence.HPersistence;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.util.time.MonotonicSystemClock;
import org.eclipse.jgit.util.time.ProposedTimestamp;

@Builder
@Slf4j
public class AcquiredDistributedLock implements AcquiredLock {
  public static final MonotonicSystemClock monotonicSystemClock = new MonotonicSystemClock();

  @Getter private DistributedLock lock;
  private long startTimestamp;

  enum CloseAction { RELEASE, DESTROY }

  private DistributedLockSvc distributedLockSvc;
  private HPersistence persistence;
  private CloseAction closeAction;

  public static long monotonicTimestamp() {
    try (ProposedTimestamp timestamp = monotonicSystemClock.propose()) {
      return timestamp.millis();
    }
  }

  @Override
  public void release() {
    lock.unlock();
    lock = null;
  }

  @Override
  public void close() {
    if (lock == null) {
      return;
    }

    // Check if procedure took longer than its timeout. This is as bad as not having lock at first place.
    // Any lock that attempts to grab the lock after its timeout will be able to grab it. Resulting in
    // working in parallel with the current process.
    final long elapsed = monotonicTimestamp() - startTimestamp;
    final int timeout = lock.getOptions().getInactiveLockTimeout();
    if (elapsed > timeout) {
      logger.error("The distributed lock {} was not released on time. THIS IS VERY BAD!!!, elapsed: {}, timeout {}",
          lock.getName(), elapsed, timeout);

      // At this point the situation is already troublesome. After the timeout expired the current
      // process potentially overlapped with some other process working at the same time.
      // Lets not make the things even worse with releasing potentially someones else lock.
      // NOTE: letting the lock as is, is not a problem. It being timeout is as good as releasing it.

      // TODO: All this is very good only if the timeout functionality is working. The library we currently using
      //       does not respect the timing out. Return from here when it is fixed.
      // return;
    }

    if (!lock.isLocked()) {
      logger.error("attempt to release lock that is not currently locked", new Exception(""));
      return;
    }

    try {
      switch (closeAction) {
        case DESTROY:
          String name = lock.getName();
          final BasicDBObject filter = new BasicDBObject().append("_id", name);
          persistence.getCollection(LOCKS_STORE, "locks").remove(filter);
          break;
        case RELEASE:
          lock.unlock();
          break;
        default:
          unhandled(closeAction);
      }
    } catch (RuntimeException ex) {
      logger.warn("releaseLock failed for key: " + lock.getName(), ex);
    }
  }
}

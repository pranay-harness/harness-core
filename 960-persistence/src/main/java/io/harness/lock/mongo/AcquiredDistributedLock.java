/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.lock.mongo;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.govern.Switch.unhandled;
import static io.harness.lock.mongo.MongoPersistentLocker.LOCKS_STORE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.lock.AcquiredLock;
import io.harness.persistence.HPersistence;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.mongodb.BasicDBObject;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.util.time.MonotonicSystemClock;
import org.eclipse.jgit.util.time.ProposedTimestamp;

@OwnedBy(PL)
@Builder
@Slf4j
public class AcquiredDistributedLock implements AcquiredLock<DistributedLock> {
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
      log.error("The distributed lock {} was not released on time. THIS IS VERY BAD!!!, elapsed: {}, timeout {}",
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
      log.error("attempt to release lock that is not currently locked", new Exception(""));
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
      log.warn("releaseLock failed for key: " + lock.getName(), ex);
    }
  }
}

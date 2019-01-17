package io.harness.lock;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.NOBODY;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockOptions;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.mongodb.BasicDBObject;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredDistributedLock.AcquiredDistributedLockBuilder;
import io.harness.lock.AcquiredDistributedLock.CloseAction;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import io.harness.persistence.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Singleton
public class PersistentLocker implements Locker {
  private static final Logger logger = LoggerFactory.getLogger(PersistentLocker.class);

  public static final Store LOCKS_STORE = Store.builder().name("locks").build();

  @Inject private DistributedLockSvc distributedLockSvc;
  @Inject private HPersistence persistence;
  @Inject private TimeLimiter timeLimiter;

  @Override
  public AcquiredLock acquireLock(String name, Duration timeout) {
    return acquireLock(name, timeout, AcquiredDistributedLock.builder().closeAction(CloseAction.RELEASE));
  }

  @Override
  public AcquiredLock acquireEphemeralLock(String name, Duration timeout) {
    return acquireLock(
        name, timeout, AcquiredDistributedLock.builder().closeAction(CloseAction.DESTROY).persistence(persistence));
  }

  @SuppressWarnings("PMD")
  public AcquiredLock acquireLock(String name, Duration timeout, AcquiredDistributedLockBuilder builder) {
    DistributedLockOptions options = new DistributedLockOptions();
    options.setInactiveLockTimeout((int) timeout.toMillis());

    DistributedLock lock = distributedLockSvc.create(name, options);

    // measure the time before obtaining the lock
    long start = AcquiredDistributedLock.monotonicTimestamp();

    try {
      if (lock.tryLock()) {
        return builder.lock(lock).startTimestamp(start).build();
      }
    } catch (NullPointerException ignore) {
      // There is a race inside DistributedLock that can result in a NullPointerException when the persistent db lock
      // object is deleted in the middle of tryLock. Ignore the exception and assume that we failed to obtain the lock.
    }

    throw new WingsException(GENERAL_ERROR, NOBODY)
        .addParam("message", format("Failed to acquire distributed lock for %s", name));
  }

  @Override
  public AcquiredLock acquireLock(Class entityClass, String entityId, Duration timeout) {
    return acquireLock(entityClass.getName() + "-" + entityId, timeout);
  }

  @Override
  public AcquiredLock tryToAcquireLock(Class entityClass, String entityId, Duration timeout) {
    return tryToAcquireLock(entityClass.getName() + "-" + entityId, timeout);
  }

  @Override
  public AcquiredLock tryToAcquireEphemeralLock(Class entityClass, String entityId, Duration timeout) {
    return tryToAcquireEphemeralLock(entityClass.getName() + "-" + entityId, timeout);
  }

  @Override
  public AcquiredLock tryToAcquireLock(String name, Duration timeout) {
    try {
      return acquireLock(name, timeout);
    } catch (WingsException exception) {
      return null;
    }
  }

  @Override
  public AcquiredLock tryToAcquireEphemeralLock(String name, Duration timeout) {
    try {
      return acquireEphemeralLock(name, timeout);
    } catch (WingsException exception) {
      return null;
    }
  }

  @Override
  public AcquiredLock waitToAcquireLock(
      Class entityClass, String entityId, Duration lockTimeout, Duration waitTimeout) {
    String name = entityClass.getName() + "-" + entityId;
    return waitToAcquireLock(name, lockTimeout, waitTimeout);
  }

  @Override
  public AcquiredLock waitToAcquireLock(String name, Duration lockTimeout, Duration waitTimeout) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        while (true) {
          try {
            return acquireLock(name, lockTimeout);
          } catch (WingsException exception) {
            sleep(ofMillis(100));
          }
        }
      }, waitTimeout.toMillis(), TimeUnit.MILLISECONDS, true);
    } catch (Exception e) {
      throw new WingsException(GENERAL_ERROR, NOBODY, e)
          .addParam("message", format("Failed to acquire distributed lock for %s within %s", name, waitTimeout));
    }
  }

  @Override
  public void destroy(AcquiredLock acquiredLock) {
    String name = acquiredLock.getLock().getName();
    // NOTE: DistributedLockSvc destroy does not work. Also it expects the lock to not be acquired which
    //       is design flow. The only safe moment to destroy lock is, when you currently have it acquired.
    final BasicDBObject filter = new BasicDBObject().append("_id", name);
    persistence.getCollection(LOCKS_STORE, ReadPref.NORMAL, "locks").remove(filter);
    acquiredLock.release();
    throw new WingsException(GENERAL_ERROR, NOBODY)
        .addParam("message", format("Acquired distributed lock %s was destroyed and the lock was broken.", name));
  }
}

package io.harness.lock.mongo;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.NOBODY;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockOptions;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import io.dropwizard.lifecycle.Managed;
import io.harness.exception.GeneralException;
import io.harness.exception.WingsException;
import io.harness.health.HealthMonitor;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.lock.mongo.AcquiredDistributedLock.AcquiredDistributedLockBuilder;
import io.harness.lock.mongo.AcquiredDistributedLock.CloseAction;
import io.harness.persistence.HPersistence;
import io.harness.persistence.Store;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Singleton
@Slf4j
public class MongoPersistentLocker implements PersistentLocker, HealthMonitor, Managed {
  private static final String LOCKS_COLLECTION = "locks";
  public static final Store LOCKS_STORE = Store.builder().name(LOCKS_COLLECTION).build();
  private DistributedLockSvc distributedLockSvc;
  private HPersistence persistence;
  private TimeLimiter timeLimiter;

  @Inject
  public MongoPersistentLocker(@Named("locksMongoClient") MongoClient mongoClient,
      @Named("locksDatabase") String locksDB, HPersistence persistence, TimeLimiter timeLimiter) {
    this.persistence = persistence;
    this.timeLimiter = timeLimiter;
    DistributedLockSvcOptions distributedLockSvcOptions =
        new DistributedLockSvcOptions(mongoClient, locksDB, LOCKS_COLLECTION);
    distributedLockSvcOptions.setEnableHistory(false);
    this.distributedLockSvc = new DistributedLockSvcFactory(distributedLockSvcOptions).getLockSvc();
    if (distributedLockSvc != null && !distributedLockSvc.isRunning()) {
      distributedLockSvc.startup();
    }
  }

  @Override
  public AcquiredLock acquireLock(String name, Duration timeout) {
    return acquireLock(name, timeout, AcquiredDistributedLock.builder().closeAction(CloseAction.RELEASE));
  }

  @Override
  public AcquiredLock acquireEphemeralLock(String name, Duration timeout) {
    return acquireLock(name, timeout,
        AcquiredDistributedLock.builder()
            .closeAction(CloseAction.DESTROY)
            .persistence(persistence)
            .distributedLockSvc(distributedLockSvc));
  }

  @SuppressWarnings({"PMD", "squid:S2222"})
  public AcquiredLock acquireLock(String name, Duration timeout, AcquiredDistributedLockBuilder builder) {
    DistributedLockOptions options = new DistributedLockOptions();
    options.setInactiveLockTimeout((int) timeout.toMillis());

    DistributedLock lock = distributedLockSvc.create(name, options);

    // measure the time before obtaining the lock

    try {
      if (lock.tryLock()) {
        logger.info("Lock acquired on {} for timeout {}", name, timeout);
        long start = AcquiredDistributedLock.monotonicTimestamp();
        return builder.lock(lock).startTimestamp(start).build();
      }
    } catch (NullPointerException ignore) {
      // There is a race inside DistributedLock that can result in a NullPointerException when the persistent db lock
      // object is deleted in the middle of tryLock. Ignore the exception and assume that we failed to obtain the lock.
    }

    throw new GeneralException(format("Failed to acquire distributed lock for %s", name), NOBODY);
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
      throw new GeneralException(
          format("Failed to acquire distributed lock for %s within %s", name, waitTimeout), e, NOBODY);
    }
  }

  @Override
  public void destroy(AcquiredLock acquiredLock) {
    Lock lock = acquiredLock.getLock();
    DistributedLock distributedLock = (DistributedLock) lock;
    String name = distributedLock.getName();
    // NOTE: DistributedLockSvc destroy does not work. Also it expects the lock to not be acquired which
    //       is design flow. The only safe moment to destroy lock is, when you currently have it acquired.
    final BasicDBObject filter = new BasicDBObject().append("_id", name);
    persistence.getCollection(LOCKS_STORE, LOCKS_COLLECTION).remove(filter);
    acquiredLock.release();
    throw new GeneralException(
        format("Acquired distributed lock %s was destroyed and the lock was broken.", name), NOBODY);
  }

  @Override
  public Duration healthExpectedResponseTimeout() {
    return ofSeconds(10);
  }

  @Override
  public Duration healthValidFor() {
    return ofSeconds(15);
  }

  @Override
  public void isHealthy() {
    try (AcquiredLock dummy = acquireEphemeralLock("HEALTH_CHECK - " + generateUuid(), ofSeconds(1))) {
      // nothing to do
    }
  }

  @Override
  public void start() throws Exception {
    if (distributedLockSvc != null && !distributedLockSvc.isRunning()) {
      distributedLockSvc.startup();
    }
  }

  @Override
  public void stop() throws Exception {
    if (distributedLockSvc != null) {
      distributedLockSvc.shutdown();
    }
  }
}

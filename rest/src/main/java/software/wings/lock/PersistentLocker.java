package software.wings.lock;

import static java.lang.String.format;
import static software.wings.beans.ErrorCode.GENERAL_ERROR;
import static software.wings.beans.ResponseMessage.Acuteness.IGNORABLE;
import static software.wings.beans.ResponseMessage.aResponseMessage;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockOptions;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsException;

import java.time.Duration;

@Singleton
public class PersistentLocker implements Locker {
  private static final Logger logger = LoggerFactory.getLogger(PersistentLocker.class);
  @Inject private DistributedLockSvc distributedLockSvc;

  @Override
  public AcquiredLock acquireLock(Class entityClass, String entityId, Duration timeout) {
    return acquireLock(entityClass.getName(), entityId, timeout);
  }

  @Override
  public AcquiredLock acquireLock(String entityType, String entityId, Duration timeout) {
    DistributedLockOptions options = new DistributedLockOptions();
    options.setInactiveLockTimeout((int) timeout.toMillis());

    String key = entityType + "-" + entityId;
    DistributedLock lock = distributedLockSvc.create(key, options);

    // measure the time before obtaining the lock
    long start = AcquiredLock.monotonicTimestamp();
    if (lock.tryLock()) {
      return AcquiredLock.builder().lock(lock).startTimestamp(start).build();
    }
    throw new WingsException(aResponseMessage().code(GENERAL_ERROR).acuteness(IGNORABLE).build())
        .addParam("args", format("Failed to acquire distributed lock for %s", key));
  }
}

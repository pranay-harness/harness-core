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
import com.mongodb.BasicDBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;

import java.time.Duration;

@Singleton
public class PersistentLocker implements Locker {
  private static final Logger logger = LoggerFactory.getLogger(PersistentLocker.class);
  @Inject private DistributedLockSvc distributedLockSvc;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public AcquiredLock acquireLock(String name, Duration timeout) {
    DistributedLockOptions options = new DistributedLockOptions();
    options.setInactiveLockTimeout((int) timeout.toMillis());

    DistributedLock lock = distributedLockSvc.create(name, options);

    // measure the time before obtaining the lock
    long start = AcquiredLock.monotonicTimestamp();
    if (lock.tryLock()) {
      return AcquiredLock.builder().lock(lock).startTimestamp(start).build();
    }

    throw new WingsException(aResponseMessage().code(GENERAL_ERROR).acuteness(IGNORABLE).build())
        .addParam("args", format("Failed to acquire distributed lock for %s", name));
  }

  @Override
  public AcquiredLock acquireLock(Class entityClass, String entityId, Duration timeout) {
    return acquireLock(entityClass.getName() + "-" + entityId, timeout);
  }

  @Override
  public AcquiredLock tryToAcquireLock(Class entityClass, String entityId, Duration timeout) {
    try {
      return acquireLock(entityClass.getName() + "-" + entityId, timeout);
    } catch (WingsException exception) {
      return null;
    }
  }

  @Override
  public void destroy(AcquiredLock acquiredLock) {
    String name = acquiredLock.getLock().getName();
    // NOTE: DistributedLockSvc destroy does not work. Also it expects the lock to not be acquired which
    //       is design flow. The only safe moment to destroy lock is, when you currently have it acquired.
    final BasicDBObject filter = new BasicDBObject().append("_id", name);
    wingsPersistence.getCollection("locks").remove(filter);
    acquiredLock.release();
    throw new WingsException(aResponseMessage().code(GENERAL_ERROR).acuteness(IGNORABLE).build())
        .addParam("args", format("Acquired distributed lock %s was destroyed and the lock was broken.", name));
  }
}

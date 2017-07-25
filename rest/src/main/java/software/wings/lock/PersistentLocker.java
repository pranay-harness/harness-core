package software.wings.lock;

import com.google.inject.Singleton;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.utils.Misc;

import javax.inject.Inject;

/**
 * Persistent Locker implementation using Mongo DB.
 *
 * @author Rishi
 */
@Singleton
public class PersistentLocker implements Locker {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private DistributedLockSvc distributedLockSvc;

  /* (non-Javadoc)
   * @see software.wings.lock.Locker#acquireLock(java.lang.Class, java.lang.String)
   */
  @Override
  public boolean acquireLock(Class entityClass, String entityId) {
    return acquireLock(entityClass.getName(), entityId);
  }

  /* (non-Javadoc)
   * @see software.wings.lock.Locker#acquireLock(java.lang.String, java.lang.String)
   */
  @Override
  public boolean acquireLock(String entityType, String entityId) {
    DistributedLock lock = distributedLockSvc.create(entityType + "-" + entityId);
    try {
      return lock.tryLock();
    } catch (Exception ex) {
      Misc.debug(logger, "acquireLock failed - entityType: " + entityType + ", entityId: " + entityId, ex);
      return false;
    }
  }

  /* (non-Javadoc)
   * @see software.wings.lock.Locker#releaseLock(java.lang.Class, java.lang.String)
   */
  @Override
  public boolean releaseLock(Class entityClass, String entityId) {
    return releaseLock(entityClass.getName(), entityId);
  }

  /* (non-Javadoc)
   * @see software.wings.lock.Locker#releaseLock(java.lang.String, java.lang.String)
   */
  @Override
  public boolean releaseLock(String entityType, String entityId) {
    DistributedLock lock = distributedLockSvc.create(entityType + "-" + entityId);

    if (lock.isLocked()) {
      try {
        lock.unlock();
      } catch (Exception ex) {
        Misc.debug(logger, "releaseLock failed - entityType: " + entityType + ", entityId: " + entityId, ex);
        return false;
      }
      return true;
    } else {
      return false;
    }
  }
}

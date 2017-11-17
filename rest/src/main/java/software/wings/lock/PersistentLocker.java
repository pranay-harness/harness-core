package software.wings.lock;

import com.google.inject.Singleton;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    return acquireLock(entityClass.getName(), entityId, 0);
  }

  /* (non-Javadoc)
   * @see software.wings.lock.Locker#acquireLock(java.lang.Class, java.lang.String, long)
   */
  @Override
  public boolean acquireLock(Class entityClass, String entityId, long timeout) {
    return acquireLock(entityClass.getName(), entityId, timeout);
  }

  /* (non-Javadoc)
   * @see software.wings.lock.Locker#acquireLock(java.lang.String, java.lang.String)
   */
  @Override
  public boolean acquireLock(String entityType, String entityId) {
    return acquireLock(entityType, entityId, 0);
  }

  /* (non-Javadoc)
   * @see software.wings.lock.Locker#acquireLock(java.lang.String, java.lang.String, long)
   */
  @Override
  public boolean acquireLock(String entityType, String entityId, long timeout) {
    DistributedLock lock = distributedLockSvc.create(entityType + "-" + entityId);
    try {
      boolean acquired = lock.tryLock();
      long start = System.currentTimeMillis();
      while (!acquired && System.currentTimeMillis() - start < timeout) {
        try {
          Thread.sleep(200L);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        acquired = lock.tryLock();
      }
      return acquired;
    } catch (Exception ex) {
      logger.debug("acquireLock failed - entityType: " + entityType + ", entityId: " + entityId, ex);
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
        logger.debug("releaseLock failed - entityType: " + entityType + ", entityId: " + entityId, ex);
        return false;
      }
      return true;
    } else {
      return false;
    }
  }
}

package software.wings.lock;

import java.net.InetAddress;
import java.util.Date;

import javax.inject.Inject;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import software.wings.beans.PageRequest;
import software.wings.beans.SearchFilter;
import software.wings.dl.WingsPersistence;

/**
 *  Persistent Locker implementation using Mongo DB.
 *
 *
 * @author Rishi
 *
 */
@Singleton
public class PersistentLocker implements Locker {
  @Inject private DistributedLockSvc distributedLockSvc;

  @Override
  public boolean acquireLock(Class entityClass, String entityId) {
    return acquireLock(entityClass.getName(), entityId);
  }

  @Override
  public boolean acquireLock(String entityType, String entityId) {
    DistributedLock lock = distributedLockSvc.create(entityType + "-" + entityId);
    try {
      return lock.tryLock();
    } catch (Exception e) {
      logger.debug("acquireLock failed - entityType: " + entityType + ", entityId: " + entityId, e);
      return false;
    }
  }

  @Override
  public boolean releaseLock(Class entityClass, String entityId) {
    return releaseLock(entityClass.getName(), entityId);
  }

  @Override
  public boolean releaseLock(String entityType, String entityId) {
    DistributedLock lock = distributedLockSvc.create(entityType + "-" + entityId);

    if (lock.isLocked()) {
      try {
        lock.unlock();
      } catch (Exception e) {
        logger.debug("releaseLock failed - entityType: " + entityType + ", entityId: " + entityId, e);
        return false;
      }
      return true;
    } else {
      return false;
    }
  }

  private static Logger logger = LoggerFactory.getLogger(PersistentLocker.class);
}

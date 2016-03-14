package software.wings.lock;

import java.net.InetAddress;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.beans.ErrorConstants;
import software.wings.beans.PageRequest;
import software.wings.beans.SearchFilter;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;

/**
 *  Persistent Locker implementation using Mongo DB.
 *
 *
 * @author Rishi
 *
 */
public class PersistentLocker implements Locker {
  private static PersistentLocker instance;
  private WingsPersistence wingsPersistence;

  public PersistentLocker(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  public synchronized static void init(WingsPersistence wingsPersistence) {
    if (instance == null) {
      instance = new PersistentLocker(wingsPersistence);
    }
  }

  public static PersistentLocker getInstance() {
    if (instance == null) {
      throw new WingsException(ErrorConstants.NOT_INITIALIZED);
    }
    return instance;
  }

  @Override
  public boolean acquireLock(Class entityClass, String entityId) {
    return acquireLock(entityClass.getName(), entityId, null);
  }

  @Override
  public boolean acquireLock(Class entityClass, String entityId, Date expiryDate) {
    return acquireLock(entityClass.getName(), entityId, null);
  }

  @Override
  public boolean acquireLock(String entityType, String entityId) {
    return acquireLock(entityType, entityId, null);
  }

  @Override
  public boolean acquireLock(String entityType, String entityId, Date expiryDate) {
    Lock lock = new Lock();
    lock.setEntityType(entityType);
    lock.setEntityId(entityId);
    lock.setExpiryDate(expiryDate);
    populateHostIp(lock);
    try {
      String uuid = wingsPersistence.save(lock);
      logger.debug(
          "Lock acquired - entityType: " + entityType + ", entityId: " + entityId + ", expiryDate: " + expiryDate);
      return true;
    } catch (Exception e) {
      logger.debug(
          "acquireLock failed - entityType: " + entityType + ", entityId: " + entityId + ", expiryDate: " + expiryDate,
          e);
      return false;
    }
  }

  @Override
  public boolean releaseLock(Class entityClass, String entityId) {
    return releaseLock(entityClass.getName(), entityId);
  }
  @Override
  public boolean releaseLock(String entityType, String entityId) {
    try {
      PageRequest<Lock> req = new PageRequest<>();
      SearchFilter filter = new SearchFilter();
      filter.setFieldName("entityType");
      filter.setFieldValue(entityType);
      req.getFilters().add(filter);

      filter = new SearchFilter();
      filter.setFieldName("entityId");
      filter.setFieldValue(entityId);
      req.getFilters().add(filter);

      Lock lock = wingsPersistence.get(Lock.class, req);
      if (lock == null) {
        logger.warn("releaseLock failed - No lock found for the entityType: " + entityType + ", entityId: " + entityId);
        return false;
      }
      boolean deleted = wingsPersistence.delete(lock);
      if (!deleted) {
        logger.warn(
            "releaseLock failed - No lock deleted for the entityType: " + entityType + ", entityId: " + entityId);
      }
      return deleted;
    } catch (Exception e) {
      logger.debug("releaseLock failed - entityType: " + entityType + ", entityId: " + entityId, e);
      return false;
    }
  }

  private void populateHostIp(Lock lock) {
    try {
      lock.setThreadId(Thread.currentThread().getId());
      lock.setThreadName(Thread.currentThread().getName());
      InetAddress ip = InetAddress.getLocalHost();
      lock.setHostName(ip.getHostName());
      lock.setIpAddress(ip.getHostAddress());

    } catch (Exception e) {
      // ignore
    }
  }
  private static Logger logger = LoggerFactory.getLogger(PersistentLocker.class);
}

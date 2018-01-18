package software.wings.lock;

import java.time.Duration;

/**
 * Locker interface to acquire and release locks.
 *
 * @author Rishi
 */
public interface Locker {
  /**
   * Acquire lock.
   *
   * @param name    the lock name
   * @param timeout for how long to keep the lock if the app crashes
   * @return AcquiredLock object
   * @throws WingsException
   */
  AcquiredLock acquireLock(String name, Duration timeout);

  /**
   * Acquire lock.
   *
   * @param entityClass the entity class
   * @param entityId    the entity id
   * @param timeout     for how long to keep the lock if the app crashes
   * @return AcquiredLock object
   * @throws WingsException
   */
  AcquiredLock acquireLock(Class entityClass, String entityId, Duration timeout);

  /**
   * Acquire lock.
   *
   * @param entityClass the entity class
   * @param entityId    the entity id
   * @param timeout     for how long to keep the lock if the app crashes
   * @return AcquiredLock object
   * @throws WingsException
   */
  AcquiredLock tryToAcquireLock(Class entityClass, String entityId, Duration timeout);

  /**
   * Destroy lock.
   *
   * @param acquiredLock  already acquired lock
   */
  void destroy(AcquiredLock acquiredLock);
}

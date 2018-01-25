package software.wings.lock;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
   */
  AcquiredLock acquireLock(String name, Duration timeout);

  /**
   * Acquire lock.
   *
   * @param entityClass the entity class
   * @param entityId    the entity id
   * @param timeout     for how long to keep the lock if the app crashes
   * @return AcquiredLock object
   */
  AcquiredLock acquireLock(Class entityClass, String entityId, Duration timeout);

  /**
   * Acquire lock.
   *
   * @param entityClass the entity class
   * @param entityId    the entity id
   * @param timeout     for how long to keep the lock if the app crashes
   * @return AcquiredLock object
   */
  AcquiredLock tryToAcquireLock(Class entityClass, String entityId, Duration timeout);

  /**
   * Acquire lock.
   *
   * @param entityClass the entity class
   * @param entityId    the entity id
   * @param lockTimeout     for how long to keep the lock if the app crashes
   * @param timeoutDuration  how long to wait to acquire the lock
   * @param timeoutUnit  time unit for how long to wait to acquire the lock
   * @return AcquiredLock object
   */
  AcquiredLock waitToAcquireLock(
      Class entityClass, String entityId, Duration lockTimeout, long timeoutDuration, TimeUnit timeoutUnit);

  /**
   * Destroy lock.
   *
   * @param acquiredLock  already acquired lock
   */
  void destroy(AcquiredLock acquiredLock);
}

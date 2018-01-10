package software.wings.lock;

/**
 * Locker interface to acquire and release locks.
 *
 * @author Rishi
 */
public interface Locker {
  /**
   * Acquire lock.
   *
   * @param entityClass the entity class
   * @param entityId    the entity id
   * @return AcquiredLock object
   */
  AcquiredLock acquireLock(Class entityClass, String entityId);

  /**
   * Acquire lock.
   *
   * @param entityType the entity type
   * @param entityId   the entity id
   * @return true, if successful
   */
  AcquiredLock acquireLock(String entityType, String entityId);
}

package software.wings.lock;

/**
 * Locker interface to acquire and release locks
 *
 * @author Rishi
 */
public interface Locker {
  public boolean acquireLock(Class entityClass, String entityId);

  public boolean acquireLock(String entityType, String entityId);

  public boolean releaseLock(Class entityClass, String entityId);

  public boolean releaseLock(String entityType, String entityId);
}

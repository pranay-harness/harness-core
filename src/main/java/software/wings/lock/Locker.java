package software.wings.lock;

import java.util.Date;

public interface Locker {
  public boolean acquireLock(String entityType, String entityId);
  public boolean acquireLock(String entityType, String entityId, Date expiryDate);

  public boolean releaseLock(String entityType, String entityId);
}

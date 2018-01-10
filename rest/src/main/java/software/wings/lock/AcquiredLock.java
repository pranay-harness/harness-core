package software.wings.lock;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

@Builder
public class AcquiredLock implements Closeable {
  private static Logger logger = LoggerFactory.getLogger(AcquiredLock.class);

  private String key;
  private DistributedLockSvc distributedLockSvc;

  @Override
  public void close() {
    DistributedLock lock = distributedLockSvc.create(key);

    if (!lock.isLocked()) {
      logger.error("attempt to release lock that is not currently locked", new Exception(""));
      return;
    }

    try {
      lock.unlock();
    } catch (RuntimeException ex) {
      logger.warn("releaseLock failed for key: " + key, ex);
    }
  }
}

package software.wings.lock;

import static java.lang.String.format;
import static software.wings.beans.ErrorCode.GENERAL_ERROR;
import static software.wings.beans.ResponseMessage.Acuteness.IGNORABLE;
import static software.wings.beans.ResponseMessage.aResponseMessage;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsException;

@Singleton
public class PersistentLocker implements Locker {
  private static final Logger logger = LoggerFactory.getLogger(PersistentLocker.class);
  @Inject private DistributedLockSvc distributedLockSvc;

  @Override
  public AcquiredLock acquireLock(Class entityClass, String entityId) {
    return acquireLock(entityClass.getName(), entityId);
  }

  @Override
  public AcquiredLock acquireLock(String entityType, String entityId) {
    String key = entityType + "-" + entityId;
    DistributedLock lock = distributedLockSvc.create(key);
    if (lock.tryLock()) {
      return AcquiredLock.builder().key(key).distributedLockSvc(distributedLockSvc).build();
    }
    throw new WingsException(aResponseMessage().code(GENERAL_ERROR).acuteness(IGNORABLE).build())
        .addParam("args", format("Failed to acquire distributed lock for %s", key));
  }
}

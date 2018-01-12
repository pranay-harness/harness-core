package software.wings.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.exception.WingsException.Scenario.MAINTENANCE_JOB;

import com.google.inject.Inject;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import software.wings.MockTest;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;

/**
 * The Class PersistentLockerTest.
 */
public class PersistentLockerTest extends MockTest {
  @Mock private DistributedLockSvc distributedLockSvc;

  @Inject @InjectMocks private PersistentLocker persistentLocker;

  @Test
  public void testAcquireLockDoLock() {
    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.tryLock()).thenReturn(true);
    when(distributedLock.isLocked()).thenReturn(true);
    when(distributedLockSvc.create("abc-cba")).thenReturn(distributedLock);

    try (AcquiredLock lock = persistentLocker.acquireLock("abc", "cba")) {
    }

    InOrder inOrder = inOrder(distributedLock);
    inOrder.verify(distributedLock, times(1)).tryLock();
    inOrder.verify(distributedLock, times(1)).unlock();
  }

  @Test
  public void testAcquireLockDoNotRunTheBody() {
    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.tryLock()).thenReturn(false);
    when(distributedLockSvc.create("abc-cba")).thenReturn(distributedLock);

    boolean body = false;
    try (AcquiredLock lock = persistentLocker.acquireLock("abc", "cba")) {
      body = true;
    } catch (RuntimeException ex) {
      assertThat(ex.getMessage()).isEqualTo("GENERAL_ERROR");
    }

    assertThat(body).isFalse();

    InOrder inOrder = inOrder(distributedLock);
    inOrder.verify(distributedLock, times(1)).tryLock();
    inOrder.verify(distributedLock, times(0)).unlock();
  }

  @Test
  public void testAcquireLockNonLockedAtRelease() {
    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.tryLock()).thenReturn(true);
    when(distributedLock.isLocked()).thenReturn(false);
    when(distributedLockSvc.create("abc-cba")).thenReturn(distributedLock);

    Logger logger = mock(Logger.class);
    Whitebox.setInternalState(new AcquiredLock(null, null), "logger", logger);

    try (AcquiredLock lock = persistentLocker.acquireLock("abc", "cba")) {
    }

    verify(logger).error(matches("attempt to release lock that is not currently locked"), any(Throwable.class));
  }

  @Test
  public void testAcquireLockLogging() {
    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.tryLock()).thenReturn(false);
    when(distributedLockSvc.create("abc-cba")).thenReturn(distributedLock);

    Logger logger = mock(Logger.class);

    Whitebox.setInternalState(ResponseCodeCache.getInstance(), "logger", logger);
    Whitebox.setInternalState(new WingsException(""), "logger", logger);

    try (AcquiredLock lock = persistentLocker.acquireLock("abc", "cba")) {
    } catch (WingsException exception) {
      exception.logProcessedMessages(MAINTENANCE_JOB);
    }

    verify(logger, times(0)).error(any());
  }
}

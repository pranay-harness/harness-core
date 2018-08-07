package software.wings.lock;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;

import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockOptions;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.harness.MockableTest;
import io.harness.eraro.MessageManager;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import software.wings.exception.WingsException;

import java.time.Duration;

/**
 * The Class PersistentLockerTest.
 */
public class PersistentLockerTest extends MockableTest {
  @Mock private DistributedLockSvc distributedLockSvc;

  @Inject @InjectMocks private PersistentLocker persistentLocker;

  @Test
  public void testAcquireLockDoLock() {
    Duration timeout = ofMillis(1000);

    DistributedLockOptions options = new DistributedLockOptions();
    options.setInactiveLockTimeout((int) timeout.toMillis());

    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.getOptions()).thenReturn(options);

    when(distributedLock.tryLock()).thenReturn(true);
    when(distributedLock.isLocked()).thenReturn(true);
    when(distributedLockSvc.create(matches(AcquiredLock.class.getName() + "-cba"), any())).thenReturn(distributedLock);

    try (AcquiredLock lock = persistentLocker.acquireLock(AcquiredLock.class, "cba", Duration.ofMinutes(1))) {
    }

    InOrder inOrder = inOrder(distributedLock);
    inOrder.verify(distributedLock, times(1)).tryLock();
    inOrder.verify(distributedLock, times(1)).unlock();
  }

  @Test
  public void testAcquireLockDoNotRunTheBody() {
    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.tryLock()).thenReturn(false);
    when(distributedLockSvc.create(matches(AcquiredLock.class.getName() + "-cba"), any())).thenReturn(distributedLock);

    boolean body = false;
    try (AcquiredLock lock = persistentLocker.acquireLock(AcquiredLock.class, "cba", Duration.ofMinutes(1))) {
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
  public void testTryAcquireLockDoNotThrowException() {
    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.tryLock()).thenReturn(false);
    when(distributedLockSvc.create(matches(AcquiredLock.class.getName() + "-cba"), any())).thenReturn(distributedLock);

    boolean body = false;
    try (AcquiredLock lock = persistentLocker.tryToAcquireLock(AcquiredLock.class, "cba", Duration.ofMinutes(1))) {
      assertThat(lock).isNull();
      body = true;
    }

    assertThat(body).isTrue();

    InOrder inOrder = inOrder(distributedLock);
    inOrder.verify(distributedLock, times(1)).tryLock();
    inOrder.verify(distributedLock, times(0)).unlock();
  }

  @Test
  public void testAcquireLockNonLockedAtRelease() {
    Duration timeout = ofMillis(1000);

    DistributedLockOptions options = new DistributedLockOptions();
    options.setInactiveLockTimeout((int) timeout.toMillis());

    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.getOptions()).thenReturn(options);

    when(distributedLock.tryLock()).thenReturn(true);
    when(distributedLock.isLocked()).thenReturn(false);
    when(distributedLockSvc.create(matches(AcquiredLock.class.getName() + "-cba"), any())).thenReturn(distributedLock);

    Logger logger = mock(Logger.class);
    Whitebox.setInternalState(new AcquiredLock(null, 0L), "logger", logger);

    try (AcquiredLock lock = persistentLocker.acquireLock(AcquiredLock.class, "cba", timeout)) {
    }

    verify(logger).error(matches("attempt to release lock that is not currently locked"), any(Throwable.class));
  }

  @Test
  public void testAcquireLockLogging() throws IllegalAccessException {
    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.tryLock()).thenReturn(false);
    when(distributedLockSvc.create(matches(AcquiredLock.class.getName() + "-cba"), any())).thenReturn(distributedLock);

    Logger mockLogger = mock(Logger.class);
    MockableTest.setStaticFieldValue(MessageManager.class, "logger", mockLogger);

    try (AcquiredLock lock = persistentLocker.acquireLock(AcquiredLock.class, "cba", Duration.ofMinutes(1))) {
    } catch (WingsException exception) {
      exception.logProcessedMessages(MANAGER, mockLogger);
    }

    verify(mockLogger, times(0)).error(any());
  }

  @Test
  @Ignore // TODO: enable when we can have timeout logic
  public void testAcquireTimeout() throws InterruptedException {
    Duration timeout = ofMillis(1);

    DistributedLockOptions options = new DistributedLockOptions();
    options.setInactiveLockTimeout((int) timeout.toMillis());

    DistributedLock distributedLock = mock(DistributedLock.class);
    when(distributedLock.getOptions()).thenReturn(options);
    when(distributedLock.getName()).thenReturn(AcquiredLock.class.getName() + "-cba");
    when(distributedLock.tryLock()).thenReturn(true);
    when(distributedLockSvc.create(matches(AcquiredLock.class.getName() + "-cba"), any())).thenReturn(distributedLock);

    Logger logger = mock(Logger.class);

    try (AcquiredLock lock = persistentLocker.acquireLock(AcquiredLock.class, "cba", timeout)) {
      Thread.sleep(10);
    } catch (WingsException exception) {
      exception.logProcessedMessages(MANAGER, logger);
    }

    verify(logger).error(matches(
        "The distributed lock abc-cba was not released on time. THIS IS VERY BAD!!!, elapsed: \\d+, timeout 1"));
  }
}

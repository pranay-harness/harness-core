package io.harness.lock.redis;

import static io.harness.rule.OwnerRule.RAMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.lock.AcquiredLock;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Spy;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author rktummala on 01/07/2020
 */
public class RedisPersistentLockerTest extends PersistenceTest {
  private RedissonClient client = mock(RedissonClient.class);
  @Spy private RedisPersistentLocker redisPersistentLocker = new RedisPersistentLocker(client);

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testAcquireLockDoLock() throws InterruptedException {
    RLock rLock = mock(RLock.class);
    when(client.getLock(anyString())).thenReturn(rLock);
    when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

    try (AcquiredLock lock = redisPersistentLocker.acquireLock(AcquiredLock.class, "cba", Duration.ofMinutes(1))) {
    }

    verify(rLock, times(1)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testAcquireLockDoNotRunTheBody() throws InterruptedException {
    RLock rLock = mock(RLock.class);
    when(client.getLock(anyString())).thenReturn(rLock);
    when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

    boolean body = false;
    try (AcquiredLock lock = redisPersistentLocker.acquireLock(AcquiredLock.class, "cba", Duration.ofMinutes(1))) {
      body = true;
    } catch (RuntimeException ex) {
      assertThat(ex.getMessage()).isEqualTo("Failed to acquire distributed lock for io.harness.lock.AcquiredLock-cba");
    }

    assertThat(body).isFalse();
    verify(rLock, times(1)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testTryAcquireLockDoNotThrowException() throws InterruptedException {
    RLock rLock = mock(RLock.class);
    when(client.getLock(anyString())).thenReturn(rLock);
    when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

    try (AcquiredLock lock = redisPersistentLocker.tryToAcquireLock(AcquiredLock.class, "cba", Duration.ofMinutes(1))) {
      assertThat(lock).isNull();
    }

    verify(rLock, times(1)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testAcquireLockNonLockedAtRelease() throws InterruptedException {
    RLock rLock = mock(RLock.class);
    when(client.getLock(anyString())).thenReturn(rLock);
    when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

    try (AcquiredLock lock = redisPersistentLocker.acquireLock(AcquiredLock.class, "cba", Duration.ofMinutes(1))) {
      assertThat(lock).isNotNull();
    }

    verify(rLock, times(1)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testWaitToAcquireLock() throws InterruptedException {
    RLock rLock = mock(RLock.class);
    when(client.getLock(anyString())).thenReturn(rLock);
    when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

    try (AcquiredLock lock = redisPersistentLocker.waitToAcquireLock(
             AcquiredLock.class, "cba", Duration.ofMinutes(1), Duration.ofMinutes(2))) {
      assertThat(lock).isNotNull();
    }

    verify(rLock, times(1)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
  }
}

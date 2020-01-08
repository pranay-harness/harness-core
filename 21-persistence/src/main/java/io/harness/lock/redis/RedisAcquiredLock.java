package io.harness.lock.redis;

import io.harness.lock.AcquiredLock;
import lombok.Builder;
import lombok.Getter;
import org.redisson.api.RLock;

@Builder
public class RedisAcquiredLock implements AcquiredLock<RLock> {
  @Getter RLock lock;

  @Override
  public void release() {
    if (lock != null && lock.isLocked()) {
      lock.unlock();
    }
  }

  @Override
  public void close() {
    if (lock != null && lock.isLocked()) {
      lock.unlock();
    }
  }
}

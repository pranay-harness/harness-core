package io.harness.lock.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.lock.AcquiredLock;

import lombok.Builder;
import lombok.Getter;
import org.redisson.api.RLock;

@OwnedBy(PL)
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

package io.harness.lock.noop;

import io.harness.lock.AcquiredLock;

import java.util.concurrent.locks.Lock;
import lombok.Builder;

@Builder
public class AcquiredNoopLock implements AcquiredLock {
  @Override
  public Lock getLock() {
    return null;
  }

  @Override
  public void release() {
    // noop release
  }

  @Override
  public void close() {
    // noop close
  }
}

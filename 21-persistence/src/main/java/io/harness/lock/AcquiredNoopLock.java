package io.harness.lock;

import com.deftlabs.lock.mongo.DistributedLock;
import lombok.Builder;

@Builder
public class AcquiredNoopLock implements AcquiredLock {
  @Override
  public DistributedLock getLock() {
    return null;
  }

  @Override
  public void release() {}

  @Override
  public void close() {}
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

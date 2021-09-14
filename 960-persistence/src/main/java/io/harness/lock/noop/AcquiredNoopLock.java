/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.lock.noop;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.lock.AcquiredLock;

import java.util.concurrent.locks.Lock;
import lombok.Builder;

@OwnedBy(PL)
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

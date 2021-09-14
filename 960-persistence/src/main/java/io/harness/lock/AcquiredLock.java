/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.lock;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;

@OwnedBy(PL)
public interface AcquiredLock<T extends Lock> extends Closeable {
  T getLock();
  void release();
  @Override void close();
}

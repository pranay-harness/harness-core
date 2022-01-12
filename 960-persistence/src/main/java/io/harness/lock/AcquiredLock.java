/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

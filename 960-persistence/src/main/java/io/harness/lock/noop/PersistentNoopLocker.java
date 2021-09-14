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
import io.harness.lock.PersistentLocker;

import io.dropwizard.lifecycle.Managed;
import java.time.Duration;

@OwnedBy(PL)
public class PersistentNoopLocker implements PersistentLocker, Managed {
  @Override
  public AcquiredLock acquireLock(String name, Duration timeout) {
    return new AcquiredNoopLock();
  }

  @Override
  public AcquiredLock acquireEphemeralLock(String name, Duration timeout) {
    return new AcquiredNoopLock();
  }

  @Override
  public AcquiredLock acquireLock(Class entityClass, String entityId, Duration timeout) {
    return new AcquiredNoopLock();
  }

  @Override
  public AcquiredLock tryToAcquireLock(Class entityClass, String entityId, Duration timeout) {
    return new AcquiredNoopLock();
  }

  @Override
  public AcquiredLock tryToAcquireEphemeralLock(Class entityClass, String entityId, Duration timeout) {
    return new AcquiredNoopLock();
  }

  @Override
  public AcquiredLock tryToAcquireLock(String name, Duration timeout) {
    return new AcquiredNoopLock();
  }

  @Override
  public AcquiredLock tryToAcquireInfiniteLockWithPeriodicRefresh(String name, Duration waitTime) {
    return new AcquiredNoopLock();
  }

  @Override
  public AcquiredLock tryToAcquireEphemeralLock(String name, Duration timeout) {
    return new AcquiredNoopLock();
  }

  @Override
  public AcquiredLock waitToAcquireLock(
      Class entityClass, String entityId, Duration lockTimeout, Duration waitTimeout) {
    return new AcquiredNoopLock();
  }

  @Override
  public AcquiredLock waitToAcquireLock(String name, Duration lockTimeout, Duration waitTimeout) {
    return new AcquiredNoopLock();
  }

  @Override
  public void destroy(AcquiredLock acquiredLock) {
    // Nothing to do
  }

  @Override
  public void start() throws Exception {
    // Nothing to do
  }

  @Override
  public void stop() throws Exception {
    // Nothing to do
  }
}

package io.harness.lock.noop;

import io.dropwizard.lifecycle.Managed;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;

import java.time.Duration;

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

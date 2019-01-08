package io.harness.lock;

import java.time.Duration;

public class PersistentNoopLocker implements Locker {
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
  public void destroy(AcquiredLock acquiredLock) {}
}

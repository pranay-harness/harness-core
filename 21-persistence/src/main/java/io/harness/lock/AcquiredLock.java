package io.harness.lock;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;

public interface AcquiredLock<T extends Lock> extends Closeable {
  T getLock();
  void release();
  @Override void close();
}

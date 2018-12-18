package io.harness.lock;

import com.deftlabs.lock.mongo.DistributedLock;

import java.io.Closeable;

public interface AcquiredLock extends Closeable {
  DistributedLock getLock();
  void release();
  void close();
}

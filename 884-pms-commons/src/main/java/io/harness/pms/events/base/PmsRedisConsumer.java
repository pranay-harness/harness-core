package io.harness.pms.events.base;

public interface PmsRedisConsumer extends Runnable {
  void shutDown();
}

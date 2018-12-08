package io.harness.queue;

public interface QueueController {
  boolean isPrimary();
  boolean isNotPrimary();
}

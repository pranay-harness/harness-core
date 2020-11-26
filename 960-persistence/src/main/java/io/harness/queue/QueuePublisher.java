package io.harness.queue;

import java.util.List;

/**
 * The Interface Queue.
 */
public interface QueuePublisher<T extends Queuable> extends Queue {
  void send(T payload);
  void send(List<String> additionalTopicElements, T payload);
}

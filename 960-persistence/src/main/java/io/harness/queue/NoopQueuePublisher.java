package io.harness.queue;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopQueuePublisher<T extends Queuable> implements QueuePublisher<T> {
  @Override
  public String getName() {
    return null;
  }

  @Override
  public void send(T payload) {
    // noop
  }

  @Override
  public void send(List<String> additionalTopicElements, T payload) {
    // noop
  }
}

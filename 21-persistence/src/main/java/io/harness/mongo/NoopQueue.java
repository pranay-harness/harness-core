package io.harness.mongo;

import io.harness.queue.Queuable;
import io.harness.queue.Queue;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Date;

@Slf4j
public class NoopQueue<T extends Queuable> implements Queue<T> {
  @Override
  public T get(Duration wait, Duration poll) {
    return null;
  }

  @Override
  public void updateHeartbeat(T message) {
    // noop
  }

  @Override
  public long count(Filter filter) {
    return 0;
  }

  @Override
  public void ack(T message) {
    // noop
  }

  @Override
  public void requeue(String id, int retries) {
    // noop
  }

  @Override
  public void requeue(String id, int retries, Date earliestGet) {
    // noop
  }

  @Override
  public void send(T payload) {
    // noop
  }

  @Override
  public Duration heartbeat() {
    return null;
  }

  @Override
  public String name() {
    return null;
  }
}

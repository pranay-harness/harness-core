/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.queue;

import java.time.Duration;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopQueueConsumer<T extends Queuable> implements QueueConsumer<T> {
  @Override
  public T get(Duration wait, Duration poll) {
    return null;
  }

  @Override
  public void updateHeartbeat(T message) {
    // noop
  }

  @Override
  public String getName() {
    return null;
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
  public Duration heartbeat() {
    return null;
  }
}

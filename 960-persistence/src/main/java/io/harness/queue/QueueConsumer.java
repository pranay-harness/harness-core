/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queue;

import java.time.Duration;
import java.util.Date;

/**
 * The Interface Queue.
 */
public interface QueueConsumer<T extends Queuable> extends Queue {
  T get(Duration wait, Duration poll);
  void updateHeartbeat(T message);

  enum Filter { ALL, RUNNING, NOT_RUNNING }
  long count(Filter filter);

  void ack(T message);
  void requeue(String id, int retries);
  void requeue(String id, int retries, Date earliestGet);
  Duration heartbeat();
}

/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

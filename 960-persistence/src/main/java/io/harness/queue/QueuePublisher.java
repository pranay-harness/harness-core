/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queue;

import java.util.List;

/**
 * The Interface Queue.
 */
public interface QueuePublisher<T extends Queuable> extends Queue {
  void send(T payload);
  void send(List<String> additionalTopicElements, T payload);
}

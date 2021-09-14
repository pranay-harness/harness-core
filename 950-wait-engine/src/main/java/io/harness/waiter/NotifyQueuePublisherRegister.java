/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class NotifyQueuePublisherRegister {
  Map<String, NotifyQueuePublisher> registry = new HashMap<>();

  public void register(String name, NotifyQueuePublisher publisher) {
    registry.put(name, publisher);
  }

  public NotifyQueuePublisher obtain(String publisherName) {
    return registry.get(publisherName);
  }
}

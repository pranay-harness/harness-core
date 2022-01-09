/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

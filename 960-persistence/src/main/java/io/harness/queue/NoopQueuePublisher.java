/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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

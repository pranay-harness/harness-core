/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.eventsframework.impl.noop;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.AbstractConsumer;
import io.harness.eventsframework.consumer.Message;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@OwnedBy(PL)
public class NoOpConsumer extends AbstractConsumer {
  public NoOpConsumer(String topicName, String groupName) {
    super(topicName, groupName);
  }

  @Override
  public List<Message> read(Duration maxWaitTime) {
    return Collections.emptyList();
  }

  @Override
  public void acknowledge(String messageId) {}

  @Override
  public void shutdown() {}

  public static NoOpConsumer of(String topicName, String groupName) {
    return new NoOpConsumer(topicName, groupName);
  }
}

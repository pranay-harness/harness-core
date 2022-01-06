/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.eventsframework.impl.noop;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.producer.Message;

@OwnedBy(PL)
public class NoOpProducer extends AbstractProducer {
  public NoOpProducer(String topicName) {
    super(topicName, "dummyProducer");
  }

  @Override
  public String send(Message message) {
    return "dummy-message-id";
  }

  @Override
  public void shutdown() {
    // Nothing required to shutdown
  }

  public static NoOpProducer of(String topicName) {
    return new NoOpProducer(topicName);
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.eventsframework.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(PL)
public abstract class AbstractProducer implements Producer {
  @Getter private final String topicName;
  @Getter private final String producerName;

  protected AbstractProducer(String topicName, String producerName) {
    this.topicName = topicName;
    this.producerName = producerName;
  }
}

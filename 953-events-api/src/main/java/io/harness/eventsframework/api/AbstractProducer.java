/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

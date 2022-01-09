/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.eventsframework.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(PL)
public abstract class AbstractConsumer implements Consumer {
  private static final int CONSUMER_NAME_LENGTH = 4;
  @Getter private final String topicName;
  @Getter private final String groupName;
  @Getter private final String name;

  protected AbstractConsumer(String topicName, String groupName) {
    this.topicName = topicName;
    this.groupName = groupName;
    this.name = randomAlphabetic(CONSUMER_NAME_LENGTH);
  }

  protected AbstractConsumer(String topicName, String groupName, String consumerName) {
    this.topicName = topicName;
    this.groupName = groupName;

    // Used when we want only one consumer per consumer group for sequential processing
    this.name = consumerName;
  }
}

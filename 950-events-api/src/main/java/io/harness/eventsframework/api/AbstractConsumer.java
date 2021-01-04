package io.harness.eventsframework.api;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import lombok.Getter;

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

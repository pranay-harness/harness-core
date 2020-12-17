package io.harness.eventsframework.api;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import lombok.Getter;

public abstract class AbstractConsumer implements Consumer {
  protected static final int CONSUMER_NAME_LENGTH = 4;
  protected String topicName;
  protected String groupName;
  @Getter protected String name;

  public AbstractConsumer(String topicName, String groupName) {
    this.topicName = topicName;
    this.groupName = groupName;
    this.name = randomAlphabetic(CONSUMER_NAME_LENGTH);
  }

  public AbstractConsumer(String topicName, String groupName, String consumerName) {
    this.topicName = topicName;
    this.groupName = groupName;

    // Used when we want only one consumer per consumer group for sequential processing
    this.name = consumerName;
  }
}

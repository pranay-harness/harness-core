package io.harness.eventsframework.api;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class AbstractConsumer implements Consumer {
  protected static int CONSUMER_NAME_LENGTH = 4;
  protected String topicName;
  protected String groupName;
  @Getter protected String name;

  public AbstractConsumer(String topicName, String groupName) {
    this.topicName = topicName;
    this.groupName = groupName;
    this.name = randomAlphabetic(CONSUMER_NAME_LENGTH);
  }
}

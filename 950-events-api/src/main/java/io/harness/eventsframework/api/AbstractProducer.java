package io.harness.eventsframework.api;

import lombok.Getter;

public abstract class AbstractProducer implements Producer {
  @Getter private final String topicName;
  @Getter private final String producerName;

  protected AbstractProducer(String topicName, String producerName) {
    this.topicName = topicName;
    this.producerName = producerName;
  }
}

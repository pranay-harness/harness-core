package io.harness.eventsframework.api;

import lombok.Getter;

public abstract class AbstractProducer implements Producer {
  @Getter private final String topicName;

  protected AbstractProducer(String topicName) {
    this.topicName = topicName;
  }
}

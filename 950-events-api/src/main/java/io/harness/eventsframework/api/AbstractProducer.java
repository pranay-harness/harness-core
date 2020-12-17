package io.harness.eventsframework.api;

public abstract class AbstractProducer implements Producer {
  protected final String topicName;

  public AbstractProducer(String topicName) {
    this.topicName = topicName;
  }
}

package io.harness.eventsframework.api;

import io.harness.eventsframework.producer.Message;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class AbstractProducer implements Producer {
  private String topicName;
}

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

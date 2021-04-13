package io.harness.eventsframework.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public class ProducerShutdownException extends Exception {
  public ProducerShutdownException(String message) {
    super(message);
  }
}

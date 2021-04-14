package io.harness.eventsframework.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public class ConsumerShutdownException extends Exception {
  public ConsumerShutdownException(String message) {
    super(message);
  }
}

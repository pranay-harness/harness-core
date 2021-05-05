package io.harness.eventsframework.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public class EventsFrameworkDownException extends RuntimeException {
  public EventsFrameworkDownException(String message) {
    super(message);
  }
}

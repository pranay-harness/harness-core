package io.harness.event.model;

public interface Event {
  EventType getEventType();

  EventData getData();
}

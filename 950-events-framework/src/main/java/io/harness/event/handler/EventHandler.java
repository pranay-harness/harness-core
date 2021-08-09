package io.harness.event.handler;

import io.harness.event.model.Event;

public interface EventHandler {
  void handleEvent(Event event);
}

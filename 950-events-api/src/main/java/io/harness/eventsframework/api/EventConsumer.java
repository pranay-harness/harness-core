package io.harness.eventsframework.api;

public interface EventConsumer {
  EventFilter getEventFilter();
  EventHandler getEventHandler();
}

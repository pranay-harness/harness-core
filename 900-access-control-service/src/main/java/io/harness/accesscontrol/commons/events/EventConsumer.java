package io.harness.accesscontrol.commons.events;

public interface EventConsumer {
  EventFilter getEventFilter();
  EventHandler getEventHandler();
}

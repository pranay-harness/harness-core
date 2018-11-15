package io.harness.event.listener;

import io.harness.event.handler.EventHandler;
import io.harness.event.model.Event;
import io.harness.event.model.EventType;

import java.util.List;

public interface EventListener {
  void onEvent(Event event);

  void registerEventHandler(EventHandler handler, List<EventType> eventType);

  void deregisterEventHandler(EventHandler handler, List<EventType> eventType);
}

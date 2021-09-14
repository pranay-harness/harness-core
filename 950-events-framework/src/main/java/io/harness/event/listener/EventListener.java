/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.listener;

import io.harness.event.handler.EventHandler;
import io.harness.event.model.EventType;

import java.util.Set;

public interface EventListener {
  void registerEventHandler(EventHandler handler, Set<EventType> eventTypes);

  void deregisterEventHandler(EventHandler handler, Set<EventType> eventTypes);
}

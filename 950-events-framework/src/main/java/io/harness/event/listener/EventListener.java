/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.listener;

import io.harness.event.handler.EventHandler;
import io.harness.event.model.EventType;

import java.util.Set;

public interface EventListener {
  void registerEventHandler(EventHandler handler, Set<EventType> eventTypes);

  void deregisterEventHandler(EventHandler handler, Set<EventType> eventTypes);
}

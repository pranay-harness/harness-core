/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.handler;

import io.harness.event.model.Event;

public interface EventHandler {
  void handleEvent(Event event);
}

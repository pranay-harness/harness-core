/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.publisher;

import io.harness.event.model.Event;

public interface EventPublisher {
  void publishEvent(Event event) throws EventPublishException;
}

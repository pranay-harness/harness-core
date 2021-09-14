/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala
 */
@Data
@Builder
public class Event {
  private EventData eventData;
  private EventType eventType;
}

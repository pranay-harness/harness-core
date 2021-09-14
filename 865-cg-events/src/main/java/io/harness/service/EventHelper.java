/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Event;
import io.harness.beans.EventConfig;

@OwnedBy(CDC)
public interface EventHelper {
  default boolean canSendEvent(EventConfig eventConfig, Event event, String appId) {
    return false;
  }
}

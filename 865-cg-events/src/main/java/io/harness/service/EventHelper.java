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

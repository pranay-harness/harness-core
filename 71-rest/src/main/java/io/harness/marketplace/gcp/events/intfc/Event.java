package io.harness.marketplace.gcp.events.intfc;

import io.harness.marketplace.gcp.events.EventType;

/**
 * Interface representing GCP marketplace event
 */
public interface Event {
  EventType getEventType();
  String getEventId();
}

package io.harness.marketplace.gcp.procurement.pubsub;

import io.harness.marketplace.gcp.procurement.ProcurementEventType;

/**
 * Interface representing GCP marketplace event
 */
public interface Event {
  ProcurementEventType getEventType();
  String getEventId();
}

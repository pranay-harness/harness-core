package io.harness.marketplace.gcp.events;

public enum EventType {
  ACCOUNT_ACTIVE, // this is sent on account creation
  ENTITLEMENT_CREATION_REQUESTED,
  ENTITLEMENT_CANCELLED,
  ENTITLEMENT_DELETED,
  ENTITLEMENT_ACTIVE
}

package io.harness.ng.core.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class ServiceOutboxEvents {
  public static final String SERVICE_CREATED = "ServiceCreated";
  public static final String SERVICE_UPDATED = "ServiceUpdated";
  public static final String SERVICE_DELETED = "ServiceDeleted";
  public static final String SERVICE_UPSERTED = "ServiceUpserted";
}

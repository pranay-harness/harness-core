package io.harness.marketplace.gcp.events;

import java.util.Optional;

public interface GcpMarketplaceEventService {
  GcpMarketplaceEvent save(GcpMarketplaceEvent event);

  Optional<GcpMarketplaceEvent> getEvent(String gcpAccountId, EventType type);
}

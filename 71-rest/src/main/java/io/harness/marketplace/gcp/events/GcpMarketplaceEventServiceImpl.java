package io.harness.marketplace.gcp.events;

import com.google.inject.Inject;

import io.harness.persistence.HQuery;
import software.wings.dl.WingsPersistence;

import java.util.Optional;

public class GcpMarketplaceEventServiceImpl implements GcpMarketplaceEventService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public GcpMarketplaceEvent save(GcpMarketplaceEvent event) {
    wingsPersistence.save(event);
    return event;
  }

  @Override
  public Optional<GcpMarketplaceEvent> getEvent(String gcpAccountId, EventType type) {
    GcpMarketplaceEvent event = wingsPersistence.createQuery(GcpMarketplaceEvent.class, HQuery.excludeAuthority)
                                    .disableValidation()
                                    .field("event.eventType")
                                    .equal(type)
                                    .field("event.account.id")
                                    .equal(gcpAccountId)
                                    .get();

    return Optional.ofNullable(event);
  }
}

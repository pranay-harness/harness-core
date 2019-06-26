package io.harness.marketplace.gcp.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CreateAccountEventHandler extends BaseEvent {
  @Inject private GcpMarketplaceEventService marketplaceEventService;

  public void handle(String messageId, AccountActiveEvent event) {
    marketplaceEventService.save(new GcpMarketplaceEvent(messageId, event));
  }
}

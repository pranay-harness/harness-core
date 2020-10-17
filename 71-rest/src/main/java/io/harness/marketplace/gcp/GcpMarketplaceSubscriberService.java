package io.harness.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import io.harness.annotations.dev.OwnedBy;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class GcpMarketplaceSubscriberService implements Managed {
  @Inject private GcpMarketplaceTopicSubscriber gcpMarketplaceTopicSubscriber;

  @Override
  public void start() {
    try {
      gcpMarketplaceTopicSubscriber.subscribeAsync();
    } catch (Exception e) {
      logger.error("Could not subscribe to GCP marketplace Topic.", e);
    }
  }

  @Override
  public void stop() {
    gcpMarketplaceTopicSubscriber.stopAsync();
  }
}

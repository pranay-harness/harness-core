package io.harness.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import io.harness.annotations.dev.OwnedBy;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;

@OwnedBy(PL)
@Slf4j
@Singleton
public class GcpMarketplaceSubscriberService implements Managed {
  @Inject private GcpMarketplaceTopicSubscriber gcpMarketplaceTopicSubscriber;
  @Inject private MainConfiguration configuration;

  @Override
  public void start() {
    if (!configuration.getGcpMarketplaceConfig().isEnabled()) {
      log.info("Skipping subscribing to GCP marketplace");
      return;
    }

    try {
      gcpMarketplaceTopicSubscriber.subscribeAsync();
    } catch (Exception e) {
      log.error("Could not subscribe to GCP marketplace Topic.", e);
    }
  }

  @Override
  public void stop() {
    if (configuration.getGcpMarketplaceConfig().isEnabled()) {
      gcpMarketplaceTopicSubscriber.stopAsync();
    }
  }
}

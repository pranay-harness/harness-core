/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.app.MainConfiguration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;

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

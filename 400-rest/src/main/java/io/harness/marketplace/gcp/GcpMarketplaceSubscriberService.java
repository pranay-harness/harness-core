/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

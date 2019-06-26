package io.harness.marketplace.gcp;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;

@Slf4j
@Singleton
public class GcpMarketplaceSubscriberService implements Managed {
  @Inject private GcpMarketplaceTopicSubscriber gcpMarketplaceTopicSubscriber;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public void start() throws Exception {
    try {
      boolean featEnabled =
          featureFlagService.isEnabled(FeatureName.GCP_MARKETPLACE_INTEGRATION, Account.GLOBAL_ACCOUNT_ID);
      if (featEnabled) {
        this.gcpMarketplaceTopicSubscriber.subscribeAsync();
      }
    } catch (Exception e) {
      logger.error(
          "Could not subscribe to GCP marketplace topic. Users will not be able to subscribe from GCP marketplace.", e);
    }
  }

  @Override
  public void stop() throws Exception {
    if (null != this.gcpMarketplaceTopicSubscriber) {
      this.gcpMarketplaceTopicSubscriber.stopAsync();
    } else {
      logger.error("gcpMarketplaceTopicSubscriber is null");
    }
  }
}

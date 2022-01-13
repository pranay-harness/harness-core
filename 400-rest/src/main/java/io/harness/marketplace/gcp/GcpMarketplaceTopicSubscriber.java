/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.marketplace.gcp.GcpMarketPlaceConstants.SERVICE_ACCOUNT_INTEGRATION_PATH;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handler.impl.segment.SegmentHelper;
import io.harness.exception.GcpMarketplaceException;
import io.harness.marketplace.gcp.procurement.GcpMarketplaceMessageReceiver;
import io.harness.marketplace.gcp.procurement.GcpProcurementService;
import io.harness.marketplace.gcp.procurement.GcpProductsRegistry;

import software.wings.app.MainConfiguration;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.Subscription;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

@OwnedBy(PL)
@Slf4j
@Singleton
public class GcpMarketplaceTopicSubscriber {
  // This topic is on Google's side
  private static final String FULL_TOPIC_NAME = "projects/cloudcommerceproc-prod/topics/harness-public";
  private static final String GCP_PROJECT_ID = "harness-public";
  private static final String FULL_SUBSCRIPTION_FORMAT = "projects/{}/subscriptions/{}";

  private Subscriber subscriber;
  @Inject private GcpProcurementService gcpProcurementService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private GcpProductsRegistry gcpProductsRegistry;
  @Inject private MainConfiguration configuration;
  @Inject private SegmentHelper segmentHelper;

  public void subscribeAsync() throws IOException {
    String gcpSubscriptionName = configuration.getGcpMarketplaceConfig().getSubscriptionName();
    String fullSubscriptionName =
        MessageFormatter.format(FULL_SUBSCRIPTION_FORMAT, GCP_PROJECT_ID, gcpSubscriptionName).getMessage();

    FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials());
    SubscriptionAdminSettings subscriptionAdminSettings =
        SubscriptionAdminSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();

    try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
      Subscription sub = Subscription.newBuilder().setName(fullSubscriptionName).setTopic(FULL_TOPIC_NAME).build();
      try {
        subscriptionAdminClient.createSubscription(sub);
      } catch (AlreadyExistsException e) {
        log.info("Subscription Already Exists. Subscription Name: {} Exception: {}", fullSubscriptionName, e);
      }
    }

    ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(GCP_PROJECT_ID, gcpSubscriptionName);
    subscriber = Subscriber
                     .newBuilder(projectSubscriptionName,
                         new GcpMarketplaceMessageReceiver(gcpProcurementService, wingsPersistence, accountService,
                             gcpProductsRegistry, segmentHelper))
                     .setCredentialsProvider(credentialsProvider)
                     .build();

    log.info("Starting listening to pub/sub topic: {}; subscription: {}", FULL_TOPIC_NAME, gcpSubscriptionName);
    try {
      subscriber.startAsync().awaitRunning(1, TimeUnit.MINUTES);
    } catch (TimeoutException e) {
      log.error("Failed to start listening to pub/sub topic: {}", FULL_TOPIC_NAME);
      throw new GcpMarketplaceException(
          String.format("Failed to start listening to pub/sub topic: %s.", FULL_TOPIC_NAME), e);
    }
  }

  public void stopAsync() {
    log.info("Stopping listening to pub/sub topic: {}", FULL_TOPIC_NAME);
    subscriber.stopAsync();
  }

  private Credentials credentials() throws IOException {
    Path credentialsPath = Paths.get(SERVICE_ACCOUNT_INTEGRATION_PATH);
    InputStream credentialStream = Files.newInputStream(credentialsPath, StandardOpenOption.READ);
    return GoogleCredentials.fromStream(credentialStream);
  }
}

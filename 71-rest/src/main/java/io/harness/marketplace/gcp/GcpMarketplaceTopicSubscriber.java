package io.harness.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.marketplace.gcp.GcpMarketPlaceConstants.SERVICE_ACCOUNT_INTEGRATION_PATH;

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

import io.harness.annotations.dev.OwnedBy;
import io.harness.marketplace.gcp.procurement.GcpMarketplaceMessageReceiver;
import io.harness.marketplace.gcp.procurement.GcpProcurementService;
import io.harness.marketplace.gcp.procurement.GcpProductsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@OwnedBy(PL)
@Slf4j
@Singleton
public class GcpMarketplaceTopicSubscriber {
  // This topic is on Google's side
  private static final String FULL_TOPIC_NAME = "projects/cloudcommerceproc-prod/topics/harness-public";
  private static final String GCP_PROJECT_ID = "harness-public";
  private static final String GCP_SUBSCRIPTION_NAME = "marketplace-topic-subscriber";
  private static final String FULL_SUBSCRIPTION_FORMAT = "projects/{}/subscriptions/{}";
  private static final String FULL_SUBSCRIPTION_NAME =
      MessageFormatter.format(FULL_SUBSCRIPTION_FORMAT, GCP_PROJECT_ID, GCP_SUBSCRIPTION_NAME).getMessage();

  private Subscriber subscriber;
  @Inject private GcpProcurementService gcpProcurementService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private GcpProductsRegistry gcpProductsRegistry;

  public void subscribeAsync() throws IOException {
    FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials());
    SubscriptionAdminSettings subscriptionAdminSettings =
        SubscriptionAdminSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();

    try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
      Subscription sub = Subscription.newBuilder().setName(FULL_SUBSCRIPTION_NAME).setTopic(FULL_TOPIC_NAME).build();
      try {
        subscriptionAdminClient.createSubscription(sub);
      } catch (AlreadyExistsException e) {
        log.info("Subscription Already Exists. Subscription Name: {} Exception: {}", FULL_SUBSCRIPTION_NAME, e);
      }
    }

    ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(GCP_PROJECT_ID, GCP_SUBSCRIPTION_NAME);
    subscriber = Subscriber
                     .newBuilder(projectSubscriptionName,
                         new GcpMarketplaceMessageReceiver(
                             gcpProcurementService, wingsPersistence, accountService, gcpProductsRegistry))
                     .setCredentialsProvider(credentialsProvider)
                     .build();

    log.info("Starting listening to pub/sub topic: {}", FULL_TOPIC_NAME);
    subscriber.startAsync();
  }

  public void stopAsync() {
    subscriber.stopAsync();
  }

  private Credentials credentials() throws IOException {
    Path credentialsPath = Paths.get(SERVICE_ACCOUNT_INTEGRATION_PATH);
    InputStream credentialStream = Files.newInputStream(credentialsPath, StandardOpenOption.READ);
    return GoogleCredentials.fromStream(credentialStream);
  }
}

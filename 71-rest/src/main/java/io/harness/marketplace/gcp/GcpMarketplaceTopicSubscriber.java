package io.harness.marketplace.gcp;

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

import io.harness.marketplace.gcp.events.CreateAccountEventHandler;
import io.harness.marketplace.gcp.events.GcpMarketplaceMessageReceiver;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@Slf4j
@Singleton
public class GcpMarketplaceTopicSubscriber {
  // This topic is on Google's side
  private static final String FULL_TOPIC_NAME = "projects/cloudcommerceproc-prod/topics/harness-public";

  // TODO(jatin): use Kubernetes secret instead
  private static final String GCP_PROJECT_ID = "harness-public";
  private static final String GCP_SUBSCRIPTION_NAME = "marketplace-topic-subscriber";

  private static final String FULL_SUBSCRIPTION_FORMAT = "projects/{}/subscriptions/{}";
  private static final String FULL_SUBSCRIPTION_NAME =
      MessageFormatter.format(FULL_SUBSCRIPTION_FORMAT, GCP_PROJECT_ID, GCP_SUBSCRIPTION_NAME).getMessage();

  private Subscriber subscriber;
  @Inject private CreateAccountEventHandler createAccountEventHandler;

  public void subscribeAsync() throws IOException {
    final Optional<Credentials> credentialsMaybe = credentials();
    Credentials credentials;

    if (credentialsMaybe.isPresent()) {
      credentials = credentialsMaybe.get();
    } else {
      logger.error("Could not get credentials. Will not subscribe.");
      return;
    }

    final FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);
    SubscriptionAdminSettings subscriptionAdminSettings =
        SubscriptionAdminSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();

    try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
      Subscription sub = Subscription.newBuilder().setName(FULL_SUBSCRIPTION_NAME).setTopic(FULL_TOPIC_NAME).build();
      try {
        subscriptionAdminClient.createSubscription(sub);
      } catch (AlreadyExistsException e) {
        logger.info(
            "Subscription Already Exists. Subscription Name:{} Message: {}", FULL_SUBSCRIPTION_NAME, e.getMessage());
      }
    }

    ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(GCP_PROJECT_ID, GCP_SUBSCRIPTION_NAME);

    logger.info("Starting listening to pub/sub topic: {}", FULL_TOPIC_NAME);
    this.subscriber =
        Subscriber.newBuilder(projectSubscriptionName, new GcpMarketplaceMessageReceiver(createAccountEventHandler))
            .setCredentialsProvider(credentialsProvider)
            .build();
    this.subscriber.startAsync();
  }

  public void stopAsync() {
    if (null != this.subscriber) {
      this.subscriber.stopAsync();
    }
  }

  private Optional<Credentials> credentials() throws IOException {
    Path credentialsPath = Paths.get(GcpMarketPlaceConstants.SERVICE_ACCOUNT_INTEGRATION_PATH);
    if (Files.exists(credentialsPath)) {
      InputStream credentialStream = Files.newInputStream(credentialsPath, StandardOpenOption.READ);
      return Optional.of(GoogleCredentials.fromStream(credentialStream));
    } else {
      logger.error("GCP Credentials don't exist at path: {}", GcpMarketPlaceConstants.SERVICE_ACCOUNT_INTEGRATION_PATH);
      return Optional.empty();
    }
  }
}

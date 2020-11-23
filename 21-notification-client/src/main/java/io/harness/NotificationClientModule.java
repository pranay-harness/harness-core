package io.harness;

import io.harness.govern.ProviderModule;
import io.harness.messageclient.MessageClient;
import io.harness.mongo.queue.NGMongoQueuePublisher;
import io.harness.ng.MongoNotificationRequest;
import io.harness.notification.NotificationClientBackendConfiguration;
import io.harness.notification.NotificationClientConfiguration;
import io.harness.notificationclient.NotificationClient;
import io.harness.notificationclient.NotificationClientImpl;
import io.harness.queue.QueuePublisher;
import io.harness.remote.NotificationHTTPClient;
import io.harness.remote.NotificationHTTPFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import org.springframework.data.mongodb.core.MongoTemplate;

public class NotificationClientModule extends AbstractModule {
  private final NotificationClientConfiguration notificationClientConfiguration;

  public NotificationClientModule(NotificationClientConfiguration configuration) {
    this.notificationClientConfiguration = configuration;
  }

  @Override
  protected void configure() {
    install(new ProviderModule() {
      @Provides
      @Singleton
      NotificationClientBackendConfiguration getNotificationClientConfiguration() {
        return notificationClientConfiguration.getNotificationClientBackendConfiguration();
      }
    });
    assert (notificationClientConfiguration.getNotificationClientBackendConfiguration().getType().equalsIgnoreCase(
        "mongo"));
    bind(MessageClient.class).to(io.harness.messageclient.MongoClient.class);
    bind(NotificationHTTPClient.class).toProvider(NotificationHTTPFactory.class).in(Scopes.SINGLETON);
    bind(NotificationClient.class).to(NotificationClientImpl.class);
  }

  @Provides
  @Singleton
  public QueuePublisher<MongoNotificationRequest> getQueuePublisher(
      @Named("notification-channel") MongoTemplate mongoTemplate) {
    return new NGMongoQueuePublisher<>("Notification-queue", new ArrayList<>(), mongoTemplate);
  }

  @Provides
  private NotificationHTTPFactory notificationHTTPFactory(KryoConverterFactory kryoConverterFactory) {
    ServiceHttpClientConfig serviceHttpClientConfig = this.notificationClientConfiguration.getServiceHttpClientConfig();
    return new NotificationHTTPFactory(serviceHttpClientConfig,
        this.notificationClientConfiguration.getNotificationSecrets().getNotificationClientSecret(),
        new ServiceTokenGenerator(), kryoConverterFactory);
  }
}

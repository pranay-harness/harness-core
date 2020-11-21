package io.harness;

import io.harness.govern.ProviderModule;
import io.harness.messageclient.KafkaClient;
import io.harness.messageclient.MessageClient;
import io.harness.mongo.queue.NGMongoQueuePublisher;
import io.harness.ng.MongoNotificationRequest;
import io.harness.notification.KafkaBackendConfiguration;
import io.harness.notification.MongoBackendConfiguration;
import io.harness.notification.NotificationClientBackendConfiguration;
import io.harness.notification.NotificationClientConfiguration;
import io.harness.notificationclient.NotificationClient;
import io.harness.notificationclient.NotificationClientImpl;
import io.harness.queue.QueuePublisher;
import io.harness.remote.NotificationHTTPClient;
import io.harness.remote.NotificationHTTPFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.*;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.mongodb.core.MongoTemplate;

public class NotificationClientModule extends AbstractModule {
  private final NotificationClientConfiguration notificationClientConfiguration;

  public NotificationClientModule(NotificationClientConfiguration configuration) {
    this.notificationClientConfiguration = configuration;
  }

  private MongoClient getMongoClient(MongoBackendConfiguration mongoConfig) {
    MongoClientOptions primaryMongoClientOptions = MongoClientOptions.builder()
                                                       .retryWrites(true)
                                                       .connectTimeout(mongoConfig.getConnectTimeout())
                                                       .serverSelectionTimeout(mongoConfig.getServerSelectionTimeout())
                                                       .maxConnectionIdleTime(mongoConfig.getMaxConnectionIdleTime())
                                                       .connectionsPerHost(mongoConfig.getConnectionsPerHost())
                                                       .readPreference(ReadPreference.primary())
                                                       .build();
    MongoClientURI uri =
        new MongoClientURI(mongoConfig.getUri(), MongoClientOptions.builder(primaryMongoClientOptions));
    return new MongoClient(uri);
  }

  @Override
  protected void configure() {
    install(new ProviderModule() {
      @Provides
      @Singleton
      NotificationClientBackendConfiguration getNotificationClientConfiguration() {
        return notificationClientConfiguration.getNotificationClientBackendConfiguration();
      }

      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().build();
      }
    });

    bindMessagePublisher();
    bind(NotificationHTTPClient.class).toProvider(NotificationHTTPFactory.class).in(Scopes.SINGLETON);
    bind(NotificationClient.class).to(NotificationClientImpl.class);
  }

  private void bindMessagePublisher() {
    NotificationClientBackendConfiguration backendConfiguration =
        this.notificationClientConfiguration.getNotificationClientBackendConfiguration();
    String type = backendConfiguration.getType();
    if (type.equalsIgnoreCase("KAFKA")) {
      bind(MessageClient.class).toInstance(new KafkaClient((KafkaBackendConfiguration) backendConfiguration));
    } else {
      MongoBackendConfiguration mongoBackendConfiguration = (MongoBackendConfiguration) backendConfiguration;
      MongoClient mongoClient = getMongoClient(mongoBackendConfiguration);
      MongoTemplate mongoTemplate = new MongoTemplate(
          mongoClient, Objects.requireNonNull(new MongoClientURI(mongoBackendConfiguration.getUri()).getDatabase()));
      bind(new TypeLiteral<QueuePublisher<MongoNotificationRequest>>() {
      }).toInstance(new NGMongoQueuePublisher<>("Notification-queue", new ArrayList<>(), mongoTemplate));
      bind(MessageClient.class).to(io.harness.messageclient.MongoClient.class);
    }
  }

  @Provides
  private NotificationHTTPFactory notificationHTTPFactory(KryoConverterFactory kryoConverterFactory) {
    ServiceHttpClientConfig serviceHttpClientConfig = this.notificationClientConfiguration.getServiceHttpClientConfig();
    return new NotificationHTTPFactory(serviceHttpClientConfig,
        this.notificationClientConfiguration.getNotificationSecrets().getNotificationClientSecret(),
        new ServiceTokenGenerator(), kryoConverterFactory);
  }
}

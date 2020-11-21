package io.harness.notification.modules;

import static java.time.Duration.ofSeconds;

import io.harness.mongo.queue.NGMongoQueueConsumer;
import io.harness.ng.MongoNotificationRequest;
import io.harness.notification.*;
import io.harness.notification.eventbackbone.KafkaMessageConsumer;
import io.harness.notification.eventbackbone.MessageConsumer;
import io.harness.notification.eventbackbone.MongoMessageConsumer;
import io.harness.notification.service.*;
import io.harness.notification.service.api.*;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
public class NotificationCoreModule extends AbstractModule {
  NotificationConfiguration appConfig;
  public NotificationCoreModule(NotificationConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  public void configure() {
    install(new UserGroupClientModule(
        appConfig.getServiceHttpClientConfig(), appConfig.getNotificationSecrets().getManagerServiceSecret()));
    bind(ChannelService.class).to(ChannelServiceImpl.class);
    bind(NotificationTemplateService.class).to(NotificationTemplateServiceImpl.class);
    bind(NotificationSettingsService.class).to(NotificationSettingsServiceImpl.class);
    bind(SeedDataPopulaterService.class).to(SeedDataPopulaterServiceImpl.class);
    bind(MailService.class).to(MailServiceImpl.class);
    bind(SlackService.class).to(SlackServiceImpl.class);
    bind(PagerDutyService.class).to(PagerDutyServiceImpl.class);
    bind(MSTeamsService.class).to(MSTeamsServiceImpl.class);
    bind(NotificationService.class).to(NotificationServiceImpl.class);
    bindMessageConsumer();
  }

  private void bindMessageConsumer() {
    NotificationClientBackendConfiguration notificationClientBackendConfiguration = getBackendConfig();
    switch (notificationClientBackendConfiguration.getType()) {
      case "KAFKA":
        log.info("Using Kafka as the message broker");
        bind(MessageConsumer.class).to(KafkaMessageConsumer.class);
        break;
      case "MONGO":
        log.info("Using Mongo as the message broker");
        bindMongoQueueConsumer(notificationClientBackendConfiguration);
        bind(MessageConsumer.class).to(MongoMessageConsumer.class);
        break;
      default:
        log.info("Using NoOp as the message broker");
        bindNoopMessageConsumer();
    }
  }

  private void bindMongoQueueConsumer(NotificationClientBackendConfiguration backendConfiguration) {
    MongoBackendConfiguration mongoBackendConfiguration = (MongoBackendConfiguration) backendConfiguration;
    MongoClient mongoClient = getMongoClient(mongoBackendConfiguration);
    MongoTemplate mongoTemplate = new MongoTemplate(
        mongoClient, Objects.requireNonNull(new MongoClientURI(mongoBackendConfiguration.getUri()).getDatabase()));
    bind(new TypeLiteral<NGMongoQueueConsumer<MongoNotificationRequest>>() {})
        .toInstance(
            new NGMongoQueueConsumer<>(MongoNotificationRequest.class, ofSeconds(5), new ArrayList<>(), mongoTemplate));
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

  private NotificationClientBackendConfiguration getBackendConfig() {
    return appConfig.getNotificationClientConfiguration().getNotificationClientBackendConfiguration();
  }

  private void bindNoopMessageConsumer() {
    bind(MessageConsumer.class).toInstance(() -> {
      try {
        new CountDownLatch(1).await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
  }

  @Provides
  @Singleton
  SmtpConfig getSmtpConfig() {
    return appConfig.getSmtpConfig();
  }

  //  @Provides
  //  @Singleton
  //  QueueConsumer<MongoNotificationRequest> emailQueueConsumer(Injector injector, PublisherConfiguration config) {
  //    return QueueFactory.createQueueConsumer(injector, MongoNotificationRequest.class, ofSeconds(5), null, config);
  //  }

  @Provides
  @Singleton
  KafkaMessageConsumer getKafkaMessageConsumer(NotificationService listener) {
    return new KafkaMessageConsumer((KafkaBackendConfiguration) getBackendConfig(), listener);
  }
}

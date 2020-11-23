package io.harness.notification.modules;

import static java.time.Duration.ofSeconds;

import io.harness.mongo.queue.NGMongoQueueConsumer;
import io.harness.ng.MongoNotificationRequest;
import io.harness.notification.KafkaBackendConfiguration;
import io.harness.notification.NotificationClientBackendConfiguration;
import io.harness.notification.NotificationConfiguration;
import io.harness.notification.SmtpConfig;
import io.harness.notification.eventbackbone.KafkaMessageConsumer;
import io.harness.notification.eventbackbone.MessageConsumer;
import io.harness.notification.eventbackbone.MongoMessageConsumer;
import io.harness.notification.service.*;
import io.harness.notification.service.api.*;
import io.harness.queue.QueueConsumer;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
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
    assert (notificationClientBackendConfiguration.getType().equalsIgnoreCase("mongo"));
    log.info("Using Mongo as the message broker");
    bind(MessageConsumer.class).to(MongoMessageConsumer.class);
  }

  private NotificationClientBackendConfiguration getBackendConfig() {
    return appConfig.getNotificationClientConfiguration().getNotificationClientBackendConfiguration();
  }

  @Provides
  @Singleton
  SmtpConfig getSmtpConfig() {
    return appConfig.getSmtpConfig();
  }

  @Provides
  @Singleton
  QueueConsumer<MongoNotificationRequest> getQueueConsumer(
      Injector injector, @Named("notification-channel") MongoTemplate mongoTemplate) {
    return new NGMongoQueueConsumer<>(MongoNotificationRequest.class, ofSeconds(5), new ArrayList<>(), mongoTemplate);
  }

  @Provides
  @Singleton
  KafkaMessageConsumer getKafkaMessageConsumer(NotificationService listener) {
    return new KafkaMessageConsumer((KafkaBackendConfiguration) getBackendConfig(), listener);
  }
}

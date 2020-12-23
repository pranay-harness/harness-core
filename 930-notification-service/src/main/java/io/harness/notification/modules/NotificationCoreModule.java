package io.harness.notification.modules;

import static io.harness.notification.constant.NotificationServiceConstants.MAILSERVICE;
import static io.harness.notification.constant.NotificationServiceConstants.MSTEAMSSERVICE;
import static io.harness.notification.constant.NotificationServiceConstants.PAGERDUTYSERVICE;
import static io.harness.notification.constant.NotificationServiceConstants.SLACKSERVICE;

import static java.time.Duration.ofSeconds;

import io.harness.mongo.queue.NGMongoQueueConsumer;
import io.harness.ng.core.UserClientModule;
import io.harness.notification.NotificationClientBackendConfiguration;
import io.harness.notification.NotificationConfiguration;
import io.harness.notification.SmtpConfig;
import io.harness.notification.entities.MongoNotificationRequest;
import io.harness.notification.eventbackbone.MessageConsumer;
import io.harness.notification.eventbackbone.MongoMessageConsumer;
import io.harness.notification.service.ChannelServiceImpl;
import io.harness.notification.service.MSTeamsServiceImpl;
import io.harness.notification.service.MailServiceImpl;
import io.harness.notification.service.NotificationServiceImpl;
import io.harness.notification.service.NotificationSettingsServiceImpl;
import io.harness.notification.service.NotificationTemplateServiceImpl;
import io.harness.notification.service.PagerDutyServiceImpl;
import io.harness.notification.service.SeedDataPopulaterServiceImpl;
import io.harness.notification.service.SlackServiceImpl;
import io.harness.notification.service.api.ChannelService;
import io.harness.notification.service.api.NotificationService;
import io.harness.notification.service.api.NotificationSettingsService;
import io.harness.notification.service.api.NotificationTemplateService;
import io.harness.notification.service.api.SeedDataPopulaterService;
import io.harness.queue.QueueConsumer;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
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
        appConfig.getRbacServiceConfig(), appConfig.getNotificationSecrets().getManagerServiceSecret()));
    install(new UserClientModule(appConfig.getServiceHttpClientConfig(),
        appConfig.getNotificationSecrets().getManagerServiceSecret(), "NotificationService"));
    bind(ChannelService.class).to(ChannelServiceImpl.class);
    bind(NotificationSettingsService.class).to(NotificationSettingsServiceImpl.class);
    bind(SeedDataPopulaterService.class).to(SeedDataPopulaterServiceImpl.class);
    bind(ChannelService.class).annotatedWith(Names.named(MAILSERVICE)).to(MailServiceImpl.class);
    bind(ChannelService.class).annotatedWith(Names.named(SLACKSERVICE)).to(SlackServiceImpl.class);
    bind(ChannelService.class).annotatedWith(Names.named(PAGERDUTYSERVICE)).to(PagerDutyServiceImpl.class);
    bind(ChannelService.class).annotatedWith(Names.named(MSTEAMSSERVICE)).to(MSTeamsServiceImpl.class);
    bind(NotificationService.class).to(NotificationServiceImpl.class);
    bind(NotificationTemplateService.class).to(NotificationTemplateServiceImpl.class);
    bindMessageConsumer();
  }

  private void bindMessageConsumer() {
    NotificationClientBackendConfiguration notificationClientBackendConfiguration = getBackendConfig();
    assert notificationClientBackendConfiguration.getType().equalsIgnoreCase("mongo");
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
}

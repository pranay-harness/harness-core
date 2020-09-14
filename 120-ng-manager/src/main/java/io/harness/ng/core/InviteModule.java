package io.harness.ng.core;

import static java.time.Duration.ofSeconds;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.ng.NextGenConfiguration;
import io.harness.ng.core.invites.api.InvitesService;
import io.harness.ng.core.invites.api.impl.InvitesServiceImpl;
import io.harness.ng.core.invites.ext.mail.EmailData;
import io.harness.ng.core.invites.ext.mail.EmailNotificationListener;
import io.harness.ng.core.invites.ext.mail.SmtpConfig;
import io.harness.ng.remote.client.ServiceHttpClientConfig;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.queue.QueuePublisher;

public class InviteModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String managerServiceSecret;

  public InviteModule(ServiceHttpClientConfig serviceHttpClientConfig, String managerServiceSecret) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.managerServiceSecret = managerServiceSecret;
  }

  @Override
  protected void configure() {
    bind(InvitesService.class).to(InvitesServiceImpl.class);
    bind(new TypeLiteral<QueueListener<EmailData>>() {}).to(EmailNotificationListener.class);
    registerRequiredBindings();
    install(new UserClientModule(this.serviceHttpClientConfig, this.managerServiceSecret));
  }

  @Provides
  @Singleton
  QueuePublisher<EmailData> emailQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, EmailData.class, null, config);
  }

  @Provides
  @Singleton
  QueueConsumer<EmailData> emailQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, EmailData.class, ofSeconds(5), null, config);
  }

  @Provides
  @Named("baseURL")
  @Singleton
  protected String getBaseURL(NextGenConfiguration nextGenConfiguration) {
    return nextGenConfiguration.getBaseURL();
  }

  @Provides
  @Named("env")
  @Singleton
  protected String getEnv(NextGenConfiguration nextGenConfiguration) {
    return nextGenConfiguration.getEnvironment();
  }

  @Provides
  @Named("userVerificatonSecret")
  @Singleton
  protected String getUserVerificationSecret(NextGenConfiguration nextGenConfiguration) {
    return nextGenConfiguration.getNextGenConfig().getUserVerificationSecret();
  }

  @Provides
  @Singleton
  protected SmtpConfig getSmtpConfig(NextGenConfiguration nextGenConfiguration) {
    return nextGenConfiguration.getSmtpConfig();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}

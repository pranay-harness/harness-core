package io.harness.ng.core;

import static java.time.Duration.ofSeconds;

import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.ng.NextGenConfiguration;
import io.harness.ng.core.invites.api.InvitesService;
import io.harness.ng.core.invites.api.impl.InvitesServiceImpl;
import io.harness.ng.core.invites.ext.mail.EmailData;
import io.harness.ng.core.invites.ext.mail.EmailNotificationListener;
import io.harness.ng.core.invites.ext.mail.SmtpConfig;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.queue.QueuePublisher;
import io.harness.remote.client.ServiceHttpClientConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class InviteModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String managerServiceSecret;
  private final String clientId;

  public InviteModule(ServiceHttpClientConfig serviceHttpClientConfig, String managerServiceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.managerServiceSecret = managerServiceSecret;
    this.clientId = clientId;
  }

  @Override
  protected void configure() {
    bind(InvitesService.class).to(InvitesServiceImpl.class);
    bind(new TypeLiteral<QueueListener<EmailData>>() {}).to(EmailNotificationListener.class);
    registerRequiredBindings();
    install(new UserClientModule(this.serviceHttpClientConfig, this.managerServiceSecret, this.clientId));
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
  @Named("baseUrl")
  @Singleton
  protected String getBaseURL(NextGenConfiguration nextGenConfiguration) {
    return nextGenConfiguration.getNgManagerClientConfig().getBaseUrl();
  }

  @Provides
  @Named("userVerificationSecret")
  @Singleton
  protected String getUserVerificationSecret(NextGenConfiguration nextGenConfiguration) {
    return nextGenConfiguration.getNextGenConfig().getUserVerificationSecret();
  }

  @Provides
  @Singleton
  protected TransactionTemplate getTransactionTemplate(MongoTransactionManager mongoTransactionManager) {
    return new TransactionTemplate(mongoTransactionManager);
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

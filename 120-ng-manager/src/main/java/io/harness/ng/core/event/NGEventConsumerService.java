package io.harness.ng.core.event;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkConstants.DEFAULT_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_ACTIVITY;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_ACTIVITY_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.SAML_AUTHORIZATION_ASSERTION;
import static io.harness.eventsframework.EventsFrameworkConstants.SETUP_USAGE;
import static io.harness.eventsframework.EventsFrameworkConstants.SETUP_USAGE_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.USERMEMBERSHIP;
import static io.harness.eventsframework.EventsFrameworkConstants.USERMEMBERSHIP_MAX_PROCESSING_TIME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.authenticationsettings.SamlAuthorizationStreamConsumer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class NGEventConsumerService implements Managed {
  @Inject private NGAccountSetupConsumer ngAccountSetupConsumer;
  @Inject private EntityCRUDStreamConsumer entityCRUDStreamConsumer;
  @Inject private UserMembershipStreamConsumer userMembershipStreamConsumer;
  @Inject private SetupUsageStreamConsumer setupUsageStreamConsumer;
  @Inject private EntityActivityStreamConsumer entityActivityStreamConsumer;
  @Inject private SamlAuthorizationStreamConsumer samlAuthorizationStreamConsumer;
  private ExecutorService ngAccountSetupConsumerService;
  private ExecutorService entityCRUDConsumerService;
  private ExecutorService setupUsageConsumerService;
  private ExecutorService entityActivityConsumerService;
  private ExecutorService userMembershipConsumerService;
  private ExecutorService samlAuthorizationConsumerService;

  @Override
  public void start() {
    ngAccountSetupConsumerService = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("ng_account_setup_consumer").build());
    entityCRUDConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(ENTITY_CRUD).build());
    setupUsageConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(SETUP_USAGE).build());
    entityActivityConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(ENTITY_ACTIVITY).build());
    userMembershipConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(USERMEMBERSHIP).build());
    samlAuthorizationConsumerService = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat(SAML_AUTHORIZATION_ASSERTION).build());

    entityCRUDConsumerService.execute(entityCRUDStreamConsumer);
    ngAccountSetupConsumerService.execute(ngAccountSetupConsumer);
    setupUsageConsumerService.execute(setupUsageStreamConsumer);
    entityActivityConsumerService.execute(entityActivityStreamConsumer);
    userMembershipConsumerService.execute(userMembershipStreamConsumer);
    samlAuthorizationConsumerService.execute(samlAuthorizationStreamConsumer);
  }

  @Override
  public void stop() throws InterruptedException {
    ngAccountSetupConsumerService.shutdown();
    entityCRUDConsumerService.shutdown();
    setupUsageConsumerService.shutdown();
    entityActivityConsumerService.shutdown();
    userMembershipConsumerService.shutdown();
    samlAuthorizationConsumerService.shutdown();
    ngAccountSetupConsumerService.awaitTermination(ENTITY_CRUD_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
    entityCRUDConsumerService.awaitTermination(ENTITY_CRUD_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
    setupUsageConsumerService.awaitTermination(SETUP_USAGE_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
    entityActivityConsumerService.awaitTermination(ENTITY_ACTIVITY_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
    userMembershipConsumerService.awaitTermination(USERMEMBERSHIP_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
    samlAuthorizationConsumerService.awaitTermination(DEFAULT_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
  }
}

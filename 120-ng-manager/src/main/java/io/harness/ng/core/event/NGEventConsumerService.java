package io.harness.ng.core.event;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_ACTIVITY;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.SETUP_USAGE;

import io.harness.annotations.dev.OwnedBy;

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
  @Inject private EntityCRUDStreamConsumer entityCRUDStreamConsumer;
  @Inject private FeatureFlagStreamConsumer featureFlagStreamConsumer;
  @Inject private SetupUsageStreamConsumer setupUsageStreamConsumer;
  @Inject private EntityActivityStreamConsumer entityActivityStreamConsumer;
  private ExecutorService entityCRUDConsumerService;
  private ExecutorService featureFlagConsumerService;
  private ExecutorService setupUsageConsumerService;
  private ExecutorService entityActivityConsumerService;

  @Override
  public void start() {
    entityCRUDConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(ENTITY_CRUD).build());
    featureFlagConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(FEATURE_FLAG_STREAM).build());
    setupUsageConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(SETUP_USAGE).build());
    entityActivityConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(ENTITY_ACTIVITY).build());
    entityCRUDConsumerService.execute(entityCRUDStreamConsumer);
    featureFlagConsumerService.execute(featureFlagStreamConsumer);
    setupUsageConsumerService.execute(setupUsageStreamConsumer);
    entityActivityConsumerService.execute(entityActivityStreamConsumer);
  }

  @Override
  public void stop() throws InterruptedException {
    entityCRUDConsumerService.shutdown();
    featureFlagConsumerService.shutdown();
    setupUsageConsumerService.shutdown();
    entityActivityConsumerService.shutdown();
    entityCRUDConsumerService.awaitTermination(ENTITY_CRUD_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
    featureFlagConsumerService.awaitTermination(FEATURE_FLAG_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
    setupUsageConsumerService.awaitTermination(FEATURE_FLAG_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
    entityActivityConsumerService.awaitTermination(FEATURE_FLAG_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
  }
}

package io.harness.pms.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_EVENTS_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_EVENTS_STREAM_MAX_PROCESSING_TIME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.pms.event.entitycrud.PMSEntityCRUDStreamConsumer;
import io.harness.pms.event.pollingevent.PollingEventStreamConsumer;
import io.harness.pms.event.webhookevent.WebhookEventStreamConsumer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Todo: Migrate these to use event controller and delete this class.
 */
@Slf4j
@OwnedBy(PIPELINE)
public class PMSEventConsumerService implements Managed {
  @Inject private PMSEntityCRUDStreamConsumer entityCRUDStreamConsumer;
  @Inject private WebhookEventStreamConsumer webhookEventStreamConsumer;
  @Inject private PollingEventStreamConsumer pollingEventStreamConsumer;

  private ExecutorService entityCRUDConsumerService;
  private ExecutorService webhookEventConsumerService;
  private ExecutorService pollingEventConsumerService;

  @Override
  public void start() {
    entityCRUDConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(ENTITY_CRUD).build());
    entityCRUDConsumerService.execute(entityCRUDStreamConsumer);

    webhookEventConsumerService =
        Executors.newFixedThreadPool(15, new ThreadFactoryBuilder().setNameFormat(WEBHOOK_EVENTS_STREAM).build());
    webhookEventConsumerService.execute(webhookEventStreamConsumer);

    pollingEventConsumerService = Executors.newFixedThreadPool(
        5, new ThreadFactoryBuilder().setNameFormat(EventsFrameworkConstants.POLLING_EVENTS_STREAM).build());
    pollingEventConsumerService.execute(pollingEventStreamConsumer);
  }

  @Override
  public void stop() throws InterruptedException {
    entityCRUDConsumerService.shutdownNow();
    entityCRUDConsumerService.awaitTermination(ENTITY_CRUD_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);

    webhookEventConsumerService.shutdownNow();
    webhookEventConsumerService.awaitTermination(
        WEBHOOK_EVENTS_STREAM_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);

    pollingEventConsumerService.shutdownNow();
    pollingEventConsumerService.awaitTermination(
        EventsFrameworkConstants.POLLING_EVENTS_STREAM_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
  }
}

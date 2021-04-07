package io.harness.pms.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME;

import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class PMSEventConsumerService implements Managed {
  @Inject private PMSEntityCRUDStreamConsumer entityCRUDStreamConsumer;
  private ExecutorService entityCRUDConsumerService;

  @Override
  public void start() {
    entityCRUDConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(ENTITY_CRUD).build());
    entityCRUDConsumerService.execute(entityCRUDStreamConsumer);
  }

  @Override
  public void stop() throws InterruptedException {
    entityCRUDConsumerService.shutdown();
    entityCRUDConsumerService.awaitTermination(ENTITY_CRUD_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
  }
}

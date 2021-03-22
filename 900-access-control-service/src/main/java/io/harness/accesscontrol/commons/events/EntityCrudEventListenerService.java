package io.harness.accesscontrol.commons.events;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EntityCrudEventListenerService implements Managed {
  private final EntityCrudEventListener entityCrudEventListener;
  private final ExecutorService executorService;
  private Future<?> eventListenerFuture;

  @Inject
  public EntityCrudEventListenerService(EntityCrudEventListener entityCrudEventListener) {
    this.entityCrudEventListener = entityCrudEventListener;
    executorService = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("entity-crud-event-listener-main-thread").build());
  }

  @Override
  public void start() throws Exception {
    eventListenerFuture = executorService.submit(entityCrudEventListener);
  }

  @Override
  public void stop() throws Exception {
    eventListenerFuture.cancel(true);
    executorService.shutdownNow();
  }
}

package io.harness.accesscontrol.commons.events;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EventListenerService implements Managed {
  private final EventListener eventListener;
  private final ExecutorService executorService;
  private Future<?> eventListenerFuture;

  @Inject
  public EventListenerService(EventListener eventListener) {
    this.eventListener = eventListener;
    executorService = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("event-listener-main-thread").build());
  }

  @Override
  public void start() throws Exception {
    eventListenerFuture = executorService.submit(eventListener);
  }

  @Override
  public void stop() throws Exception {
    eventListenerFuture.cancel(true);
    executorService.shutdownNow();
  }
}

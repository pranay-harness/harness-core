package io.harness.eventframework.manager;

import static io.harness.eventsframework.EventsFrameworkConstants.DEFAULT_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.OBSERVER_EVENT_CHANNEL;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ManagerEventConsumerService implements Managed {
  @Inject private ManagerRemoteObserverEventConsumer managerRemoteObserverEventConsumerService;
  private ExecutorService managerRemoteObserverStreamConsumerService;

  @Override
  public void start() {
    managerRemoteObserverStreamConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(OBSERVER_EVENT_CHANNEL).build());

    managerRemoteObserverStreamConsumerService.execute(managerRemoteObserverEventConsumerService);
  }

  @Override
  public void stop() throws InterruptedException {
    managerRemoteObserverStreamConsumerService.shutdownNow();
    managerRemoteObserverStreamConsumerService.awaitTermination(
        DEFAULT_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
  }
}

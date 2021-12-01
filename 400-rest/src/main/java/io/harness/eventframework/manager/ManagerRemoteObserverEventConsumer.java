package io.harness.eventframework.manager;

import io.harness.AuthorizationServiceHeader;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.observer.RemoteObserver;
import io.harness.observer.consumer.AbstractRemoteObserverEventConsumer;
import io.harness.observer.consumer.RemoteObserverProcessor;
import io.harness.queue.QueueController;

import java.util.Map;

@OwnedBy(HarnessTeam.DEL)
public class ManagerRemoteObserverEventConsumer extends AbstractRemoteObserverEventConsumer {
  public ManagerRemoteObserverEventConsumer(Consumer redisConsumer, Map<String, RemoteObserver> remoteObservers,
      QueueController queueController, RemoteObserverProcessor remoteObserverProcessor) {
    super(redisConsumer, remoteObservers, queueController, remoteObserverProcessor);
  }

  @Override
  public String getServicePrincipal() {
    return AuthorizationServiceHeader.DMS.getServiceId();
  }
}
